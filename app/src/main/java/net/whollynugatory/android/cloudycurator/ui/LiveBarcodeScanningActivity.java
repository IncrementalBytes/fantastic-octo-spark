/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.whollynugatory.android.cloudycurator.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.chip.Chip;
import com.google.common.base.Objects;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.barcodedetection.BarcodeProcessor;
import net.whollynugatory.android.cloudycurator.barcodedetection.BarcodeResultFragment;
import net.whollynugatory.android.cloudycurator.camera.CameraSource;
import net.whollynugatory.android.cloudycurator.camera.CameraSourcePreview;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;
import net.whollynugatory.android.cloudycurator.camera.WorkflowModel;
import net.whollynugatory.android.cloudycurator.camera.WorkflowModel.WorkflowState;
import net.whollynugatory.android.cloudycurator.common.GoogleBookApiTask;
import net.whollynugatory.android.cloudycurator.common.QueryBookDatabaseTask;
import net.whollynugatory.android.cloudycurator.db.CuratorRepository;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;

import java.io.IOException;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

/**
 *  The barcode scanning workflow using camera preview.
 **/
public class LiveBarcodeScanningActivity extends AppCompatActivity implements
  View.OnClickListener {

  private static final String TAG = BaseActivity.BASE_TAG + "LiveBarcodeActivity";

  private CameraSource cameraSource;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private View settingsButton;
  private View flashButton;
  private Chip promptChip;
  private AnimatorSet promptChipAnimator;
  private WorkflowModel workflowModel;
  private WorkflowState currentWorkflowState;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_live_barcode);
    preview = findViewById(R.id.camera_preview);
    graphicOverlay = findViewById(R.id.camera_preview_graphic_overlay);
    graphicOverlay.setOnClickListener(this);
    cameraSource = new CameraSource(graphicOverlay);

    promptChip = findViewById(R.id.bottom_prompt_chip);
    promptChipAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.bottom_prompt_chip_enter);
    promptChipAnimator.setTarget(promptChip);

    findViewById(R.id.close_button).setOnClickListener(this);
    flashButton = findViewById(R.id.flash_button);
    flashButton.setOnClickListener(this);
    settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(this);

    setUpWorkflowModel();
  }

  @Override
  protected void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
    workflowModel.markCameraFrozen();
    settingsButton.setEnabled(true);
    currentWorkflowState = WorkflowState.NOT_STARTED;
    cameraSource.setFrameProcessor(new BarcodeProcessor(graphicOverlay, workflowModel));
    workflowModel.setWorkflowState(WorkflowState.DETECTING);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();

    Log.d(TAG, "++onPostResume()");
    BarcodeResultFragment.dismiss(getSupportFragmentManager());
  }

  @Override
  protected void onPause() {
    super.onPause();

    Log.d(TAG, "++onPause()");
    currentWorkflowState = WorkflowState.NOT_STARTED;
    stopCameraPreview();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    if (cameraSource != null) {
      cameraSource.release();
      cameraSource = null;
    }
  }

  @Override
  public void onClick(View view) {

    Log.d(TAG, "++onClick(View)");
    int id = view.getId();
    if (id == R.id.close_button) {
      onBackPressed();
    } else if (id == R.id.flash_button) {
      if (flashButton.isSelected()) {
        flashButton.setSelected(false);
        cameraSource.updateFlashMode(Parameters.FLASH_MODE_OFF);
      } else {
        flashButton.setSelected(true);
        cameraSource.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
      }
    } else if (id == R.id.settings_button) { // Sets as disabled to prevent the user from clicking on it too fast.
      settingsButton.setEnabled(false);
      startActivity(new Intent(this, SettingsActivity.class));
    }
  }

  private void startCameraPreview() {

    if (!workflowModel.isCameraLive() && cameraSource != null) {
      try {
        workflowModel.markCameraLive();
        preview.start(cameraSource);
      } catch (IOException e) {
        Log.e(TAG, "Failed to start camera preview!", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  private void stopCameraPreview() {

    if (workflowModel.isCameraLive()) {
      workflowModel.markCameraFrozen();
      flashButton.setSelected(false);
      preview.stop();
    }
  }

  private void setUpWorkflowModel() {
    workflowModel = ViewModelProviders.of(this).get(WorkflowModel.class);

    // Observes the workflow state changes, if happens, update the overlay view indicators and camera preview state.
    workflowModel.workflowState.observe(
      this,
      workflowState -> {
        if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
          return;
        }

        currentWorkflowState = workflowState;
        Log.d(TAG, "Current workflow state: " + currentWorkflowState.name());

        boolean wasPromptChipGone = (promptChip.getVisibility() == View.GONE);

        switch (workflowState) {
          case DETECTING:
            promptChip.setVisibility(View.VISIBLE);
            promptChip.setText(R.string.prompt_point_at_a_barcode);
            startCameraPreview();
            break;
          case CONFIRMING:
            promptChip.setVisibility(View.VISIBLE);
            promptChip.setText(R.string.prompt_move_camera_closer);
            startCameraPreview();
            break;
          case SEARCHING:
            promptChip.setVisibility(View.VISIBLE);
            promptChip.setText(R.string.prompt_searching);
            stopCameraPreview();
            break;
          case DETECTED:
          case SEARCHED:
            promptChip.setVisibility(View.GONE);
            stopCameraPreview();
            break;
          default:
            promptChip.setVisibility(View.GONE);
            break;
        }

        boolean shouldPlayPromptChipEnteringAnimation =
          wasPromptChipGone && (promptChip.getVisibility() == View.VISIBLE);
        if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning()) {
          promptChipAnimator.start();
        }
      });

    workflowModel.detectedBarcode.observe(
      this,
      barcode -> {
        if (barcode != null) {
//          ArrayList<BarcodeField> barcodeFieldList = new ArrayList<>();
//          barcodeFieldList.add(new BarcodeField("Raw Value", barcode.getRawValue()));
//          BarcodeResultFragment.show(getSupportFragmentManager(), barcodeFieldList);

          if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_ISBN) {
            BookEntity bookEntity = new BookEntity();
            String barcodeValue = barcode.getDisplayValue();
            Log.d(TAG, "Found a bar code: " + barcodeValue);
            if (barcodeValue != null && barcodeValue.length() == 8) {
              bookEntity.ISBN_8 = barcodeValue;
            } else if (barcodeValue != null && barcodeValue.length() == 13) {
              bookEntity.ISBN_13 = barcodeValue;
            }
            if ((!bookEntity.ISBN_8.isEmpty() && !bookEntity.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) ||
              (!bookEntity.ISBN_13.isEmpty() && !bookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13))) {
              new QueryBookDatabaseTask(this, CuratorRepository.getInstance(this), bookEntity).execute();
            }
          } else {
            Log.w(TAG, "Unexpected bar code: " + barcode.getDisplayValue());
          }
        }
      });
  }

//  @Override
//  public void onBarcodeProcessed(FirebaseVisionBarcode barcode) {
//
//    Log.d(TAG, "++onBarcodeProcessed(FirebaseVisionBarcode)");
//    if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_ISBN) {
//      BookEntity bookEntity = new BookEntity();
//      String barcodeValue = barcode.getDisplayValue();
//      Log.d(TAG, "Found a bar code: " + barcodeValue);
//      if (barcodeValue != null && barcodeValue.length() == 8) {
//        bookEntity.ISBN_8 = barcodeValue;
//      } else if (barcodeValue != null && barcodeValue.length() == 13) {
//        bookEntity.ISBN_13 = barcodeValue;
//      }
//
//      if ((!bookEntity.ISBN_8.isEmpty() && !bookEntity.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) ||
//        (!bookEntity.ISBN_13.isEmpty() && !bookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13))) {
//        new QueryBookDatabaseTask(this, CuratorRepository.getInstance(this), bookEntity).execute();
//      }
//    } else {
//      Log.w(TAG, "Unexpected bar code: " + barcode.getDisplayValue());
//    }
//  }

  public void queryBookDatabaseComplete(BookDetail bookDetail) {

    Log.d(TAG, "++queryBookDatabaseComplete(BookDetail)");
    if (bookDetail.isValid()) {
      Log.d(TAG, "Found existing book entity in database.");
//      mProgressBar.setIndeterminate(false);
      Intent intent = new Intent(this, MainActivity.class);
      intent.putExtra(BaseActivity.ARG_BOOK_DETAIL, bookDetail);
      startActivity(intent);
    } else {
      Log.d(TAG, "Not in user's book list: " + bookDetail.toString());
      if (bookDetail.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) &&
        bookDetail.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) &&
        bookDetail.LCCN.equals(BaseActivity.DEFAULT_LCCN) &&
        bookDetail.Title.isEmpty()) {
        Log.d(TAG, "Book entity is not valid: " + bookDetail.toString());
//        showDismissableSnackbar(getString(R.string.err_search_criteria));
      } else {
        new GoogleBookApiTask(this, bookDetail).execute();
      }
    }
  }

  public void retrieveBooksComplete(ArrayList<BookEntity> bookEntityList) {

    Log.d(TAG, "++retrieveBooksComplete(ArrayList<BookEntity>)");
//    mProgressBar.setIndeterminate(false);
    if (bookEntityList.size() == 0) {
      Log.d(TAG, "Book entity list is empty after Google Book API query.");
//      showDismissableSnackbar(getString(R.string.no_results));
    } else {
      if (bookEntityList.size() == BaseActivity.MAX_RESULTS) {
//        showDismissableSnackbar(getString(R.string.max_results));
      }

      Intent intent = new Intent(this, MainActivity.class);
      intent.putExtra(BaseActivity.ARG_RESULT_LIST, bookEntityList);
      startActivity(intent);
    }
  }
}
