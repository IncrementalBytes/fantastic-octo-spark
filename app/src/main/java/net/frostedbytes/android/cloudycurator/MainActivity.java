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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import net.frostedbytes.android.cloudycurator.fragments.BookFragment;
import net.frostedbytes.android.cloudycurator.fragments.MainListFragment;
import net.frostedbytes.android.cloudycurator.fragments.QueryBookFragment;
import net.frostedbytes.android.cloudycurator.models.Book;
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;
import net.frostedbytes.android.cloudycurator.utils.SortUtils;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends BaseActivity implements
    BookFragment.OnBookListListener,
    MainListFragment.OnMainListListener,
    NavigationView.OnNavigationItemSelectedListener,
    QueryBookFragment.OnQueryBookListener {

    private static final String TAG = BASE_TAG + MainActivity.class.getSimpleName();

    static final int READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 12;
    static final int INTERNET_PERMISSIONS_REQUEST = 13;

    private FloatingActionButton mAddButton;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ProgressBar mProgressBar;

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
        mNavigationView = findViewById(R.id.main_navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        View navigationHeaderView = mNavigationView.inflateHeaderView(R.layout.main_navigation_header);
        TextView navigationFullName = navigationHeaderView.findViewById(R.id.navigation_text_full_name);
        navigationFullName.setText(mUser.FullName);
        TextView navigationEmail = navigationHeaderView.findViewById(R.id.navigation_text_email);
        navigationEmail.setText(mUser.Email);
        TextView navigationVersion = navigationHeaderView.findViewById(R.id.navigation_text_version);
        navigationVersion.setText(BuildConfig.VERSION_NAME);

        getUserBookList();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        LogUtils.debug(TAG, "++onNavigationItemSelected(%s)", item.getTitle());
        switch (item.getItemId()) {
            case R.id.navigation_menu_home:
                replaceFragment(MainListFragment.newInstance(mUserBookList));
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
                        replaceFragment(QueryBookFragment.newInstance(mUserBookList));
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
    public void onBookAddedToLibrary(UserBook userBook) {

        LogUtils.debug(TAG, "++onBookAddedToLibrary(%s)", userBook.toString());
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id, Book.ROOT, userBook.ISBN);
        FirebaseFirestore.getInstance().document(queryPath).set(userBook, SetOptions.merge())
            .addOnFailureListener(e -> LogUtils.error(TAG, "Could not merge data under %s", queryPath));
        getUserBookList();
    }

    public void onBookLibraryFail() {

        LogUtils.debug(TAG, "++onBookLibraryFail()");
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.err_add_book_fail),
            Snackbar.LENGTH_LONG)
            .show();
        replaceFragment(MainListFragment.newInstance(mUserBookList));
    }

    public void onBookInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onBookInit(%s)", String.valueOf(isSuccessful));
        mProgressBar.setIndeterminate(false);
    }

    public void onMainListItemSelected(UserBook userBook) {

        LogUtils.debug(TAG, "++onMainListItemSelected(UserBook)");
        mAddButton.setEnabled(false);
        replaceFragment(BookFragment.newInstance(mUser.Id, userBook));
    }

    public void onMainListPopulated(int size) {

        LogUtils.debug(TAG, "++onMainListPopulated(%d)", size);
        mProgressBar.setIndeterminate(false);
        if (size == 0) {
            Snackbar.make(
                findViewById(R.id.main_drawer_layout),
                getString(R.string.err_no_data),
                Snackbar.LENGTH_LONG)
                .setAction(
                    getString(R.string.add),
                    view -> replaceFragment(QueryBookFragment.newInstance(mUserBookList)))
                .show();
        }
    }

    @Override
    public void onQueryBookInit(boolean isSuccessful) {

        LogUtils.debug(TAG, "++onQueryBookInit(%s)", String.valueOf(isSuccessful));
        if (isSuccessful) {
            mAddButton.setEnabled(false);
        }
    }

    @Override
    public void onQueryBookFailure() {

        LogUtils.debug(TAG, "++onQueryBookFailure()");
        Snackbar.make(
            findViewById(R.id.main_drawer_layout),
            getString(R.string.err_book_search_fail),
            Snackbar.LENGTH_LONG)
            .show();
        replaceFragment(MainListFragment.newInstance(mUserBookList));
    }

    @Override
    public void onQueryBookFound(UserBook userBook) {

        LogUtils.debug(TAG, "++onQueryBookFound(%s)", userBook.toString());
        replaceFragment(BookFragment.newInstance(mUser.Id, userBook));
    }

    /*
        Private Method(s)
     */
    private void getUserBookList() {

        LogUtils.debug(TAG, "++getUserBookList()");
        String queryPath = PathUtils.combine(User.ROOT, mUser.Id, Book.ROOT);
        FirebaseFirestore.getInstance().collection(queryPath).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {

                mUserBookList = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    UserBook userBook = document.toObject(UserBook.class);
                    userBook.ISBN = document.getId();
                    mUserBookList.add(userBook);
                }

                mUserBookList.sort(new SortUtils.ByBookName());
                mProgressBar.setIndeterminate(false);
                mAddButton.setOnClickListener(pickView -> replaceFragment(QueryBookFragment.newInstance(mUserBookList)));
                replaceFragment(MainListFragment.newInstance(mUserBookList));
        }).addOnFailureListener(e -> LogUtils.debug(TAG, "Could not get user book list: %s", e.getMessage()));
    }

    private void replaceFragment(Fragment fragment) {

        LogUtils.debug(TAG, "++replaceFragment()");
        updateTitleAndDrawer(fragment);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    private void updateTitleAndDrawer(Fragment fragment) {

        LogUtils.debug(TAG, "++updateTitleAndDrawer(Fragment)");
        String fragmentClassName = fragment.getClass().getName();
        mAddButton.hide();
        if (fragmentClassName.equals(MainListFragment.class.getName())) {
            mAddButton.show();
            setTitle(getString(R.string.title_main));
        } else if (fragmentClassName.equals(BookFragment.class.getName())) {
            setTitle(getString(R.string.title_book));
        } else if (fragmentClassName.equals(QueryBookFragment.class.getName())) {
            setTitle(getString(R.string.title_query));
        } else {
            mAddButton.show();
            setTitle(getString(R.string.app_name));
        }
    }
}
