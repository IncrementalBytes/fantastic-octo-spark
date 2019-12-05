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
  BookEntityListFragment.OnBookEntityListListener {

  private static final String TAG = BASE_TAG + "MainActivity";

  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

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

    replaceFragment(BookEntityListFragment.newInstance());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
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
        findViewById(R.id.main_fragment_container),
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

  /*
      Private Method(s)
   */
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
