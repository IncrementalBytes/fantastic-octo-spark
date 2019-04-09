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
import android.support.design.widget.FloatingActionButton;
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
import net.frostedbytes.android.cloudycurator.fragments.UserBookFragment;
import net.frostedbytes.android.cloudycurator.fragments.UserBookListFragment;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.models.UserBook;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements
    CloudyBookFragment.OnCloudyBookListener,
    CloudyBookListFragment.OnCloudyBookListListener,
    NavigationView.OnNavigationItemSelectedListener,
    QueryFragment.OnQueryListener,
    UserBookFragment.OnUserBookListListener,
    UserBookListFragment.OnUserBookListListener {

    private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

    static final int READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 12;
    static final int INTERNET_PERMISSIONS_REQUEST = 13;

    private FloatingActionButton mAddButton;
    private DrawerLayout mDrawerLayout;
    private ProgressBar mProgressBar;
    private FloatingActionButton mSyncButton;

    private ArrayList<UserBook> mUserBookList;
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
        LogUtils.debug(TAG, "Run Milliseconds: %d", Calendar.getInstance().getTimeInMillis());
        setContentView(R.layout.activity_main);

        mAddButton = findViewById(R.id.main_fab_add);
        mDrawerLayout = findViewById(R.id.main_drawer_layout);
        mProgressBar = findViewById(R.id.main_progress);
        mProgressBar.setIndeterminate(true);
        mSyncButton = findViewById(R.id.main_fab_sync);

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
        readLocalLibrary();
        if (mUserBookList == null || mUserBookList.size() == 0) {
            readServerLibrary(); // attempt to get user's book library from cloud
        } else {
            mUserBookList.sort(new SortUtils.ByBookName());
            mProgressBar.setIndeterminate(false);
            mAddButton.show();
            mAddButton.setOnClickListener(pickView -> replaceFragment(QueryFragment.newInstance(mUserBookList)));
            mSyncButton.show();
            mSyncButton.setOnClickListener(pickView -> synchronizeLibrary());
            replaceFragment(UserBookListFragment.newInstance(mUserBookList));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        LogUtils.debug(TAG, "++onNavigationItemSelected(%s)", item.getTitle());
        switch (item.getItemId()) {
            case R.id.navigation_menu_home:
                replaceFragment(UserBookListFragment.newInstance(mUserBookList));
                break;
            case R.id.navigation_menu_add:
                mProgressBar.setIndeterminate(true);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Snackbar.make(
                            findViewById(R.id.main_drawer_layout),
                            getString(R.string.permission_denied_explanation),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(
                                getString(R.string.ok),
                                view -> ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST))
                            .show();
                    } else {
                        ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
                    }
                } else {
                    LogUtils.debug(TAG, "%s permission granted.", Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)) {
                            Snackbar.make(
                                findViewById(R.id.main_drawer_layout),
                                getString(R.string.permission_denied_explanation),
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(
                                    getString(R.string.ok),
                                    view -> ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[]{Manifest.permission.INSTALL_LOCATION_PROVIDER},
                                        INTERNET_PERMISSIONS_REQUEST))
                                .show();
                        } else {
                            ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.INTERNET},
                                INTERNET_PERMISSIONS_REQUEST);
                        }
                    } else {
                        LogUtils.debug(TAG, "%s permission granted.", Manifest.permission.INTERNET);
                        replaceFragment(QueryFragment.newInstance(mUserBookList));
                    }
                }

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
                } else {
                    LogUtils.debug(TAG, "READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST permission denied.");
                }

                break;
            case INTERNET_PERMISSIONS_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtils.debug(TAG, "INTERNET_PERMISSIONS_REQUEST permission granted.");
                } else {
                    LogUtils.debug(TAG, "INTERNET_PERMISSIONS_REQUEST permission denied.");
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
    public void onCloudyBookInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onCloudyBookInit(%s)", String.valueOf(isSuccessful));
        mProgressBar.setIndeterminate(false);
    }

    @Override
    public void onCloudyBookListItemSelected(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++onCloudyBookListItemSelected(%s)", cloudyBook.toString());
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        String queryPath = PathUtils.combine(CloudyBook.ROOT, cloudyBook.ISBN);
        FirebaseFirestore.getInstance().document(queryPath).set(cloudyBook, SetOptions.merge())
            .addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    LogUtils.debug(TAG, "Successfully added: %s", cloudyBook.toString());
                    replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudyBook));
                } else {
                    LogUtils.warn(TAG, "Failed to added: %s", cloudyBook.toString());
                    if (task.getException() != null) {
                        Crashlytics.logException(task.getException());
                    }
                    // TODO: add empty object in cloud for manual import?
                }
            });
    }

    @Override
    public void onCloudyBookListPopulated(int size) {

        LogUtils.debug(TAG, "++onCloudyBookListPopulated(%d)", size);
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        if (size > 0) {
            setTitle(R.string.select_a_book);
        }
    }

    @Override
    public void onQueryCancelled() {

        LogUtils.debug(TAG, "++onQueryCancelled()");
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
    }

    @Override
    public void onQueryFailure() {

        LogUtils.debug(TAG, "++onQueryFailure()");
        mProgressBar.setIndeterminate(false);
        mAddButton.show();
        mSyncButton.show();
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.err_book_search_fail),
            Snackbar.LENGTH_LONG)
            .show();
        replaceFragment(UserBookListFragment.newInstance(mUserBookList));
    }

    @Override
    public void onQueryFoundBook(CloudyBook cloudBook) {

        LogUtils.debug(TAG, "++onQueryFoundBook(%s)", cloudBook.toString());
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        replaceFragment(CloudyBookFragment.newInstance(mUser.Id, cloudBook));
    }

    @Override
    public void onQueryFoundMultipleBooks(ArrayList<CloudyBook> cloudyBooks) {

        LogUtils.debug(TAG, "++onQueryFoundMultipleBooks(%d)", cloudyBooks.size());
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        replaceFragment(CloudyBookListFragment.newInstance(cloudyBooks));
    }

    @Override
    public void onQueryFoundUserBook(UserBook userBook) {

        LogUtils.debug(TAG, "++onQueryFoundUserBook(%s)", userBook.toString());
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        replaceFragment(UserBookFragment.newInstance(mUser.Id, userBook));
    }

    @Override
    public void onQueryInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onQueryInit(%s)", String.valueOf(isSuccessful));
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
    }

    @Override
    public void onQueryNoBarcode() {

        LogUtils.debug(TAG, "++onQueryNoBarcode()");
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.no_bar_codes),
            Snackbar.LENGTH_LONG)
            .show();
        replaceFragment(QueryFragment.newInstance(mUserBookList));
    }

    @Override
    public void onQueryNoResultsFound() {

        LogUtils.debug(TAG, "++onQueryNoResultsFound()");
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.no_results),
            Snackbar.LENGTH_LONG)
            .show();
        replaceFragment(QueryFragment.newInstance(mUserBookList));
    }

    @Override
    public void onQueryStarted() {

        LogUtils.debug(TAG, "++onQueryStarted()");
        mProgressBar.setIndeterminate(true);
        mAddButton.hide();
        mSyncButton.hide();
    }

    @Override
    public void onUserBookAddedToLibrary(UserBook userBook) {

        LogUtils.debug(TAG, "++onUserBookAddedToLibrary(%s)", userBook.toString());
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id, UserBook.ROOT, userBook.ISBN);
        FirebaseFirestore.getInstance().document(queryPath).set(userBook, SetOptions.merge())
            .addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    mUserBookList.add(userBook);
                    new WriteToLocalLibraryTask(this, mUserBookList).execute();
                } else {
                    LogUtils.error(TAG, "Could not merge data under %s", queryPath);
                    if (task.getException() != null) {
                        Crashlytics.logException(task.getException());
                    }
                }
            });
    }

    @Override
    public void onUserBookAddedToLibraryFail() {

        LogUtils.debug(TAG, "++onUserBookAddedToLibraryFail()");
        mProgressBar.setIndeterminate(false);
        mAddButton.show();
        mSyncButton.show();
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.err_add_book_fail),
            Snackbar.LENGTH_LONG)
            .show();
        replaceFragment(UserBookListFragment.newInstance(mUserBookList));
    }

    @Override
    public void onUserBookInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onUserBookInit(%s)", String.valueOf(isSuccessful));
        mProgressBar.setIndeterminate(false);
        mAddButton.hide();
        mSyncButton.hide();
    }

    @Override
    public void onUserBookFail() {

        LogUtils.debug(TAG, "++onUserBookFail()");
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.err_add_book_fail),
            Snackbar.LENGTH_LONG)
            .show();
    }

    @Override
    public void onUserBookUpdated(UserBook updatedUserBook) {

        LogUtils.debug(TAG, "++onUserBookUpdated(%s)", updatedUserBook.toString());
        ArrayList<UserBook> updatedUserBookList = new ArrayList<>();
        for (UserBook userBook : mUserBookList) {
            if (userBook.equals(updatedUserBook)) {
                updatedUserBookList.add(updatedUserBook);
            } else {
                updatedUserBookList.add(userBook);
            }
        }

        new WriteToLocalLibraryTask(this, updatedUserBookList).execute();
        replaceFragment(UserBookListFragment.newInstance(updatedUserBookList));
    }

    @Override
    public void onUserBookListItemSelected(UserBook userBook) {

        LogUtils.debug(TAG, "++onUserBookListItemSelected(%s)", userBook.toString());
        mAddButton.hide();
        mSyncButton.hide();
        replaceFragment(UserBookFragment.newInstance(mUser.Id, userBook));
    }

    @Override
    public void onUserBookListPopulated(int size) {

        LogUtils.debug(TAG, "++onMainListPopulated(%d)", size);
        mProgressBar.setIndeterminate(false);
        mAddButton.show();
        mSyncButton.show();
        if (size == 0) {
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_no_data),
                Snackbar.LENGTH_LONG)
                .setAction(
                    getString(R.string.add),
                    view -> replaceFragment(QueryFragment.newInstance(mUserBookList)))
                .show();
        }
    }

    /*
        Private Method(s)
     */
    private void readLocalLibrary() {

        LogUtils.debug(TAG, "++readLocalLibrary()");
        String parsableString;
        String resourcePath = BaseActivity.DEFAULT_LIBRARY_FILE;
        File file = new File(getFilesDir(), resourcePath);
        LogUtils.debug(TAG, "Loading %s", file.getAbsolutePath());
        mUserBookList = new ArrayList<>();
        try {
            if (file.exists() && file.canWrite()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                while ((parsableString = bufferedReader.readLine()) != null) { //process line
                    if (parsableString.startsWith("--")) { // comment line; ignore
                        continue;
                    }

                    // [ISBN]|[Title]|[Author(s)]|[AddedDate]|[HasRead]|[IsOwned]
                    List<String> elements = new ArrayList<>(Arrays.asList(parsableString.split("\\|")));
                    UserBook currentUserBook = new UserBook();
                    currentUserBook.ISBN = elements.remove(0);
                    currentUserBook.Title = elements.remove(0);
                    currentUserBook.Authors = new ArrayList<>(Arrays.asList(elements.remove(0).split(",")));
                    currentUserBook.AddedDate = Long.parseLong(elements.remove(0));
                    currentUserBook.HasRead = Boolean.parseBoolean(elements.remove(0));
                    currentUserBook.IsOwned = Boolean.parseBoolean(elements.remove(0));

                    // attempt to locate this book in existing list
                    boolean bookFound = false;
                    for (UserBook userBook : mUserBookList) {
                        if (userBook.equals(currentUserBook)) {
                            bookFound = true;
                            break;
                        }
                    }

                    if (!bookFound) {
                        mUserBookList.add(currentUserBook);
                        LogUtils.debug(TAG, "Adding %s to user book collection.", currentUserBook.toString());
                    }
                }
            } else {
                LogUtils.debug(TAG, "%s does not exist yet.", resourcePath);
            }
        } catch (Exception e) {
            LogUtils.warn(TAG, "Exception when reading local library data.");
            Crashlytics.logException(e);
        }
    }

    private void readServerLibrary() {

        LogUtils.debug(TAG, "++readServerLibrary()");
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id, UserBook.ROOT);
        LogUtils.debug(TAG, "QueryPath: %s", queryPath);
        mUserBookList = new ArrayList<>();
        FirebaseFirestore.getInstance().collection(queryPath).get()
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot document : task.getResult().getDocuments()) {
                        UserBook userBook = document.toObject(UserBook.class);
                        if (userBook != null) {
                            userBook.ISBN = document.getId();
                            mUserBookList.add(userBook);
                        } else {
                            LogUtils.warn(TAG, "Unable to convert user book: %s", queryPath);
                        }
                    }

                    mUserBookList.sort(new SortUtils.ByBookName());
                    new WriteToLocalLibraryTask(this, mUserBookList).execute();
                    mProgressBar.setIndeterminate(false);
                    mAddButton.show();
                    mAddButton.setOnClickListener(pickView -> replaceFragment(QueryFragment.newInstance(mUserBookList)));
                    mSyncButton.show();
                    mSyncButton.setOnClickListener(pickView -> synchronizeLibrary());
                    replaceFragment(UserBookListFragment.newInstance(mUserBookList));
                } else {
                    LogUtils.debug(TAG, "Could not get user book list: %s", queryPath);
                }
            });
    }

    private void replaceFragment(Fragment fragment) {

        LogUtils.debug(TAG, "++replaceFragment()");
        updateTitleAndDrawer(fragment);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void synchronizeLibrary() {

        LogUtils.debug(TAG, "++synchronizeLibrary()");
        readServerLibrary();
    }

    private void updateTitleAndDrawer(Fragment fragment) {

        LogUtils.debug(TAG, "++updateTitleAndDrawer(Fragment)");
        String fragmentClassName = fragment.getClass().getName();
        mAddButton.hide();
        mSyncButton.hide();
        if (fragmentClassName.equals(CloudyBookFragment.class.getName())) {
            setTitle(getString(R.string.fragment_cloudybook));
        } else if (fragmentClassName.equals(CloudyBookListFragment.class.getName())) {
            setTitle(getString(R.string.fragment_cloudybooklist));
        } else if (fragmentClassName.equals(QueryFragment.class.getName())) {
            setTitle(getString(R.string.fragment_query));
        } else if (fragmentClassName.equals(UserBookFragment.class.getName())) {
            setTitle(getString(R.string.fragment_userbook));
        } else if (fragmentClassName.equals(UserBookListFragment.class.getName())) {
            mAddButton.show();
            mSyncButton.show();
            setTitle(getString(R.string.fragment_userbooklist));
        } else {
            mAddButton.show();
            mSyncButton.show();
            setTitle(getString(R.string.app_name));
        }
    }

    private void writeComplete(ArrayList<UserBook> userBookList) {

        LogUtils.debug(TAG, "++writeComplete(%d)", userBookList.size());
        mUserBookList = userBookList;
    }

    static class WriteToLocalLibraryTask extends AsyncTask<Void, Void, ArrayList<UserBook>> {

        private WeakReference<MainActivity> mFragmentWeakReference;
        private ArrayList<UserBook> mUserBooks;

        WriteToLocalLibraryTask(MainActivity context, ArrayList<UserBook> userBookList) {

            mFragmentWeakReference = new WeakReference<>(context);
            mUserBooks = userBookList;
        }

        protected ArrayList<UserBook> doInBackground(Void... params) {

            ArrayList<UserBook> booksWritten = new ArrayList<>();
            FileOutputStream outputStream;
            try {
                outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
                    "localLibrary.txt",
                    Context.MODE_PRIVATE);
                for (UserBook userBook : mUserBooks) {
                    String lineContents = String.format(
                        Locale.ENGLISH,
                        "%s|%s|%s|%s|%s|%s\r\n",
                        userBook.ISBN,
                        userBook.Title,
                        userBook.getAuthorsDelimited(),
                        String.valueOf(userBook.AddedDate),
                        String.valueOf(userBook.HasRead),
                        String.valueOf(userBook.IsOwned));
                    outputStream.write(lineContents.getBytes());
                    booksWritten.add(userBook);
                }
            } catch (Exception e) {
                LogUtils.warn(TAG, "Exception when writing local library.");
                Crashlytics.logException(e);
            }

            return booksWritten;
        }

        protected void onPostExecute(ArrayList<UserBook> userBookList) {

            LogUtils.debug(TAG, "++onPostExecute(%d)", userBookList.size());
            MainActivity activity = mFragmentWeakReference.get();
            if (activity == null) {
                LogUtils.error(TAG, "Activity is null.");
                return;
            }

            activity.writeComplete(userBookList);
        }
    }
}
