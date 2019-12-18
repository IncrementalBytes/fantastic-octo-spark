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
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import net.whollynugatory.android.cloudycurator.BuildConfig;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.CloudyCuratorException;
import net.whollynugatory.android.cloudycurator.common.GetDataTask;
import net.whollynugatory.android.cloudycurator.common.GetPropertyIdsTask;
import net.whollynugatory.android.cloudycurator.db.CuratorDatabase;
import net.whollynugatory.android.cloudycurator.db.CuratorRepository;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.entity.UserEntity;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.fragments.AddBookEntityFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.BookEntityListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.LibrarianFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.QueryFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ResultListFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.ScanResultsFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.UpdateBookEntityFragment;
import net.whollynugatory.android.cloudycurator.ui.fragments.UserPreferenceFragment;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
  AddBookEntityFragment.OnAddBookEntityListener,
  BookEntityListFragment.OnBookEntityListListener,
  QueryFragment.OnQueryListener,
  ResultListFragment.OnResultListListener,
  ScanResultsFragment.OnScanResultsListener,
  UpdateBookEntityFragment.OnUpdateBookEntityListener,
  UserPreferenceFragment.OnPreferencesListener {

  private static final String TAG = BaseActivity.BASE_TAG + "MainActivity";

  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private int mAttempts;
  private UserEntity mUser;

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
        if (fragmentClassName.equals(AddBookEntityFragment.class.getName())) {
          setTitle(R.string.fragment_book_add);
        } else if (fragmentClassName.equals(BookEntityListFragment.class.getName())) {
          setTitle(getString(R.string.fragment_book_list));
        } else if (fragmentClassName.equals(LibrarianFragment.class.getName())) {
          setTitle(getString(R.string.fragment_librarian));
        } else if (fragmentClassName.equals(QueryFragment.class.getName())) {
          setTitle(getString(R.string.fragment_query));
        } else if (fragmentClassName.equals(ResultListFragment.class.getName())) {
          setTitle(R.string.fragment_select_book);
        } else if (fragmentClassName.equals(ScanResultsFragment.class.getName())) {
          setTitle(R.string.fragment_select_text);
        } else if (fragmentClassName.equals(UpdateBookEntityFragment.class.getName())) {
          setTitle(R.string.fragment_book_update);
        } else if (fragmentClassName.equals(UserPreferenceFragment.class.getName())) {
          setTitle(getString(R.string.fragment_preferences));
        }
      }
    });

    mUser = new UserEntity();
    mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
    mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
    mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);

    // check for additional extras to assist with flow
    if (getIntent().hasExtra(BaseActivity.ARG_BOOK)) {
      BookDetail bookDetail = (BookDetail)getIntent().getSerializableExtra(BaseActivity.ARG_BOOK_DETAIL);
      replaceFragment(UpdateBookEntityFragment.newInstance(bookDetail));
    } else if (getIntent().hasExtra(BaseActivity.ARG_RESULT_LIST)) {
      ArrayList<BookEntity> bookEntityList = (ArrayList<BookEntity>)getIntent().getSerializableExtra(BaseActivity.ARG_RESULT_LIST);
      replaceFragment(ResultListFragment.newInstance(bookEntityList));
    } else {
      checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, BaseActivity.REQUEST_STORAGE_PERMISSIONS);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    Log.d(TAG, "++onCreateOptionsMenu(Menu)");
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    Log.d(TAG, "++onOptionsItemSelected(MenuItem)");
    switch (item.getItemId()) {
      case R.id.action_home:
        new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
        break;
      case R.id.action_add:
        checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
        break;
      case R.id.action_preferences:
        startActivity(new Intent(this, SettingsActivity.class));
        break;
      case R.id.action_logout:
        AlertDialog dialog = new AlertDialog.Builder(this)
          .setMessage(R.string.logout_message)
          .setPositiveButton(android.R.string.yes, (dialog1, which) -> {

            // sign out of firebase
            FirebaseAuth.getInstance().signOut();

            // sign out of google, if necessary
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              .requestIdToken(getString(R.string.default_web_client_id))
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

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(TAG, "++onActivityResult(int, int, Intent)");
    if (requestCode == BaseActivity.REQUEST_BOOK_ADD) { // pick up any change to tutorial
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

      new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
    } else {
      Log.w(TAG, String.format(Locale.US, "Unexpected activity request: %d", requestCode));
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
  public void onAddBookEntityAddToLibrary(BookEntity bookEntity) {

    Log.d(TAG, "++onAddBookEntityAddToLibrary(BookEntity)");
    if (bookEntity.PublisherId < 0 || bookEntity.CategoryId < 0 || bookEntity.AuthorId < 0) {
      new GetPropertyIdsTask(this, CuratorDatabase.getInstance(this), bookEntity).execute();
    } else {
      CuratorRepository.getInstance(this).insertBookEntity(bookEntity);
      new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
    }
  }

  @Override
  public void onAddBookEntityInit(boolean isSuccessful) {

    Log.d(TAG, "++onAddBookEntityInit(boolean)");
    if (mProgressBar != null) {
      mProgressBar.setIndeterminate(false);
    }
  }

  @Override
  public void onAddBookEntityStarted() {

    Log.d(TAG, "++onAddBookEntityStarted()");
    if (mProgressBar != null) {
      mProgressBar.setIndeterminate(true);
    }
  }

  @Override
  public void onBookEntityListAddBook() {

    Log.d(TAG, "++onBookEntityListItemSelected()");
    checkForPermission(Manifest.permission.CAMERA, BaseActivity.REQUEST_CAMERA_PERMISSIONS);
  }

  @Override
  public void onBookEntityListDeleteBook(String volumeId) {

    Log.d(TAG, "++onBookEntityListDeleteBook(String)");
    CuratorRepository.getInstance(this).deleteBook(volumeId);
    new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
  }

  @Override
  public void onBookEntityListItemSelected(BookDetail bookDetail) {

    Log.d(TAG, "++onBookEntityListItemSelected(BookDetail)");
    mProgressBar.setIndeterminate(false);
    setTitle(getString(R.string.fragment_book));
    replaceFragment(UpdateBookEntityFragment.newInstance(bookDetail));
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
    replaceFragment(AddBookEntityFragment.newInstance(bookEntity));
  }

  @Override
  public void onScanResultsItemSelected(String searchText) {

    Log.d(TAG, "++onScanResultsItemSelected(String)");
    BookEntity bookEntity = new BookEntity();
    bookEntity.Title = searchText;
    queryInUserBooks(bookEntity);
  }

  @Override
  public void onUpdateBookEntityActionComplete(String message) {

    Log.d(TAG, "++onUpdateBookEntityActionComplete(String)");
    showDismissableSnackbar(message);
  }

  @Override
  public void onUpdateBookEntityInit(boolean isSuccessful) {

    Log.d(TAG, "++onUpdateBookEntityInit(boolean)");
    if (mProgressBar != null) {
      mProgressBar.setIndeterminate(false);
    }
  }

  @Override
  public void onUpdateBookEntityRemove(String volumeId) {

    Log.d(TAG, "++onUpdateBookEntityRemove(String)");
    mProgressBar.setIndeterminate(false);
    CuratorRepository.getInstance(this).deleteBook(volumeId);
    new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
  }

  @Override
  public void onUpdateBookEntityStarted() {

    Log.d(TAG, "++onUpdateBookEntityStarted()");
    if (mProgressBar != null) {
      mProgressBar.setIndeterminate(true);
    }
  }

  @Override
  public void onUpdateBookEntityUpdate(BookDetail updatedBookDetail) {

    Log.d(TAG, "++onUpdateBookEntityUpdate(BookDetail)");
    mProgressBar.setIndeterminate(false);
    if (updatedBookDetail == null) {
      showDismissableSnackbar(getString(R.string.err_update_book));
    } else {
      BookEntity bookEntity = BookEntity.fromBookDetail(updatedBookDetail);
      CuratorRepository.getInstance(this).insertBookEntity(bookEntity);
      new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
    }
  }

  @Override
  public void onPreferenceChanged() throws CloudyCuratorException {

    Log.d(TAG, "++onPreferenceChanged()");
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    if (preferences.contains(UserPreferenceFragment.FORCE_EXCEPTION_PREFERENCE)) {
      boolean throwException = preferences.getBoolean(UserPreferenceFragment.FORCE_EXCEPTION_PREFERENCE, false);
      if (BuildConfig.DEBUG && throwException) {
        throw new CloudyCuratorException("Testing the exceptional expection-ness");
      }
    }
  }

  /*
    Public Method(s)
   */
  public void getPropertyIdComplete(BookEntity bookEntity) {

    Log.d(TAG, "++getPropertyIdComplete(BookEntity)");
    if (bookEntity.AuthorId > 0 && bookEntity.PublisherId > 0 && bookEntity.CategoryId > 0) {
      // TODO: timing issue, insert is it's own task so calling GetDataTask too soon won't get insert, etc.
      CuratorRepository.getInstance(this).insertBookEntity(bookEntity);
      new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
    } else {
      Log.w(TAG, "Property retrieval task failed.");
    }
  }

  public void getDataComplete(ArrayList<BookDetail> bookDetailList) {

    Log.d(TAG, "++getDataComplete(ArrayList<BookDetail>)");
    if (bookDetailList.size() == 0 && mAttempts < 5) {
      mAttempts++;
      new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
    } else {
      mAttempts = 0;
      replaceFragment(BookEntityListFragment.newInstance(bookDetailList));
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
          startActivity(new Intent(MainActivity.this, LiveBarcodeScanningActivity.class));
          break;
        case BaseActivity.REQUEST_STORAGE_PERMISSIONS:
          new GetDataTask(this, CuratorRepository.getInstance(this)).execute();
          break;
      }
    }
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

    mProgressBar.setIndeterminate(false);
    Log.w(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.main_fragment_container),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }
}
