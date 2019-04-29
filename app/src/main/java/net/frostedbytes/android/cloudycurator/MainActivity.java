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

package net.frostedbytes.android.cloudycurator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import net.frostedbytes.android.cloudycurator.common.RetrieveBookDataTask;
import net.frostedbytes.android.cloudycurator.common.WriteToLocalLibraryTask;
import net.frostedbytes.android.cloudycurator.fragments.CameraFragment;
import net.frostedbytes.android.cloudycurator.fragments.CloudyBookFragment;
import net.frostedbytes.android.cloudycurator.fragments.CloudyBookListFragment;
import net.frostedbytes.android.cloudycurator.fragments.LibrarianFragment;
import net.frostedbytes.android.cloudycurator.fragments.QueryFragment;
import net.frostedbytes.android.cloudycurator.fragments.ResultListFragment;
import net.frostedbytes.android.cloudycurator.fragments.ScanResultsFragment;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.common.LogUtils;
import net.frostedbytes.android.cloudycurator.common.PathUtils;
import net.frostedbytes.android.cloudycurator.common.SortUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements
    CameraFragment.OnCameraListener,
    CloudyBookFragment.OnCloudyBookListener,
    CloudyBookListFragment.OnCloudyBookListListener,
    NavigationView.OnNavigationItemSelectedListener,
    QueryFragment.OnQueryListener,
    ResultListFragment.OnResultListListener,
    ScanResultsFragment.OnScanResultsListener {

    private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

    static final int WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 12;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ProgressBar mProgressBar;
    private Snackbar mSnackbar;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ArrayList<CloudyBook> mCloudyBookList;
    private File mCurrentImageFile;
    private Bitmap mImageBitmap;
    private int mRotationAttempts;
    private User mUser;

    /*
        AppCompatActivity Override(s)
     */
    @Override
    public void onBackPressed() {

        LogUtils.debug(TAG, "++onBackPressed()");
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                finish();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogUtils.debug(TAG, "++onCreate(Bundle)");
        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mDrawerLayout = findViewById(R.id.main_drawer_layout);
        mProgressBar = findViewById(R.id.main_progress);
        mProgressBar.setIndeterminate(true);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
            if (fragment != null) {
                updateTitleAndDrawer(fragment);
            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mUser = new User();
        mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_FIREBASE_USER_ID);
        mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
        mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);

        // update the navigation header
        mNavigationView = findViewById(R.id.main_navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        View navigationHeaderView = mNavigationView.inflateHeaderView(R.layout.main_navigation_header);
        TextView navigationFullName = navigationHeaderView.findViewById(R.id.navigation_text_full_name);
        navigationFullName.setText(mUser.FullName);
        TextView navigationEmail = navigationHeaderView.findViewById(R.id.navigation_text_email);
        navigationEmail.setText(mUser.Email);
        TextView navigationVersion = navigationHeaderView.findViewById(R.id.navigation_text_version);
        navigationVersion.setText(BuildConfig.VERSION_NAME);

        // get user's permissions
        getUserPermissions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mCloudyBookList = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        LogUtils.debug(TAG, "++onNavigationItemSelected(%s)", item.getTitle());
        switch (item.getItemId()) {
            case R.id.navigation_menu_home:
                replaceFragment(CloudyBookListFragment.newInstance(mCloudyBookList));
                break;
            case R.id.navigation_menu_add:
                mProgressBar.setIndeterminate(false);
                replaceFragment(QueryFragment.newInstance());
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        LogUtils.debug(TAG, "++onRequestPermissionResult(int, String[], int[])");
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogUtils.debug(TAG, "WRITE_EXTERNAL_STORAGE permission granted.");
                readLocalLibrary();
            } else {
                LogUtils.debug(TAG, "WRITE_EXTERNAL_STORAGE permission denied.");
            }
        } else {
            LogUtils.debug(TAG, "Unknown request code: %d", requestCode);
        }
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onCameraImageAvailable(File imageFile) {

        LogUtils.debug(TAG, "++onCameraImageAvailable(%s)", imageFile.getAbsoluteFile());
        mCurrentImageFile = imageFile;
        if (BuildConfig.DEBUG) {
            File f = new File(getString(R.string.debug_path), getString(R.string.debug_file));
            try {
                mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(mCurrentImageFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
                String message = "Image was empty.";
                LogUtils.warn(TAG, message);
                replaceFragment(QueryFragment.newInstance());
                showDismissableSnackbar(message);
            }
        } else {
            String message = "Image does not exist.";
            if (mUser.IsLibrarian) {
                message = String.format(Locale.US, "%s %s", message, imageFile.getName());
            }

            LogUtils.warn(TAG, message);
            replaceFragment(QueryFragment.newInstance());
            showDismissableSnackbar(message);
        }
    }

    @Override
    public void onCameraInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onCloudyBookActionComplete(%s)", String.valueOf(isSuccessful));
        if (!isSuccessful) {
            showDismissableSnackbar(getString(R.string.err_camera));
        }
    }

    @Override
    public void onCloudyBookActionComplete(String message) {

        LogUtils.debug(TAG, "++onCloudyBookActionComplete(%s)", message);
        showDismissableSnackbar(message);
    }

    @Override
    public void onCloudyBookAddedToLibrary(CloudyBook cloudyBook) {

        if (cloudyBook == null) {
            LogUtils.debug(TAG, "++onUserBookAddedToLibrary(null)");
            showDismissableSnackbar(getString(R.string.err_add_cloudy_book));
        } else {
            LogUtils.debug(TAG, "++onUserBookAddedToLibrary(%s)", cloudyBook.toString());
            Bundle params = new Bundle();
            params.putString("action", "added");
            params.putString("cloudy_book", cloudyBook.toString());
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
            mCloudyBookList.add(cloudyBook);
            new WriteToLocalLibraryTask(this, mCloudyBookList).execute();
        }
    }

    @Override
    public void onCloudyBookInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onCloudyBookInit(%s)", String.valueOf(isSuccessful));
        mProgressBar.setIndeterminate(false);
    }

    @Override
    public void onCloudyBookRemoved(CloudyBook cloudyBook) {

        mProgressBar.setIndeterminate(false);
        if (cloudyBook == null) {
            LogUtils.debug(TAG, "++onCloudyBookRemoved(null)");
            showDismissableSnackbar(getString(R.string.err_remove_cloudy_book));
        } else {
            LogUtils.debug(TAG, "++onCloudyBookRemoved(%s)", cloudyBook.toString());
            Bundle params = new Bundle();
            params.putString("action", "removed");
            params.putString("cloudy_book", cloudyBook.toString());
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
            mCloudyBookList.remove(cloudyBook);
            new WriteToLocalLibraryTask(this, mCloudyBookList).execute();
            replaceFragment(CloudyBookListFragment.newInstance(mCloudyBookList));
        }
    }

    @Override
    public void onCloudyBookStarted() {

        LogUtils.debug(TAG, "++onCloudyBookStarted()");
        mProgressBar.setIndeterminate(true);
    }

    @Override
    public void onCloudyBookUpdated(CloudyBook updatedCloudyBook) {

        mProgressBar.setIndeterminate(false);
        if (updatedCloudyBook == null) {
            LogUtils.debug(TAG, "++onCloudyBookUpdated(null)");
            showDismissableSnackbar(getString(R.string.err_update_cloudy_book));
        } else {
            LogUtils.debug(TAG, "++onCloudyBookUpdated(%s)", updatedCloudyBook.toString());
            Bundle params = new Bundle();
            params.putString("action", "update");
            params.putString("cloudy_book", updatedCloudyBook.toString());
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
            ArrayList<CloudyBook> updatedCloudyBookList = new ArrayList<>();
            for (CloudyBook cloudyBook : mCloudyBookList) {
                if (cloudyBook.VolumeId.equals(updatedCloudyBook.VolumeId)) {
                    updatedCloudyBookList.add(updatedCloudyBook);
                } else {
                    updatedCloudyBookList.add(cloudyBook);
                }
            }

            new WriteToLocalLibraryTask(this, updatedCloudyBookList).execute();
            replaceFragment(CloudyBookListFragment.newInstance(updatedCloudyBookList));
        }
    }

    @Override
    public void onCloudyBookListAddBook() {

        LogUtils.debug(TAG, "++onCloudyBookListItemSelected()");
        replaceFragment(QueryFragment.newInstance());
    }

    @Override
    public void onCloudyBookListItemSelected(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++onCloudyBookListItemSelected(%s)", cloudyBook.toString());
        mProgressBar.setIndeterminate(false);
        setTitle(getString(R.string.fragment_cloudy_book));
        replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBook));
    }

    @Override
    public void onCloudyBookListPopulated(int size) {

        LogUtils.debug(TAG, "++onCloudyBookListPopulated(%d)", size);
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
    public void onCloudyBookListSynchronize() {

        LogUtils.debug(TAG, "++onCloudyBookListItemSelected()");
        readServerLibrary();
    }

    @Override
    public void onQueryActionComplete(String message) {

        LogUtils.debug(TAG, "++onQueryActionComplete(%s)", message);
        mProgressBar.setIndeterminate(false);
        if (!message.isEmpty()) {
            showDismissableSnackbar(message);
        }
    }

    @Override
    public void onQueryShowManualDialog() {

        LogUtils.debug(TAG, "++onQueryShowManualDialog()");
        mProgressBar.setIndeterminate(true);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.dialog_search_manual, null);
        EditText editText = promptView.findViewById(R.id.manual_dialog_edit_search);
        RadioGroup radioGroup = promptView.findViewById(R.id.manual_dialog_radio_search);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("OK", (dialog, id) -> {

                CloudyBook cloudyBook = new CloudyBook();
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.manual_dialog_radio_isbn:
                        String value = editText.getText().toString();
                        if (value.length() == 8) {
                            cloudyBook.ISBN_8 = value;
                        } else if (value.length() == 13) {
                            cloudyBook.ISBN_13 = value;
                        }

                        if (!cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) ||
                            !cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
                            queryInUserBooks(cloudyBook);
                        } else {
                            String message = "Invalid ISBN value.";
                            showDismissableSnackbar(message);
                        }

                        break;
                    case R.id.manual_dialog_radio_title:
                        cloudyBook.Title = editText.getText().toString();
                        queryInUserBooks(cloudyBook);
                        break;
                    case R.id.manual_dialog_radio_lccn:
                        cloudyBook.LCCN = editText.getText().toString();
                        queryInUserBooks(cloudyBook);
                }
            })
            .setNegativeButton("Cancel", (dialog, id) -> {
                mProgressBar.setIndeterminate(false);
                dialog.cancel();
            });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    public void onQueryTakePicture() {

        LogUtils.debug(TAG, "++onQueryTakePicture()");
        if (mCurrentImageFile != null && mCurrentImageFile.exists()) {
            Bundle params = new Bundle();
            params.putString("action", "deleted");
            params.putString("image_path", mCurrentImageFile.getAbsolutePath());
            if (mCurrentImageFile.delete()) {
                params.putBoolean("deleted_in_image_available", true);
                LogUtils.debug(TAG, "Removed processed image: %s", mCurrentImageFile.getName());
            } else {
                params.putBoolean("deleted_in_image_available", false);
                LogUtils.warn(TAG, "Unable to remove processed image: %s", mCurrentImageFile.getName());
            }

            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            replaceFragment(CameraFragment.newInstance());
        } else {
            showDismissableSnackbar(getString(R.string.err_no_camera_detected));
        }
    }

    @Override
    public void onResultListActionComplete(String message) {

        LogUtils.debug(TAG, "++onResultListActionComplete(%s)", message);
        if (!message.isEmpty()) {
            showDismissableSnackbar(message);
        }
    }

    @Override
    public void onResultListPopulated(int size) {

        LogUtils.debug(TAG, "++onResultListPopulated(%d)", size);
        if (size > 1) {
            setTitle(R.string.select_book);
        }
    }

    @Override
    public void onResultListItemSelected(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++onResultListItemSelected(%s)", cloudyBook.toString());
        replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBook));
    }

    @Override
    public void onScanResultsPopulated(int size) {

        LogUtils.debug(TAG, "++onScanResultsPopulated(%d)", size);
        mProgressBar.setIndeterminate(false);
    }

    @Override
    public void onScanResultsItemSelected(String searchText) {

        LogUtils.debug(TAG, "++onScanResultsItemSelected(%s)", searchText);
        CloudyBook cloudyBook = new CloudyBook();
        cloudyBook.Title = searchText;
        queryInUserBooks(cloudyBook);
    }

    /*
        Public Method(s)
     */
    public void retrieveBooksComplete(ArrayList<CloudyBook> cloudyBooks) {

        LogUtils.debug(TAG, "++retrieveBooksComplete(%d)", cloudyBooks.size());
        mProgressBar.setIndeterminate(false);
        if (cloudyBooks.size() == 0) {
            showDismissableSnackbar(getString(R.string.no_results));
        } else if (cloudyBooks.size() == 1) {
            setTitle(getString(R.string.fragment_cloudy_book_add));
            replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBooks.get(0)));
        } else {
            if (cloudyBooks.size() == BaseActivity.MAX_RESULTS) {
                showDismissableSnackbar(getString(R.string.max_results));
            }

            replaceFragment(ResultListFragment.newInstance(cloudyBooks));
        }
    }

    public void writeComplete(ArrayList<CloudyBook> cloudyBookList) {

        LogUtils.debug(TAG, "++writeComplete(%d)", cloudyBookList.size());
        mProgressBar.setIndeterminate(false);
        mCloudyBookList = cloudyBookList;
        replaceFragment(CloudyBookListFragment.newInstance(mCloudyBookList));
    }

    /*
        Private Method(s)
     */
    private void checkDevicePermission() {

        LogUtils.debug(TAG, "++checkDevicePermission()");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(
                    findViewById(R.id.main_drawer_layout),
                    getString(R.string.permission_denied_explanation),
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(
                        getString(R.string.ok),
                        view -> ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST))
                    .show();
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
            }
        } else {
            LogUtils.debug(TAG, "%s permission granted.", Manifest.permission.WRITE_EXTERNAL_STORAGE);
            readLocalLibrary();
        }
    }

    private void getUserPermissions() {

        LogUtils.debug(TAG, "++getUserPermissions()");
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id);
        FirebaseFirestore.getInstance().document(queryPath).get().addOnCompleteListener(this, task -> {

            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
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

            // regardless of result, get user's book library
            checkDevicePermission();
        });
    }

    private void queryGoogleBookService(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++queryGoogleBookService(%s)", cloudyBook.toString());
        if (cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) &&
            cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) &&
            cloudyBook.LCCN.equals(BaseActivity.DEFAULT_LCCN) &&
            cloudyBook.Title.isEmpty()) {
            String message = "Invalid search criteria.";
            LogUtils.warn(TAG, message);
            showDismissableSnackbar(message);
        } else {
            new RetrieveBookDataTask(this, cloudyBook).execute();
        }
    }

    private void queryInUserBooks(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++queryInUserBooks(%s)", cloudyBook.toString());
        CloudyBook foundBook = null;
        if (mCloudyBookList != null) {
            for (CloudyBook book : mCloudyBookList) {
                if (book.isPartiallyEqual(cloudyBook)) {
                    foundBook = book;
                    break;
                }
            }
        }

        if (foundBook != null) {
            mProgressBar.setIndeterminate(false);
            replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBook));
        } else {
            LogUtils.debug(TAG, "Did not find %s in user's book list.", cloudyBook.toString());
            queryGoogleBookService(cloudyBook);
        }
    }

    private void readLocalLibrary() {

        LogUtils.debug(TAG, "++readLocalLibrary()");
        String parsableString;
        String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
        File file = new File(getFilesDir(), resourcePath);
        LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
        mCloudyBookList = new ArrayList<>();
        try {
            if (file.exists() && file.canRead()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                while ((parsableString = bufferedReader.readLine()) != null) { //process line
                    if (parsableString.startsWith("--")) { // comment line; ignore
                        continue;
                    }

                    List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
                    if (elements.size() != BaseActivity.SCHEMA_FIELDS) {
                        LogUtils.debug(
                            TAG,
                            "Local library schema mismatch. Got: %d Expected: %d",
                            elements.size(),
                            BaseActivity.SCHEMA_FIELDS);
                        continue;
                    }

                    CloudyBook cloudyBook = new CloudyBook();
                    cloudyBook.VolumeId = elements.remove(0);
                    cloudyBook.ISBN_8 = elements.remove(0);
                    cloudyBook.ISBN_13 = elements.remove(0);
                    cloudyBook.LCCN = elements.remove(0);
                    cloudyBook.Title = elements.remove(0);
                    cloudyBook.Authors = new ArrayList<>(Arrays.asList(elements.remove(0).split(",")));
                    cloudyBook.Categories = new ArrayList<>(Arrays.asList(elements.remove(0).split(",")));
                    cloudyBook.AddedDate = Long.parseLong(elements.remove(0));
                    cloudyBook.HasRead = Boolean.parseBoolean(elements.remove(0));
                    cloudyBook.IsOwned = Boolean.parseBoolean(elements.remove(0));
                    cloudyBook.PublishedDate = elements.remove(0);
                    cloudyBook.Publisher = elements.remove(0);
                    cloudyBook.UpdatedDate = Long.parseLong(elements.remove(0));

                    // attempt to locate this book in existing list
                    boolean bookFound = false;
                    for (CloudyBook book : mCloudyBookList) {
                        if (book.VolumeId.equals(cloudyBook.VolumeId)) {
                            bookFound = true;
                            break;
                        }
                    }

                    if (!bookFound) {
                        mCloudyBookList.add(cloudyBook);
                        LogUtils.debug(TAG, "Adding %s to user book collection.", cloudyBook.toString());
                    }
                }
            } else {
                LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
            }
        } catch (Exception e) {
            LogUtils.warn(TAG, "Exception when reading local library data.");
            Crashlytics.logException(e);
            mProgressBar.setIndeterminate(false);
        } finally {
            if (mCloudyBookList == null || mCloudyBookList.size() == 0) {
                readServerLibrary(); // attempt to get user's book library from cloud
            } else {
                mCloudyBookList.sort(new SortUtils.ByBookName());
                mProgressBar.setIndeterminate(false);
                replaceFragment(CloudyBookListFragment.newInstance(mCloudyBookList));
            }
        }
    }

    private void readServerLibrary() {

        LogUtils.debug(TAG, "++readServerLibrary()");
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id, CloudyBook.ROOT);
        LogUtils.debug(TAG, "QueryPath: %s", queryPath);
        mCloudyBookList = new ArrayList<>();
        FirebaseFirestore.getInstance().collection(queryPath).get().addOnCompleteListener(this, task -> {

            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                    CloudyBook cloudyBook = document.toObject(CloudyBook.class);
                    if (cloudyBook != null) {
                        cloudyBook.VolumeId = document.getId();
                        mCloudyBookList.add(cloudyBook);
                    } else {
                        LogUtils.warn(TAG, "Unable to convert user book: %s", queryPath);
                    }
                }

                mCloudyBookList.sort(new SortUtils.ByBookName());
                new WriteToLocalLibraryTask(this, mCloudyBookList).execute();
            } else {
                LogUtils.debug(TAG, "Could not get user book list: %s", queryPath);
            }
        });
    }

    private void replaceFragment(Fragment fragment) {

        LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getSimpleName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment);
        fragmentTransaction.addToBackStack(fragment.getClass().getName());
        LogUtils.debug(TAG, "Back stack count: %d", fragmentManager.getBackStackEntryCount());
        fragmentTransaction.commit();
    }

    private void scanImageForISBN() {

        LogUtils.debug(TAG, "++scanImageForISBN()");
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
                    .setPositiveButton("OK", (dialog, id) -> useFirebaseBarcodeScanning())
                    .setNegativeButton("Cancel", (dialog, id) -> {
                        mProgressBar.setIndeterminate(false);
                        dialog.cancel();
                    });

                AlertDialog alert = alertDialogBuilder.create();
                alert.show();
            } else {
                useFirebaseBarcodeScanning();
            }
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            showDismissableSnackbar(message);
        }
    }

    private void scanImageForText() {

        LogUtils.debug(TAG, "++scanImageForText()");
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
                    .setPositiveButton("OK", (dialog, id) -> useFirebaseTextScanning())
                    .setNegativeButton("Cancel", (dialog, id) -> {
                        mProgressBar.setIndeterminate(false);
                        dialog.cancel();
                    });

                AlertDialog alert = alertDialogBuilder.create();
                alert.show();
            } else {
                useFirebaseTextScanning();
            }
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            showDismissableSnackbar(message);
        }
    }

    private void showDismissableSnackbar(String message) {

        mProgressBar.setIndeterminate(false);
        mSnackbar = Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            message,
            Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
        mSnackbar.show();
    }

    private void updateTitleAndDrawer(Fragment fragment) {

        LogUtils.debug(TAG, "++updateTitleAndDrawer(%s)", fragment.getClass().getName());
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(CloudyBookListFragment.class.getName())) {
            setTitle(getString(R.string.fragment_cloudy_book_list));
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

    private void useFirebaseBarcodeScanning() {

        LogUtils.debug(TAG, "++useFirebaseBarcodeScanning()");
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
                    CloudyBook cloudyBook = new CloudyBook();
                    for (FirebaseVisionBarcode barcode : task.getResult()) {
                        if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_ISBN) {
                            String barcodeValue = barcode.getDisplayValue();
                            LogUtils.debug(TAG, "Found a bar code: %s", barcodeValue);
                            if (barcodeValue != null && barcodeValue.length() == 8) {
                                cloudyBook.ISBN_8 = barcodeValue;
                            } else if (barcodeValue != null && barcodeValue.length() == 13) {
                                cloudyBook.ISBN_13 = barcodeValue;
                            }
                        } else {
                            LogUtils.warn(TAG, "Unexpected bar code: %s", barcode.getDisplayValue());
                        }
                    }

                    if ((!cloudyBook.ISBN_8.isEmpty() && !cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) ||
                        (!cloudyBook.ISBN_13.isEmpty() && !cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13))) {
                        queryInUserBooks(cloudyBook);
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
                        mRotationAttempts = 0;
                        scanImageForText();
                    }
                } else {
                    String message = "Bar code detection task failed.";
                    LogUtils.warn(TAG, message);
                    showDismissableSnackbar(message);
                    replaceFragment(QueryFragment.newInstance());
                }
            });
    }

    private void useFirebaseTextScanning() {

        LogUtils.debug(TAG, "++useFirebaseTextScanning()");
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
                    String message = "Did not find any bar codes/text in image.";
                    LogUtils.warn(TAG, message);
                    showDismissableSnackbar(message);
                    replaceFragment(QueryFragment.newInstance());
                }
            } else {
                String message = "Text detection task failed.";
                LogUtils.warn(TAG, message);
                showDismissableSnackbar(message);
                replaceFragment(QueryFragment.newInstance());
            }
        });
    }
}
