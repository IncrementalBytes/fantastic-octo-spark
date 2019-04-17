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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import net.frostedbytes.android.cloudycurator.fragments.CloudyBookFragment;
import net.frostedbytes.android.cloudycurator.fragments.CloudyBookListFragment;
import net.frostedbytes.android.cloudycurator.fragments.QueryFragment;
import net.frostedbytes.android.cloudycurator.fragments.ResultListFragment;
import net.frostedbytes.android.cloudycurator.fragments.ScanResultsFragment;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;
import net.frostedbytes.android.cloudycurator.utils.SortUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements
    CloudyBookFragment.OnCloudyBookListener,
    CloudyBookListFragment.OnCloudyBookListListener,
    NavigationView.OnNavigationItemSelectedListener,
    QueryFragment.OnQueryListener,
    ResultListFragment.OnResultListListener,
    ScanResultsFragment.OnScanResultsListener {

    private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

    static final int READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 12;
    static final int CAMERA_PERMISSIONS_REQUEST = 13;

    private QueryFragment mQueryFragment;

    private DrawerLayout mDrawerLayout;
    private ProgressBar mProgressBar;
    private Snackbar mSnackbar;

    private ArrayList<CloudyBook> mCloudyBookList;
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
        mUser.Id = getIntent().getStringExtra(BaseActivity.ARG_USER_ID);
        mUser.Email = getIntent().getStringExtra(BaseActivity.ARG_EMAIL);
        mUser.FullName = getIntent().getStringExtra(BaseActivity.ARG_USER_NAME);

        // update the navigation header
        NavigationView mNavigationView = findViewById(R.id.main_navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        View navigationHeaderView = mNavigationView.inflateHeaderView(R.layout.main_navigation_header);
        TextView navigationFullName = navigationHeaderView.findViewById(R.id.navigation_text_full_name);
        navigationFullName.setText(mUser.FullName);
        TextView navigationEmail = navigationHeaderView.findViewById(R.id.navigation_text_email);
        navigationEmail.setText(mUser.Email);
        TextView navigationVersion = navigationHeaderView.findViewById(R.id.navigation_text_version);
        navigationVersion.setText(BuildConfig.VERSION_NAME);

        // get user's book library
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mCloudyBookList = null;
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
                mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
                replaceFragment(mQueryFragment);
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
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtils.debug(TAG, "READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST permission granted.");
                    readLocalLibrary();
                } else {
                    LogUtils.debug(TAG, "READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST permission denied.");
                }

                break;
            case CAMERA_PERMISSIONS_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtils.debug(TAG, "CAMERA_PERMISSIONS_REQUEST permission granted.");
                    if (mQueryFragment != null) {
                        mQueryFragment.takePictureIntent();
                    }
                } else {
                    LogUtils.debug(TAG, "CAMERA_PERMISSIONS_REQUEST permission denied.");
                }

                break;
            default:
                LogUtils.debug(TAG, "Unknown request code: %d", requestCode);
                break;
        }
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onCloudyBookActionComplete(String message) {

        LogUtils.debug(TAG, "++onCloudyBookActionComplete(%s)", message);
        mSnackbar = Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            message,
            Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
        mSnackbar.show();
    }

    @Override
    public void onCloudyBookAddedToLibrary(CloudyBook cloudyBook) {

        if (cloudyBook == null) {
            LogUtils.debug(TAG, "++onUserBookAddedToLibrary(null)");
            mSnackbar = Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_add_cloudy_book),
                Snackbar.LENGTH_INDEFINITE);
            mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
            mSnackbar.show();
        } else {
            LogUtils.debug(TAG, "++onUserBookAddedToLibrary(%s)", cloudyBook.toString());
            String queryPath = PathUtils.combine(User.ROOT, mUser.Id, CloudyBook.ROOT, cloudyBook.VolumeId);
            FirebaseFirestore.getInstance().document(queryPath).set(cloudyBook, SetOptions.merge())
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        mCloudyBookList.add(cloudyBook);
                        new WriteToLocalLibraryTask(this, mCloudyBookList).execute();
                        replaceFragment(CloudyBookListFragment.newInstance(mCloudyBookList));
                    } else {
                        LogUtils.error(TAG, "Could not merge data under %s", queryPath);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }
                    }
                });
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
            mSnackbar = Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_remove_cloudy_book),
                Snackbar.LENGTH_INDEFINITE);
            mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
            mSnackbar.show();
        } else {
            LogUtils.debug(TAG, "++onCloudyBookRemoved(%s)", cloudyBook.toString());
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
            mSnackbar = Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_update_cloudy_book),
                Snackbar.LENGTH_INDEFINITE);
            mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
            mSnackbar.show();
        } else {
            LogUtils.debug(TAG, "++onCloudyBookUpdated(%s)", updatedCloudyBook.toString());
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
        mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
        replaceFragment(mQueryFragment);
    }

    @Override
    public void onCloudyBookListItemSelected(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++onCloudyBookListItemSelected(%s)", cloudyBook.toString());
        mProgressBar.setIndeterminate(false);
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
                    view -> {
                        mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
                        replaceFragment(mQueryFragment);
                    })
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
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                message,
                Snackbar.LENGTH_LONG)
                .show();
            mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
            replaceFragment(mQueryFragment);
        }
    }

    @Override
    public void onQueryFoundMultipleBooks(ArrayList<CloudyBook> cloudyBooks) {

        LogUtils.debug(TAG, "++onQueryFoundMultipleBooks(%d)", cloudyBooks.size());
        mProgressBar.setIndeterminate(false);
        if (cloudyBooks.size() == 0) {
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.no_results),
                Snackbar.LENGTH_LONG)
                .show();
            mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
            replaceFragment(mQueryFragment);
        } else if (cloudyBooks.size() == 1) {
            replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBooks.get(0)));
        } else {
            if (cloudyBooks.size() == BaseActivity.MAX_RESULTS) {
                mSnackbar = Snackbar.make(
                    findViewById(R.id.main_drawer_layout),
                    getString(R.string.max_results),
                    Snackbar.LENGTH_INDEFINITE);
                mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
                mSnackbar.show();
            }

            replaceFragment(ResultListFragment.newInstance(cloudyBooks));
        }
    }

    @Override
    public void onQueryFoundBook(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++onQueryFoundBook(%s)", cloudyBook.toString());
        mProgressBar.setIndeterminate(false);
        replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBook));
    }

    @Override
    public void onQueryStarted() {

        LogUtils.debug(TAG, "++onQueryStarted()");
        mProgressBar.setIndeterminate(true);
    }

    @Override
    public void onQueryTakePicture() {

        LogUtils.debug(TAG, "++onQueryTakePicture()");
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSIONS_REQUEST);
        } else {
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_no_camera_detected),
                Snackbar.LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void onQueryTextResultsFound(ArrayList<String> results) {

        LogUtils.debug(TAG, "++onQueryTextResultsFound(%d)", results.size());
        if (results.size() > 0) {
            replaceFragment(ScanResultsFragment.newInstance(results));
        } else {
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_no_text_detected),
                Snackbar.LENGTH_LONG)
                .show();
            mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
            replaceFragment(mQueryFragment);
        }
    }

    @Override
    public void onResultListActionComplete(String message) {

        LogUtils.debug(TAG, "++onResultListActionComplete(%s)", message);
        if (!message.isEmpty()) {
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                message,
                Snackbar.LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void onResultListPopulated(int size) {

        LogUtils.debug(TAG, "++onResultListPopulated(%d)", size);
        if (size > 1) {
            setTitle(R.string.select_a_book);
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
        mQueryFragment = QueryFragment.newInstance(mCloudyBookList);
        replaceFragment(mQueryFragment);
        mQueryFragment.queryInUserBooks(cloudyBook);
    }

    /*
        Private Method(s)
     */
    private void checkPermission(String permission, int permissionCode) {

        LogUtils.debug(TAG, "++checkPermission()");
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
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    permissionCode);
            }
        } else {
            LogUtils.debug(TAG, "%s permission granted.", permission);
            switch (permissionCode) {
                case READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST:
                    readLocalLibrary();
                    break;
                case CAMERA_PERMISSIONS_REQUEST:
                    if (mQueryFragment != null) {
                        mQueryFragment.takePictureIntent();
                    }

                    break;
            }
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

        LogUtils.debug(TAG, "++replaceFragment(%s)", fragment.getClass().getName());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment);
        fragmentTransaction.addToBackStack(fragment.getClass().getName());
        fragmentTransaction.commit();
    }

    private void updateTitleAndDrawer(Fragment fragment) {

        LogUtils.debug(TAG, "++updateTitleAndDrawer(%s)", fragment.getClass().getName());
        String fragmentClassName = fragment.getClass().getName();
        if (fragmentClassName.equals(CloudyBookFragment.class.getName())) {
            setTitle(getString(R.string.fragment_cloudy_book));
        } else if (fragmentClassName.equals(CloudyBookListFragment.class.getName())) {
            setTitle(getString(R.string.fragment_cloudy_book_list));
        } else if (fragmentClassName.equals(QueryFragment.class.getName())) {
            setTitle(getString(R.string.fragment_query));
        } else {
            setTitle(getString(R.string.app_name));
        }
    }

    private void writeComplete(ArrayList<CloudyBook> cloudyBookList) {

        LogUtils.debug(TAG, "++writeComplete(%d)", cloudyBookList.size());
        mProgressBar.setIndeterminate(false);
        mCloudyBookList = cloudyBookList;
        replaceFragment(CloudyBookListFragment.newInstance(mCloudyBookList));
    }

    static class WriteToLocalLibraryTask extends AsyncTask<Void, Void, ArrayList<CloudyBook>> {

        private WeakReference<MainActivity> mFragmentWeakReference;
        private ArrayList<CloudyBook> mCloudyBooks;

        WriteToLocalLibraryTask(MainActivity context, ArrayList<CloudyBook> cloudyBookList) {

            mFragmentWeakReference = new WeakReference<>(context);
            mCloudyBooks = cloudyBookList;
        }

        protected ArrayList<CloudyBook> doInBackground(Void... params) {

            ArrayList<CloudyBook> booksWritten = new ArrayList<>();
            FileOutputStream outputStream;
            try {
                outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
                    BaseActivity.DEFAULT_LIBRARY_FILE,
                    Context.MODE_PRIVATE);
                for (CloudyBook cloudyBook : mCloudyBooks) {
                    String lineContents = String.format(
                        Locale.US,
                        "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\r\n",
                        cloudyBook.VolumeId,
                        cloudyBook.ISBN_8,
                        cloudyBook.ISBN_13,
                        cloudyBook.LCCN,
                        cloudyBook.Title,
                        cloudyBook.getAuthorsDelimited(),
                        cloudyBook.getCategoriesDelimited(),
                        String.valueOf(cloudyBook.AddedDate),
                        String.valueOf(cloudyBook.HasRead),
                        String.valueOf(cloudyBook.IsOwned),
                        cloudyBook.PublishedDate,
                        cloudyBook.Publisher,
                        cloudyBook.UpdatedDate);
                    outputStream.write(lineContents.getBytes());
                    booksWritten.add(cloudyBook);
                }
            } catch (Exception e) {
                LogUtils.warn(TAG, "Exception when writing local library.");
                Crashlytics.logException(e);
            }

            return booksWritten;
        }

        protected void onPostExecute(ArrayList<CloudyBook> cloudyBookList) {

            LogUtils.debug(TAG, "++onPostExecute(%d)", cloudyBookList.size());
            MainActivity activity = mFragmentWeakReference.get();
            if (activity == null) {
                LogUtils.error(TAG, "Activity is null.");
                return;
            }

            activity.writeComplete(cloudyBookList);
        }
    }
}
