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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
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
import net.whollynugatory.android.cloudycurator.ui.fragments.BookEntityFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.BookEntityListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.LibrarianFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.QueryFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ResultListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ScanResultsFragment;
import net.whollynugatory.android.cloudycurator.models.User;
import net.whollynugatory.android.cloudycurator.common.PathUtils;
import net.whollynugatory.android.cloudycurator.common.SortUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements
  BookEntityFragment.OnBookEntityListener,
  BookEntityListFragment.OnBookEntityListListener,
  QueryFragment.OnQueryListener,
  ResultListFragment.OnResultListListener,
  ScanResultsFragment.OnScanResultsListener {

  private static final String TAG = BASE_TAG + "MainActivity";

  static final int REQUEST_IMAGE_CAPTURE = 1;

  static final int CAMERA_PERMISSIONS_REQUEST = 11;

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
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_main);

    mProgressBar = findViewById(R.id.main_progress);
    mProgressBar.setIndeterminate(true);

    Toolbar toolbar = findViewById(R.id.main_toolbar);
    setSupportActionBar(toolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(BookEntityListFragment.class.getName())) {
          setTitle(getString(R.string.fragment_book_list));
        } else if (fragmentClassName.equals(QueryFragment.class.getName())) {
          setTitle(getString(R.string.fragment_query));
        } else if (fragmentClassName.equals(ResultListFragment.class.getName())) {
          setTitle(R.string.select_book);
        } else if (fragmentClassName.equals(ScanResultsFragment.class.getName())) {
          setTitle(R.string.select_text_search);
        } else if (fragmentClassName.equals(LibrarianFragment.class.getName())) {
          setTitle(getString(R.string.librarian_fragment));
        }
      }
    });

    mUser = new User();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);

    // get user's permissions
    checkDevicePermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    deleteImageFile();
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

    Log.d(TAG, "++onRequestPermissionResult(int, String[], int[])");
    switch (requestCode) {
      case CAMERA_PERMISSIONS_REQUEST:
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d(TAG, "CAMERA_PERMISSIONS_REQUEST permission granted.");
          takePictureIntent();
        } else {
          Log.d(TAG, "CAMERA_PERMISSIONS_REQUEST permission denied.");
        }

        break;
      default:
        Log.d(TAG, "Unknown request code: " + requestCode);
        break;
    }
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onBookEntityActionComplete(String message) {

    Log.d(TAG, "++onBookEntityActionComplete(String)");
    showDismissableSnackbar(message);
  }

  @Override
  public void onBookEntityAddedToLibrary(BookEntity bookEntity) {

    if (bookEntity == null) {
      Log.d(TAG, "++onUserBookAddedToLibrary(null)");
      showDismissableSnackbar(getString(R.string.err_add_book));
    } else {
      Log.d(TAG, "++onUserBookAddedToLibrary(BookEntity)");
//      mBookEntityList.add(bookEntity);
    }
  }

  @Override
  public void onBookEntityInit(boolean isSuccessful) {

    Log.d(TAG, "++onBookEntityInit(boolean)");
    mProgressBar.setIndeterminate(false);
  }

  @Override
  public void onBookEntityRemoved(BookEntity bookEntity) {

    mProgressBar.setIndeterminate(false);
    if (bookEntity == null) {
      Log.d(TAG, "++onBookEntityRemoved(null)");
      showDismissableSnackbar(getString(R.string.err_remove_book));
    } else {
      Log.d(TAG, "++onBookEntityRemoved(BookEntity)");
//      mBookEntityList.remove(bookEntity);
      replaceFragment(BookEntityListFragment.newInstance());
    }
  }

  @Override
  public void onBookEntityStarted() {

    Log.d(TAG, "++onBookEntityStarted()");
    mProgressBar.setIndeterminate(true);
  }

  @Override
  public void onBookEntityUpdated(BookEntity updatedBookEntity) {

    mProgressBar.setIndeterminate(false);
    if (updatedBookEntity == null) {
      Log.d(TAG, "++onBookEntityUpdated(null)");
      showDismissableSnackbar(getString(R.string.err_update_book));
    } else {
      Log.d(TAG, "++onBookEntityUpdated(BookEntity)");
      ArrayList<BookEntity> updatedBookEntityList = new ArrayList<>();
//      for (BookEntity bookEntity : mBookEntityList) {
//        if (bookEntity.VolumeId.equals(updatedBookEntity.VolumeId)) {
//          updatedBookEntityList.add(updatedBookEntity);
//        } else {
//          updatedBookEntityList.add(bookEntity);
//        }
//      }

      replaceFragment(BookEntityListFragment.newInstance());
    }
  }

  @Override
  public void onBookEntityListAddBook() {

    Log.d(TAG, "++onBookEntityListItemSelected()");
    replaceFragment(QueryFragment.newInstance());
  }

  @Override
  public void onBookEntityListItemSelected(BookEntity bookEntity) {

    Log.d(TAG, "++onBookEntityListItemSelected(BookEntity)");
    mProgressBar.setIndeterminate(false);
    setTitle(getString(R.string.fragment_book));
    replaceFragment(BookEntityFragment.newInstance(mUser.Id, bookEntity));
  }

  @Override
  public void onBookEntityListPopulated(int size) {

    Log.d(TAG, "++onBookEntityListPopulated(int)");
    mProgressBar.setIndeterminate(false);
    if (size == 0) {
      Snackbar.make(
        findViewById(R.id.main_drawer_layout),
        getString(R.string.err_no_data),
        Snackbar.LENGTH_LONG)
        .setAction(
          getString(R.string.add),
          view -> replaceFragment(QueryFragment.newInstance()))
        .show();
    }
  }

  @Override
  public void onBookEntityListSynchronize() {

    Log.d(TAG, "++onBookEntityListItemSelected()");
  }

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
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
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

    AlertDialog alert = alertDialogBuilder.create();
    alert.show();
  }

  @Override
  public void onQueryTakePicture() {

    Log.d(TAG, "++onQueryTakePicture()");
    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      checkDevicePermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
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

  public void writeComplete(ArrayList<BookEntity> bookEntityList) {

    Log.d(TAG, "++writeComplete(ArrayList<BookEntity>)");
    mProgressBar.setIndeterminate(false);
//    mBookEntityList = bookEntityList;
    replaceFragment(BookEntityListFragment.newInstance());
  }

  /*
      Private Method(s)
   */
  private void checkDevicePermission(String permission, int permissionCode) {

    Log.d(TAG, "++checkDevicePermission(String, int)");
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        Snackbar.make(
          findViewById(R.id.main_drawer_layout),
          getString(R.string.permission_denied_explanation),
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              MainActivity.this,
              new String[]{permission},
              permissionCode))
          .show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{permission}, permissionCode);
      }
    } else {
      switch (permissionCode) {
        case CAMERA_PERMISSIONS_REQUEST:
          Log.d(TAG, "Permission granted: " + permission);
          takePictureIntent();
          break;
      }
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

  private void readLocalLibrary() {

    Log.d(TAG, "++readLocalLibrary()");
    String parsableString;
    String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
    File file = new File(getFilesDir(), resourcePath);
    Log.d(TAG, "Loading " + file.getAbsolutePath());
//    mBookEntityList = new ArrayList<>();
    try {
      if (file.exists() && file.canRead()) {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        while ((parsableString = bufferedReader.readLine()) != null) { //process line
          if (parsableString.startsWith("--")) { // comment line; ignore
            continue;
          }

          List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
          if (elements.size() != BaseActivity.SCHEMA_FIELDS) {
            Log.d(TAG, "Local library schema mismatch. Got: " + elements.size());
            continue;
          }

          BookEntity bookEntity = new BookEntity();
          bookEntity.VolumeId = elements.remove(0);
          bookEntity.ISBN_8 = elements.remove(0);
          bookEntity.ISBN_13 = elements.remove(0);
          bookEntity.LCCN = elements.remove(0);
          bookEntity.Title = elements.remove(0);
//          bookEntity.Authors = new ArrayList<>(Arrays.asList(elements.remove(0).split(",")));
//          bookEntity.Categories = new ArrayList<>(Arrays.asList(elements.remove(0).split(",")));
          bookEntity.AddedDate = Long.parseLong(elements.remove(0));
          bookEntity.HasRead = Boolean.parseBoolean(elements.remove(0));
          bookEntity.IsOwned = Boolean.parseBoolean(elements.remove(0));
          bookEntity.PublishedDate = elements.remove(0);
//          bookEntity.Publisher = elements.remove(0);
          bookEntity.UpdatedDate = Long.parseLong(elements.remove(0));

          // attempt to locate this book in existing list
          boolean bookFound = false;
//          for (BookEntity book : mBookEntityList) {
//            if (book.VolumeId.equals(bookEntity.VolumeId)) {
//              bookFound = true;
//              break;
//            }
//          }

          if (!bookFound) {
//            mBookEntityList.add(bookEntity);
//            Log.d(TAG, "Adding %s to user book collection.", bookEntity.toString());
          }
        }
      } else {
        Log.d(TAG, "%s does not exist yet: " + resourcePath);
      }
    } catch (Exception e) {
      Log.w(TAG, "Exception when reading local library data.");
      Crashlytics.logException(e);
      mProgressBar.setIndeterminate(false);
    } finally {
//      if (mBookEntityList == null || mBookEntityList.size() == 0) {
//        readServerLibrary(); // attempt to get user's book library from cloud
//      } else {
//        mBookEntityList.sort(new SortUtils.ByBookName());
        mProgressBar.setIndeterminate(false);
        replaceFragment(BookEntityListFragment.newInstance());
//      }
    }
  }

  private void replaceFragment(Fragment fragment) {

    Log.d(TAG, "++replaceFragment(Fragment)");
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.main_fragment_container, fragment)
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
          .setPositiveButton(R.string.ok, (dialog, id) -> useFirebaseBarcodeScanning())
          .setNegativeButton(R.string.cancel, (dialog, id) -> {
            mProgressBar.setIndeterminate(false);
            dialog.cancel();
          });

        AlertDialog alert = alertDialogBuilder.create();
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
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
          .setPositiveButton(R.string.ok, (dialog, id) -> useFirebaseTextScanning())
          .setNegativeButton(R.string.cancel, (dialog, id) -> {
            mProgressBar.setIndeterminate(false);
            dialog.cancel();
          });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
      } else {
        useFirebaseTextScanning();
      }
    } else {
      Log.w(TAG, getString(R.string.err_image_not_loaded));
      showDismissableSnackbar(getString(R.string.err_image_not_loaded));
    }
  }

  private void showDismissableSnackbar(String message) {

    mProgressBar.setIndeterminate(false);
    Log.w(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.main_drawer_layout),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
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
