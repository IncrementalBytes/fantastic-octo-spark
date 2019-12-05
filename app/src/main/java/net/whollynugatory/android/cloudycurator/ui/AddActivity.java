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

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import net.whollynugatory.android.cloudycurator.BuildConfig;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.RetrieveBookDataTask;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.models.User;
import net.whollynugatory.android.cloudycurator.ui.fragments.BookEntityFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ManualSearchFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.QueryFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ResultListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ScanResultsFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.TutorialFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.UserPreferenceFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

public class AddActivity extends BaseActivity implements
  QueryFragment.OnQueryListener,
  ResultListFragment.OnResultListListener,
  ScanResultsFragment.OnScanResultsListener,
  TutorialFragment.OnTutorialListener {

  private static final String TAG = BaseActivity.BASE_TAG + "AddActivity";

  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private File mCurrentImageFile;
  private Bitmap mImageBitmap;
  private int mRotationAttempts;
  private User mUser;

  /*
    AppCompatActivity Override(s)
  */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_add);

    mProgressBar = findViewById(R.id.add_progress);
    Toolbar mAddToolbar = findViewById(R.id.add_toolbar);

    setSupportActionBar(mAddToolbar);
    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.add_fragment_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(ManualSearchFragment.class.getName())) {
          setTitle(getString(R.string.title_gathering_data));
        } else if (fragmentClassName.equals(TutorialFragment.class.getName())) {
          setTitle(getString(R.string.title_tutorial));
//        } else if (fragmentClassName.equals(CameraSourceFragment.class.getName())) {
//          setTitle(getString(R.string.title_camera_source));
        }
      }
    });

    if (getIntent().hasExtra(BaseActivity.ARG_USER)) {
      mUser = (User) getIntent().getSerializableExtra(BaseActivity.ARG_USER);
    }

    if (mProgressBar != null) {
      mProgressBar.setIndeterminate(true);
    }

    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      checkForCameraPermission();
    } else {
      setFailAndFinish(R.string.err_no_camera_detected);
    }
  }

  @Override
  public void onBackPressed() {

    Log.d(TAG, "++onBackPressed()");
    setCancelAndFinish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    Log.d(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.menu_add, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    deleteImageFile();

    mCurrentImageFile = null;
    mImageBitmap = null;
    mUser = null;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    Log.d(TAG, "++onOptionsItemSelected(MenuItem)");
    if (item.getItemId() == R.id.action_cancel) {
      setCancelAndFinish();
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    Log.d(TAG, "++onSaveInstanceState(Bundle)");
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(TAG, "++onActivityResult(int, int, Intent)");
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      if (BuildConfig.DEBUG) {
        File f = new File(getString(R.string.debug_path), getString(R.string.debug_file_name));
        try {
          mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
          Crashlytics.logException(e);
        }
      } else {
        try {
          mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(mCurrentImageFile));
        } catch (FileNotFoundException e) {
          Crashlytics.logException(e);
        }
      }

      if (mImageBitmap != null) {
        Bitmap emptyBitmap = Bitmap.createBitmap(
          mImageBitmap.getWidth(),
          mImageBitmap.getHeight(),
          mImageBitmap.getConfig());
        if (!mImageBitmap.sameAs(emptyBitmap)) {
          scanImageForISBN();
        } else {
          showDismissableSnackbar(getString(R.string.err_image_empty));
        }
      } else {
        showDismissableSnackbar(getString(R.string.err_image_not_found));
      }
    } else {
      showDismissableSnackbar(getString(R.string.err_camera_data_unexpected));
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    Log.d(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    checkForCameraPermission();
  }

  /*
    Fragment Callback(s)
   */
  @Override
  public void onQueryActionComplete(String message) {

    Log.d(TAG, "++onQueryActionComplete(String)");
    mProgressBar.setIndeterminate(false);
    if (!message.isEmpty()) {
      showDismissableSnackbar(message);
    }
  }

  @Override
  public void onQueryShowManualDialog() {

    Log.d(TAG, "++onQueryShowManualDialog()");
    mProgressBar.setIndeterminate(true);
    LayoutInflater layoutInflater = LayoutInflater.from(this);
    View promptView = layoutInflater.inflate(R.layout.dialog_search_manual, null);
    EditText editText = promptView.findViewById(R.id.manual_dialog_edit_search);
    RadioGroup radioGroup = promptView.findViewById(R.id.manual_dialog_radio_search);
    androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
    alertDialogBuilder.setView(promptView);
    alertDialogBuilder.setCancelable(false)
      .setPositiveButton(R.string.ok, (dialog, id) -> {

        BookEntity bookEntity = new BookEntity();
        switch (radioGroup.getCheckedRadioButtonId()) {
          case R.id.manual_dialog_radio_isbn:
            String value = editText.getText().toString();
            if (value.length() == 8) {
              bookEntity.ISBN_8 = value;
            } else if (value.length() == 13) {
              bookEntity.ISBN_13 = value;
            }

            if (!bookEntity.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) ||
              !bookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
              queryInUserBooks(bookEntity);
            } else {
              showDismissableSnackbar(getString(R.string.err_invalid_isbn));
            }

            break;
          case R.id.manual_dialog_radio_title:
            bookEntity.Title = editText.getText().toString();
            queryInUserBooks(bookEntity);
            break;
          case R.id.manual_dialog_radio_lccn:
            bookEntity.LCCN = editText.getText().toString();
            queryInUserBooks(bookEntity);
        }
      })
      .setNegativeButton(R.string.cancel, (dialog, id) -> {
        mProgressBar.setIndeterminate(false);
        dialog.cancel();
      });

    androidx.appcompat.app.AlertDialog alert = alertDialogBuilder.create();
    alert.show();
  }

  @Override
  public void onQueryTakePicture() {

    Log.d(TAG, "++onQueryTakePicture()");
    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      checkForCameraPermission();
    } else {
      showDismissableSnackbar(getString(R.string.err_no_camera_detected));
    }
  }


  @Override
  public void onResultListActionComplete(String message) {

    Log.d(TAG, "++onResultListActionComplete(String)");
    if (!message.isEmpty()) {
      showDismissableSnackbar(message);
    }
  }

  @Override
  public void onResultListPopulated(int size) {

    Log.d(TAG, "++onResultListPopulated(int)");
    if (size > 1) {
      setTitle(R.string.select_book);
    }
  }

  @Override
  public void onResultListItemSelected(BookEntity bookEntity) {

    Log.d(TAG, "++onResultListItemSelected(BookEntity)");
    replaceFragment(BookEntityFragment.newInstance(mUser.Id, bookEntity));
  }

  @Override
  public void onScanResultsPopulated(int size) {

    Log.d(TAG, "++onScanResultsPopulated(int)");
    mProgressBar.setIndeterminate(false);
  }

  @Override
  public void onScanResultsItemSelected(String searchText) {

    Log.d(TAG, "++onScanResultsItemSelected(String)");
    BookEntity bookEntity = new BookEntity();
    bookEntity.Title = searchText;
    queryInUserBooks(bookEntity);
  }

  @Override
  public void onTutorialContinue() {

    Log.d(TAG, "++onTutorialContinue()");
    if (mProgressBar != null) {
      mProgressBar.setIndeterminate(true);
    }

    showPictureIntent();
  }

  @Override
  public void onTutorialShowHint(boolean show) {

    Log.d(TAG, "++onTutorialShowHint(boolean)");
    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
    editor.putBoolean(UserPreferenceFragment.SHOW_TUTORIAL_PREFERENCE, show);
    editor.apply();
    mUser.ShowBarcodeHint = show;
  }

  /*
    Public Method(s)
   */
  public void retrieveBooksComplete(ArrayList<BookEntity> bookEntityList) {

    Log.d(TAG, "++retrieveBooksComplete(ArrayList<BookEntity>)");
    mProgressBar.setIndeterminate(false);
    if (bookEntityList.size() == 0) {
      showDismissableSnackbar(getString(R.string.no_results));
    } else if (bookEntityList.size() == 1) {
      setTitle(getString(R.string.fragment_book_add));
      replaceFragment(BookEntityFragment.newInstance(mUser.Id, bookEntityList.get(0)));
    } else {
      if (bookEntityList.size() == BaseActivity.MAX_RESULTS) {
        showDismissableSnackbar(getString(R.string.max_results));
      }

      replaceFragment(ResultListFragment.newInstance(bookEntityList));
    }
  }

  /*
     Private Method(s)
    */
  private void checkForCameraPermission() {

    Log.d(TAG, "++checkDevicePermission()");
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
        Snackbar.make(
          findViewById(R.id.add_fragment_container),
          getString(R.string.permission_camera),
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              AddActivity.this,
              new String[]{Manifest.permission.CAMERA},
              BaseActivity.REQUEST_CAMERA_PERMISSIONS))
          .show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
      }
    } else {
      Log.d(TAG, "Permission granted: " + Manifest.permission.CAMERA);
      takePictureIntent();
    }
  }

  private File createImageFile() throws IOException {

    Log.d(TAG, "++createImageFile()");
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    return File.createTempFile(imageFileName, ".jpg", storageDir);
  }

  private void deleteImageFile() {

    Log.d(TAG, "++deleteImageFile()");
    if (mCurrentImageFile != null && mCurrentImageFile.exists()) {
      if (mCurrentImageFile.delete()) {
        Log.d(TAG, "Removed processed image: " + mCurrentImageFile.getName());
      } else {
        Log.w(TAG, "Unable to remove processed image: " + mCurrentImageFile.getName());
      }
    }
  }

  private void queryGoogleBookService(BookEntity bookEntity) {

    Log.d(TAG, "++queryGoogleBookService(BookEntity)");
    if (bookEntity.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) &&
      bookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) &&
      bookEntity.LCCN.equals(BaseActivity.DEFAULT_LCCN) &&
      bookEntity.Title.isEmpty()) {
      showDismissableSnackbar(getString(R.string.err_search_criteria));
    } else {
      new RetrieveBookDataTask(this, bookEntity).execute();
    }
  }

  private void queryInUserBooks(BookEntity bookEntity) {

    Log.d(TAG, "++queryInUserBooks(BookEntity)");
    BookEntity foundBook = null;
//    if (mBookEntityList != null) {
//      for (BookEntity book : mBookEntityList) {
//        if (book.isPartiallyEqual(bookEntity)) {
//          foundBook = book;
//          break;
//        }
//      }
//    }

    if (foundBook != null) {
      mProgressBar.setIndeterminate(false);
      replaceFragment(BookEntityFragment.newInstance(mUser.Id, bookEntity));
    } else {
      Log.d(TAG, "Not in user's book list: " + bookEntity.toString());
      queryGoogleBookService(bookEntity);
    }
  }

  private void replaceFragment(Fragment fragment) {

    Log.d(TAG, "++replaceFragment(Fragment)");
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.add_fragment_container, fragment)
      .addToBackStack(null)
      .commit();
  }

  private void scanImageForISBN() {

    Log.d(TAG, "++scanImageForISBN()");
    if (mImageBitmap != null) {
      mProgressBar.setIndeterminate(true);
      if (mUser.IsLibrarian) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_debug_image, null);
        ImageView imageView = promptView.findViewById(R.id.debug_dialog_image);
        BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
        imageView.setImageDrawable(bmd);
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
          .setPositiveButton(R.string.ok, (dialog, id) -> useFirebaseBarcodeScanning())
          .setNegativeButton(R.string.cancel, (dialog, id) -> {
            mProgressBar.setIndeterminate(false);
            dialog.cancel();
          });

        androidx.appcompat.app.AlertDialog alert = alertDialogBuilder.create();
        alert.show();
      } else {
        useFirebaseBarcodeScanning();
      }
    } else {
      showDismissableSnackbar(getString(R.string.err_image_not_loaded));
    }
  }

  private void scanImageForText() {

    Log.d(TAG, "++scanImageForText()");
    if (mImageBitmap != null) {
      if (mUser.IsLibrarian) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_debug_image, null);
        ImageView imageView = promptView.findViewById(R.id.debug_dialog_image);
        BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
        imageView.setImageDrawable(bmd);
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
          .setPositiveButton(R.string.ok, (dialog, id) -> useFirebaseTextScanning())
          .setNegativeButton(R.string.cancel, (dialog, id) -> {
            mProgressBar.setIndeterminate(false);
            dialog.cancel();
          });

        androidx.appcompat.app.AlertDialog alert = alertDialogBuilder.create();
        alert.show();
      } else {
        useFirebaseTextScanning();
      }
    } else {
      Log.w(TAG, getString(R.string.err_image_not_loaded));
      showDismissableSnackbar(getString(R.string.err_image_not_loaded));
    }
  }

  private void setCancelAndFinish() {

    setResult(BaseActivity.RESULT_CANCELED, new Intent());
    finish();
  }

  private void setFailAndFinish(int messageId) {

    Intent resultIntent = new Intent();
    resultIntent.putExtra(BaseActivity.ARG_MESSAGE_ID, getString(messageId));
    setResult(BaseActivity.RESULT_ADD_FAILED, resultIntent);
    finish();
  }

  private void setSuccessAndFinish(BookEntity bookEntity) {

    Intent resultIntent = new Intent();
    resultIntent.putExtra(BaseActivity.ARG_BOOK, bookEntity);
    setResult(BaseActivity.RESULT_ADD_SUCCESS, resultIntent);
    finish();
  }

  private void showDismissableSnackbar(String message) {

    mProgressBar.setIndeterminate(false);
    Log.w(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.add_fragment_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }

  private void showPictureIntent() {

    Log.d(TAG, "++showPictureIntent()");
    deleteImageFile();
    if (BuildConfig.DEBUG) {
      LayoutInflater layoutInflater = LayoutInflater.from(this);
      View promptView = layoutInflater.inflate(R.layout.dialog_debug_camera, null);

      Spinner spinner = promptView.findViewById(R.id.debug_spinner_file);
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
      alertDialogBuilder.setView(promptView);
      alertDialogBuilder.setCancelable(false)
        .setPositiveButton(R.string.ok, (dialog, id) -> {
          Intent debugIntent = new Intent();
          debugIntent.putExtra(BaseActivity.ARG_DEBUG_FILE_NAME, spinner.getSelectedItem().toString());
          onActivityResult(BaseActivity.REQUEST_IMAGE_CAPTURE, RESULT_OK, debugIntent);
        })
        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

      AlertDialog alert = alertDialogBuilder.create();
      alert.show();
    } else if (mUser.UseImageCapture) {
      // TODO: replaceFragment(CameraSourceFragment.newInstance());
    } else {
      Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        try {
          mCurrentImageFile = createImageFile();
        } catch (IOException e) {
          Crashlytics.logException(e);
        }

        if (mCurrentImageFile != null) {
          Uri photoURI = FileProvider.getUriForFile(
            this,
            "net.frostedbytes.android.comiccollector.fileprovider",
            mCurrentImageFile);
          takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
          startActivityForResult(takePictureIntent, BaseActivity.REQUEST_IMAGE_CAPTURE);
        } else {
          setFailAndFinish(R.string.err_photo_file_not_found);
        }
      } else {
        setFailAndFinish(R.string.err_camera_intent_failed);
      }
    }
  }

  private void takePictureIntent() {

    Log.d(TAG, "++takePictureIntent()");
    deleteImageFile();
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      try {
        mCurrentImageFile = createImageFile();
      } catch (IOException e) {
        Crashlytics.logException(e);
      }

      if (mCurrentImageFile != null) {
        Uri photoURI = FileProvider.getUriForFile(
          this,
          "net.whollynugatory.android.cloudycurator.fileprovider",
          mCurrentImageFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      } else {
        showDismissableSnackbar(getString(R.string.err_photo_file_not_found));
      }
    } else {
      showDismissableSnackbar(getString(R.string.err_camera_intent_failed));
    }
  }

  private void useFirebaseBarcodeScanning() {

    Log.d(TAG, "++useFirebaseBarcodeScanning()");
    FirebaseVisionBarcodeDetectorOptions options =
      new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(
          FirebaseVisionBarcode.FORMAT_EAN_8,
          FirebaseVisionBarcode.FORMAT_EAN_13)
        .build();
    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
      .getVisionBarcodeDetector(options);
    com.google.android.gms.tasks.Task<java.util.List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
      .addOnCompleteListener(task -> {

        if (task.isSuccessful() && task.getResult() != null) {
          BookEntity bookEntity = new BookEntity();
          for (FirebaseVisionBarcode barcode : task.getResult()) {
            if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_ISBN) {
              String barcodeValue = barcode.getDisplayValue();
              Log.d(TAG, "Found a bar code: " + barcodeValue);
              if (barcodeValue != null && barcodeValue.length() == 8) {
                bookEntity.ISBN_8 = barcodeValue;
              } else if (barcodeValue != null && barcodeValue.length() == 13) {
                bookEntity.ISBN_13 = barcodeValue;
              }
            } else {
              Log.w(TAG, "Unexpected bar code: " + barcode.getDisplayValue());
            }
          }

          if ((!bookEntity.ISBN_8.isEmpty() && !bookEntity.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) ||
            (!bookEntity.ISBN_13.isEmpty() && !bookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13))) {
            queryInUserBooks(bookEntity);
          } else if (mRotationAttempts < 3) {
            mRotationAttempts++;
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            mImageBitmap = Bitmap.createBitmap(
              mImageBitmap,
              0,
              0,
              mImageBitmap.getWidth(),
              mImageBitmap.getHeight(),
              matrix,
              true);
            scanImageForISBN();
          } else {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            mImageBitmap = Bitmap.createBitmap(
              mImageBitmap,
              0,
              0,
              mImageBitmap.getWidth(),
              mImageBitmap.getHeight(),
              matrix,
              true);
            mRotationAttempts = 0;
            scanImageForText();
          }
        } else {
          showDismissableSnackbar(getString(R.string.err_bar_code_task));
          replaceFragment(QueryFragment.newInstance());
        }
      });
  }

  private void useFirebaseTextScanning() {

    Log.d(TAG, "++useFirebaseTextScanning()");
    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
    FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    com.google.android.gms.tasks.Task<FirebaseVisionText> result = detector.processImage(image).addOnCompleteListener(task -> {
      if (task.isSuccessful() && task.getResult() != null) {
        ArrayList<String> blocks = new ArrayList<>();
        for (FirebaseVisionText.TextBlock textBlock : task.getResult().getTextBlocks()) {
          String block = textBlock.getText().replace("\n", " ").replace("\r", " ");
          blocks.add(block);
        }

        if (blocks.size() > 0) {
          replaceFragment(ScanResultsFragment.newInstance(blocks));
        } else if (mRotationAttempts < 3) {
          mRotationAttempts++;
          Matrix matrix = new Matrix();
          matrix.postRotate(90);
          mImageBitmap = Bitmap.createBitmap(
            mImageBitmap,
            0,
            0,
            mImageBitmap.getWidth(),
            mImageBitmap.getHeight(),
            matrix,
            true);
          scanImageForText();
        } else {
          showDismissableSnackbar(getString(R.string.err_no_bar_code_or_text));
          replaceFragment(QueryFragment.newInstance());
        }
      } else {
        showDismissableSnackbar(getString(R.string.err_text_detection_task));
        replaceFragment(QueryFragment.newInstance());
      }
    });
  }
}
