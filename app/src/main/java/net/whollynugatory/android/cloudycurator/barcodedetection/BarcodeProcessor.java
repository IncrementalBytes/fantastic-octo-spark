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

package net.whollynugatory.android.cloudycurator.barcodedetection;

import android.animation.ValueAnimator;
import android.graphics.RectF;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.whollynugatory.android.cloudycurator.PreferenceUtils;
import net.whollynugatory.android.cloudycurator.camera.CameraReticleAnimator;
import net.whollynugatory.android.cloudycurator.camera.FrameProcessorBase;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;
import net.whollynugatory.android.cloudycurator.camera.WorkflowModel;
import net.whollynugatory.android.cloudycurator.camera.WorkflowModel.WorkflowState;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.IOException;
import java.util.List;

import androidx.annotation.MainThread;

/**
 * A processor to run the barcode detector.
 **/
public class BarcodeProcessor extends FrameProcessorBase<List<FirebaseVisionBarcode>> {

  private static final String TAG = BaseActivity.BASE_TAG + "BarcodeProcessor";

  private final FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
  private final WorkflowModel workflowModel;
  private final CameraReticleAnimator cameraReticleAnimator;

  public BarcodeProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {

    Log.d(TAG, "++BarcodeProcessor(GraphicOverlay, WorkflowModel)");
    this.workflowModel = workflowModel;
    this.cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
  }

  @Override
  protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {

    return detector.detectInImage(image);
  }

  @MainThread
  @Override
  protected void onSuccess(FirebaseVisionImage image, List<FirebaseVisionBarcode> results, GraphicOverlay graphicOverlay) {

    if (!workflowModel.isCameraLive()) {
      return;
    }

    // picks the barcode, if exists, that covers the center of graphic overlay
    FirebaseVisionBarcode barcodeInCenter = null;
    for (FirebaseVisionBarcode barcode : results) {
      RectF box = graphicOverlay.translateRect(barcode.getBoundingBox());
      if (box.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f)) {
        barcodeInCenter = barcode;
        break;
      }
    }

    graphicOverlay.clear();
    if (barcodeInCenter == null) {
      cameraReticleAnimator.start();
      graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
      workflowModel.setWorkflowState(WorkflowState.DETECTING);
    } else {
      cameraReticleAnimator.cancel();
      float sizeProgress =
        PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter);
      if (sizeProgress < 1) { // Barcode in the camera view is too small, so prompt user to move camera closer.
        graphicOverlay.add(new BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter));
        workflowModel.setWorkflowState(WorkflowState.CONFIRMING);
      } else { // Barcode size in the camera view is sufficient.
        if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.getContext())) {
          ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter);
          loadingAnimator.start();
          graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
          workflowModel.setWorkflowState(WorkflowState.SEARCHING);
        } else {
          workflowModel.setWorkflowState(WorkflowState.DETECTED);
          workflowModel.detectedBarcode.setValue(barcodeInCenter);
        }
      }
    }

    graphicOverlay.invalidate();
  }

  private ValueAnimator createLoadingAnimator(GraphicOverlay graphicOverlay, FirebaseVisionBarcode barcode) {

    Log.d(TAG, "++createLoadingAnimator(GraphicOverlay, FirebaseVisionBarcode)");
    float endProgress = 1.1f;
    ValueAnimator loadingAnimator = ValueAnimator.ofFloat(0f, endProgress);
    loadingAnimator.setDuration(2000);
    loadingAnimator.addUpdateListener(
      animation -> {
        if (Float.compare((float) loadingAnimator.getAnimatedValue(), endProgress) >= 0) {
          graphicOverlay.clear();
          workflowModel.setWorkflowState(WorkflowState.SEARCHED);
          workflowModel.detectedBarcode.setValue(barcode);
        } else {
          graphicOverlay.invalidate();
        }
      });
    return loadingAnimator;
  }

  @Override
  protected void onFailure(Exception e) {

    Log.d(TAG, "++onFailure(Exception)");
    Log.e(TAG, "Barcode detection failed!", e);
  }

  @Override
  public void stop() {

    Log.d(TAG, "++stop()");
    try {
      detector.close();
    } catch (IOException e) {
      Log.e(TAG, "Failed to close barcode detector!", e);
    }
  }
}
