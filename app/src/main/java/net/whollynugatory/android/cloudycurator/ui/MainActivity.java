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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import net.whollynugatory.android.cloudycurator.PreferenceUtils;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.GoogleBookApiTask;
import net.whollynugatory.android.cloudycurator.common.PathUtils;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.entity.UserEntity;
import net.whollynugatory.android.cloudycurator.db.viewmodel.BookListViewModel;
import net.whollynugatory.android.cloudycurator.ui.fragments.BarcodeScanFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ItemListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.LibrarianFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ManualSearchFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.QueryFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ResultListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.UserPreferenceFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
  BarcodeScanFragment.OnBarcodeScanListener,
  ItemListFragment.OnItemListListener,
  ManualSearchFragment.OnManualSearchListener,
  NavigationView.OnNavigationItemSelectedListener,
  QueryFragment.OnQueryListener,
  ResultListFragment.OnResultListListener {

  private static final String TAG = BaseActivity.BASE_TAG + "MainActivity";

  private TextView mNavigationBooksText;
  private DrawerLayout mDrawerLayout;
  private NavigationView mNavigationView;
  private Snackbar mSnackbar;

  private BookListViewModel mBookListViewModel;

  private Bitmap mImageBitmap;
  private int mRotationAttempts;
  private UserEntity mUser;

  /*
      AppCompatActivity Override(s)
   */
  @Override
  public void onBackPressed() {

    Log.d(TAG, "++onBackPressed()");
    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
      mDrawerLayout.closeDrawer(GravityCompat.START);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_main);

    mDrawerLayout = findViewById(R.id.main_drawer_layout);
    BottomNavigationView bottomNavigationView = findViewById(R.id.main_bottom_navigation);
    Toolbar toolbar = findViewById(R.id.main_toolbar);

    setSupportActionBar(toolbar);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
      this,
      mDrawerLayout,
      toolbar,
      R.string.navigation_drawer_open,
      R.string.navigation_drawer_close);
    mDrawerLayout.addDrawerListener(toggle);
    toggle.syncState();
    mNavigationView = findViewById(R.id.main_navigation_view);
    mNavigationView.setNavigationItemSelectedListener(this);

    getSupportFragmentManager().addOnBackStackChangedListener(() -> {
      Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
      if (fragment != null) {
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(LibrarianFragment.class.getName())) {
          setTitle(getString(R.string.fragment_librarian));
        } else if (fragmentClassName.equals(ManualSearchFragment.class.getName())) {
          setTitle(getString(R.string.fragment_manual));
        } else if (fragmentClassName.equals(QueryFragment.class.getName())) {
          setTitle(getString(R.string.fragment_query));
        } else if (fragmentClassName.equals(ResultListFragment.class.getName())) {
          setTitle(R.string.fragment_select_book);
        } else if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
          setTitle(getString(R.string.fragment_settings));
        } else {
          setTitle(getString(R.string.app_name));
        }
      }
    });

    mBookListViewModel = ViewModelProviders.of(this).get(BookListViewModel.class);

    bottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {

      Log.d(TAG, "++onNavigationItemSelectedListener(MenuItem)");
      switch (menuItem.getItemId()) {
        case R.id.navigation_authors:
          replaceFragment(ItemListFragment.newInstance(ItemListFragment.ItemType.Authors));
          return true;
        case R.id.navigation_categories:
          replaceFragment(ItemListFragment.newInstance(ItemListFragment.ItemType.Categories));
          return true;
        case R.id.navigation_recent:
          replaceFragment(ItemListFragment.newInstance());
          return true;
        case R.id.navigation_settings:
          replaceFragment(UserPreferenceFragment.newInstance());
          return true;
      }

      return false;
    });


    mUser = new UserEntity();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);

    View navigationHeaderView = mNavigationView.inflateHeaderView(R.layout.main_navigation_header);
    TextView navigationFullName = navigationHeaderView.findViewById(R.id.navigation_text_full_name);
    navigationFullName.setText(mUser.FullName);
    mNavigationBooksText = navigationHeaderView.findViewById(R.id.navigation_text_books);

    String queryPath = PathUtils.combine(UserEntity.ROOT, mUser.Id);
    FirebaseFirestore.getInstance().document(queryPath).get().addOnCompleteListener(this, task -> {

      if (task.isSuccessful() && task.getResult() != null) {
        UserEntity user = task.getResult().toObject(UserEntity.class);
        if (user != null) {
          mUser.IsLibrarian = user.IsLibrarian;
        }
      }

      // enable options if user has permissions
      if (mUser.IsLibrarian) {
        // TODO: add access to debug (image from camera, etc.)
        MenuItem librarianMenu = mNavigationView.getMenu().findItem(R.id.navigation_menu_librarian);
        if (librarianMenu != null) {
          librarianMenu.setVisible(true);
        }
      }

      checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_STORAGE_PERMISSIONS);
    });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {

    Log.d(TAG, "++onNavigationItemSelected(MenuItem)");
    switch (item.getItemId()) {
      case R.id.navigation_menu_home:
        replaceFragment(ItemListFragment.newInstance());
        break;
      case R.id.navigation_menu_add:
        checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
        break;
      case R.id.navigation_menu_settings:
        replaceFragment(UserPreferenceFragment.newInstance());
        break;
      case R.id.navigation_menu_librarian:
        replaceFragment(LibrarianFragment.newInstance());
        break;
      case R.id.navigation_menu_logout:
        AlertDialog dialog = new AlertDialog.Builder(this)
          .setMessage(R.string.logout_message)
          .setPositiveButton(android.R.string.yes, (dialog1, which) -> {

            // sign out of firebase
            FirebaseAuth.getInstance().signOut();

            // sign out of google, if necessary
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              //.requestIdToken(getString(R.string.default_web_client_id))
              .requestIdToken("1079143607884-n6m9tirs482fdn65bf54lnvfrk4u8e54.apps.googleusercontent.com")
              .requestEmail()
              .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {

              // return to sign-in activity
              startActivity(new Intent(getApplicationContext(), SignInActivity.class));
              finish();
            });
          })
          .setNegativeButton(android.R.string.no, null)
          .create();
        dialog.show();
        break;
    }

    mDrawerLayout.closeDrawer(GravityCompat.START);
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(TAG, "++onActivityResult(int, int, Intent)");
    switch (requestCode) {
      case BaseActivity.REQUEST_BOOK_ADD:
        String message = null;
        BookEntity bookEntity = null;
        if (data != null) {
          if (data.hasExtra(BaseActivity.ARG_MESSAGE)) {
            message = data.getStringExtra(BaseActivity.ARG_MESSAGE);
          }

          if (data.hasExtra(BaseActivity.ARG_BOOK)) {
            bookEntity = (BookEntity) data.getSerializableExtra(BaseActivity.ARG_BOOK);
          }
        }

        switch (resultCode) {
          case BaseActivity.RESULT_ADD_SUCCESS:
            if (bookEntity == null) {
              showDismissableSnackbar(getString(R.string.err_add_book));
            }

            break;
          case BaseActivity.RESULT_ADD_FAILED:
            if (message != null && message.length() > 0) {
              showDismissableSnackbar(message);
            } else {
              Log.e(TAG, "Activity return with incomplete data or no message was sent.");
              showDismissableSnackbar(getString(R.string.message_unknown_activity_result));
            }

            break;
          case RESULT_CANCELED:
            if (message != null && message.length() > 0) {
              showDismissableSnackbar(message);
            }

            break;
        }

        replaceFragment(ItemListFragment.newInstance());
        break;

      case BaseActivity.REQUEST_IMAGE_CAPTURE:
        if (resultCode != RESULT_OK) {
          Log.d(TAG, "User canceled camera intent.");
        } else {
          File f = new File(getString(R.string.debug_path), data.getStringExtra(BaseActivity.ARG_DEBUG_FILE_NAME));
          Log.d(TAG, "Using " + f.getAbsolutePath());
          try {
            mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
          } catch (FileNotFoundException e) {
            Crashlytics.logException(e);
          }

          if (mImageBitmap != null) {
            Bitmap emptyBitmap = Bitmap.createBitmap(
              mImageBitmap.getWidth(),
              mImageBitmap.getHeight(),
              mImageBitmap.getConfig());
            if (!mImageBitmap.sameAs(emptyBitmap)) {
              LayoutInflater layoutInflater = LayoutInflater.from(this);
              View promptView = layoutInflater.inflate(R.layout.dialog_debug_image, null);
              ImageView imageView = promptView.findViewById(R.id.debug_dialog_image);
              BitmapDrawable bmd = new BitmapDrawable(this.getResources(), mImageBitmap);
              imageView.setImageDrawable(bmd);
              AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
              alertDialogBuilder.setView(promptView);
              alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> useFirebaseBarcodeScanning())
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

              AlertDialog alert = alertDialogBuilder.create();
              alert.show();
            } else {
              Log.w(TAG, getString(R.string.err_image_empty));
            }
          } else {
            Log.w(TAG, getString(R.string.err_image_not_found));
          }
        }
        break;

      default:
        Log.e(TAG, "Unknown request code received:" + requestCode);
        break;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    Log.d(TAG, "++onRequestPermissionsResult(int, String[], int[])");
    switch (requestCode) {
      case BaseActivity.REQUEST_CAMERA_PERMISSIONS:
        checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
        break;
      case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
        checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_STORAGE_PERMISSIONS);
        break;
    }
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onBarcodeManual() {

    Log.d(TAG, "++onBarcodeManual()");
    replaceFragment(ManualSearchFragment.newInstance());
  }

  @Override
  public void onBarcodeScanClose() {

    Log.d(TAG, "++onBarcodeScanClose()");
    replaceFragment(ItemListFragment.newInstance());
  }

  @Override
  public void onBarcodeScanned(String barcodeValue) {

    Log.d(TAG, "++onBarcodeScanned(String)");
    lookupBarcode(barcodeValue);
  }

  @Override
  public void onBarcodeScanSettings() {

    Log.d(TAG, "++onBarcodeScanSettings()");
    replaceFragment(UserPreferenceFragment.newInstance());
  }

  @Override
  public void onItemListAddBook() {

    Log.d(TAG, "++onItemListAddBook()");
    checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
  }

  @Override
  public void onItemListAuthorSelected(String authorName) {

    Log.d(TAG, "++onItemListAuthorSelected(String)");
    replaceFragment(ItemListFragment.newInstance(ItemListFragment.ItemType.Authors, authorName));
  }

  @Override
  public void onItemListCategorySelected(String categoryName) {

    Log.d(TAG, "++onItemListCategorySelected(String)");
    replaceFragment(ItemListFragment.newInstance(ItemListFragment.ItemType.Categories, categoryName));
  }

  @Override
  public void onItemListPopulated(int size) {

    Log.d(TAG, "++onItemListPopulated(int)");
    mNavigationBooksText.setText(getResources().getQuantityString(R.plurals.navigation_book_format, size, size));
  }

  @Override
  public void onManualSearchContinue(String barcodeValue) {

    Log.d(TAG, "++onManualSearchContinue(String)");
    lookupBarcode(barcodeValue);
  }

  @Override
  public void onQueryActionComplete(String message) {

    Log.d(TAG, "++onQueryActionComplete(String)");
    if (!message.isEmpty()) {
      showDismissableSnackbar(message);
    }
  }

  @Override
  public void onQueryShowManualDialog() {

    Log.d(TAG, "++onQueryShowManualDialog()");
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
      .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

    androidx.appcompat.app.AlertDialog alert = alertDialogBuilder.create();
    alert.show();
  }

  @Override
  public void onQueryTakePicture() {

    Log.d(TAG, "++onQueryTakePicture()");
    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
      checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
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
  public void onResultListItemSelected(BookEntity bookEntity) {

    Log.d(TAG, "++onResultListItemSelected(BookEntity)");
    replaceFragment(ItemListFragment.newInstance());
  }

  /*
    Public Method(s)
   */
  public void retrieveBooksComplete(ArrayList<BookEntity> bookEntityList) {

    Log.d(TAG, "++retrieveBooksComplete(ArrayList<BookEntity>)");
    if (bookEntityList.size() == 0) {
      Log.d(TAG, "Book entity list is empty after Google Book API query.");
    } else {
      if (bookEntityList.size() == BaseActivity.MAX_RESULTS) {
        // TODO: message about only showing max results
      }

      replaceFragment(ResultListFragment.newInstance(bookEntityList));
    }
  }

  /*
      Private Method(s)
   */
  private void checkForPermission(String permission, int permissionRequest) {

    Log.d(TAG, "++checkForPermission(String, int)");
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
        String requestMessage = getString(R.string.permission_default);
        switch (permissionRequest) {
          case BaseActivity.REQUEST_CAMERA_PERMISSIONS:
            requestMessage = getString(R.string.permission_camera);
            break;
          case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
            requestMessage = getString(R.string.permission_storage);
            break;
        }

        Snackbar.make(
          findViewById(R.id.main_fragment_container),
          requestMessage,
          Snackbar.LENGTH_INDEFINITE)
          .setAction(
            getString(R.string.ok),
            view -> ActivityCompat.requestPermissions(
              MainActivity.this,
              new String[]{permission},
              permissionRequest))
          .show();
      } else {
        ActivityCompat.requestPermissions(this, new String[]{permission}, permissionRequest);
      }
    } else {
      Log.d(TAG, "Permission granted: " + permission);
      switch (permissionRequest) {
        case BaseActivity.REQUEST_CAMERA_PERMISSIONS:
          if (!PreferenceUtils.getUseCamera(this)) {
            replaceFragment(ManualSearchFragment.newInstance());
          } else {
            if (PreferenceUtils.getCameraBypass(this)) {
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
            } else {
              replaceFragment(BarcodeScanFragment.newInstance());
            }
          }

          break;
        case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
          replaceFragment(ItemListFragment.newInstance());
          break;
      }
    }
  }

  private void hideKeyboard() {

    Log.d(TAG, "++hideKeyboard()");
    InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
    View view = this.getCurrentFocus();
    if (view == null) {
      view = new View(this);
    }

    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  private void lookupBarcode(String barcodeValue) {

    Log.d(TAG, "++lookupBarcode(String)");
    hideKeyboard();
    mBookListViewModel.find(barcodeValue).observe(this, bookEntity -> {

      if (bookEntity == null) {
        BookEntity queryForBook = new BookEntity();
        if (barcodeValue != null && barcodeValue.length() == 8) {
          queryForBook.ISBN_8 = barcodeValue;
        } else if (barcodeValue != null && barcodeValue.length() == 13) {
          queryForBook.ISBN_13 = barcodeValue;
        }

        new GoogleBookApiTask(this, queryForBook).execute();
      } // TODO: found book, show summary fragment
    });
  }

  private void queryInUserBooks(BookEntity bookEntity) {

    Log.d(TAG, "++queryInUserBooks(BookEntity)");
    //new QueryBookDatabaseTask(this, CuratorRepository.getInstance(this), bookEntity).execute();
  }

  private void replaceFragment(Fragment fragment) {

    Log.d(TAG, "++replaceFragment(Fragment)");
    getSupportFragmentManager()
      .beginTransaction()
      .replace(R.id.main_fragment_container, fragment)
      .addToBackStack(null)
      .commit();
  }

  private void showDismissableSnackbar(String message) {

    Log.w(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.main_fragment_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }

  private void useFirebaseBarcodeScanning() {

    Log.d(TAG, "++useFirebaseBarcodeScanning()");
    FirebaseVisionBarcodeDetectorOptions options =
      new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_EAN_8, FirebaseVisionBarcode.FORMAT_EAN_13)
        .build();
    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

    com.google.android.gms.tasks.Task<java.util.List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
      .addOnCompleteListener(task -> {

        if (task.isSuccessful() && task.getResult() != null) {
          String barcodeValue = "";
          for (FirebaseVisionBarcode barcode : task.getResult()) {
            if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_ISBN) {
              barcodeValue = barcode.getDisplayValue();
              Log.d(TAG, "Found a bar code: " + barcodeValue);
            } else {
              Log.w(TAG, "Unexpected bar code: " + barcode.getDisplayValue());
            }
          }

          if (barcodeValue != null && !barcodeValue.isEmpty()) {
            mRotationAttempts = 0;
            onBarcodeScanned(barcodeValue);
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
            useFirebaseBarcodeScanning();
          } else {
            mRotationAttempts = 0;
          }
        } else {
          // TODO: handle detectInImage failure
        }
      });
  }
}
