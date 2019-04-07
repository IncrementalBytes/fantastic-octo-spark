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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import java.util.Locale;

public class SignInActivity extends BaseActivity implements OnClickListener {

    private static final String TAG = BASE_TAG + SignInActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 4701;

    private ProgressBar mProgressBar;

    private FirebaseAuth mAuth;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogUtils.debug(TAG, "++onCreate(Bundle)");
        setContentView(R.layout.activity_sign_in);

        SignInButton signInWithGoogleButton = findViewById(R.id.sign_in_button_google);
        signInWithGoogleButton.setOnClickListener(this);

        mProgressBar = findViewById(R.id.sign_in_progress);
        mProgressBar.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestIdToken(getString(R.string.default_web_client_id))
            .requestIdToken("1079143607884-n6m9tirs482fdn65bf54lnvfrk4u8e54.apps.googleusercontent.com")
            .requestEmail()
            .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, connectionResult -> {
                LogUtils.debug(TAG, "++onConnectionFailed(ConnectionResult)");
                String message = String.format(Locale.ENGLISH, "Connection result was null: %s", connectionResult.getErrorMessage());
                LogUtils.debug(TAG, message);
                Snackbar.make(findViewById(R.id.activity_sign_in), message, Snackbar.LENGTH_LONG).show();
            })
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        LogUtils.debug(TAG, "++onStart()");
        if (mAuth.getCurrentUser() != null) {
            onAuthenticateSuccess(mAuth.getCurrentUser());
        }
    }

    @Override
    public void onClick(View view) {

        LogUtils.debug(TAG, "++onClick()");
        switch (view.getId()) {
            case R.id.sign_in_button_google:
                signInWithGoogle();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LogUtils.debug(TAG, "++onActivityResult(%1d, %2d, Intent)", requestCode, resultCode);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                if (account != null) {
                    firebaseAuthenticateWithGoogle(account);
                } else {
                    String message = String.format(
                        Locale.ENGLISH,
                        "Could not get sign-in account: %d - %s",
                        result.getStatus().getStatusCode(),
                        result.getStatus().getStatusMessage());
                    LogUtils.error(TAG, message);
                    Snackbar.make(findViewById(R.id.activity_sign_in), message, Snackbar.LENGTH_LONG).show();
                }
            } else {
                String message = String.format(
                    Locale.ENGLISH,
                    "Getting task result failed: %d - %s",
                    result.getStatus().getStatusCode(),
                    result.getStatus().getStatusMessage());
                LogUtils.error(TAG, message);
                Snackbar.make(findViewById(R.id.activity_sign_in), message, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthenticateWithGoogle(GoogleSignInAccount acct) {

        LogUtils.debug(TAG, "++firebaseAuthWithGoogle(%1s)", acct.getId());
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                    onAuthenticateSuccess(mAuth.getCurrentUser());
                } else {
                    Crashlytics.logException(task.getException());
                    String message = "Authenticating with Google account failed.";
                    LogUtils.error(TAG, message);
                    Snackbar.make(findViewById(R.id.activity_sign_in), message, Snackbar.LENGTH_LONG).show();
                }

                mProgressBar.setIndeterminate(false);
            });
    }

    private void signInWithGoogle() {

        LogUtils.debug(TAG, "++signInWithGoogle()");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void onAuthenticateSuccess(FirebaseUser user) {

        LogUtils.debug(TAG, "++onAuthenticateSuccess(%1s)", user.getDisplayName());
        Crashlytics.setUserIdentifier(user.getUid());
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.putExtra(BaseActivity.ARG_USER_ID, user.getUid());
        intent.putExtra(BaseActivity.ARG_USER_NAME, user.getDisplayName());
        intent.putExtra(BaseActivity.ARG_EMAIL, user.getEmail());
        startActivity(intent);
        finish();
    }
}
