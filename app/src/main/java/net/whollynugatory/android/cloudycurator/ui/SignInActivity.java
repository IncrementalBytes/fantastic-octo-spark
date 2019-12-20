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

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import net.whollynugatory.android.cloudycurator.R;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

public class SignInActivity extends AppCompatActivity implements OnClickListener {

  private static final String TAG = BaseActivity.BASE_TAG + SignInActivity.class.getSimpleName();

  private static final int RC_SIGN_IN = 4701;

  private FirebaseAnalytics mFirebaseAnalytics;

  private ProgressBar mProgressBar;
  private Snackbar mSnackbar;

  private GoogleSignInAccount mAccount;
  private FirebaseAuth mAuth;
  private GoogleApiClient mGoogleApiClient;

  /*
      Activity Override(s)
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    setContentView(R.layout.activity_sign_in);

    SignInButton signInWithGoogleButton = findViewById(R.id.sign_in_button_google);
    signInWithGoogleButton.setOnClickListener(this);

    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    mProgressBar = findViewById(R.id.sign_in_progress);
    mProgressBar.setVisibility(View.INVISIBLE);

    mAuth = FirebaseAuth.getInstance();
    mAccount = GoogleSignIn.getLastSignedInAccount(this);

    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestIdToken(getString(R.string.default_web_client_id))
      .requestEmail()
      .build();

    mGoogleApiClient = new GoogleApiClient.Builder(this)
      .enableAutoManage(this, connectionResult -> {
        Log.d(TAG, "++onConnectionFailed(ConnectionResult)");
        String message = String.format(Locale.US, "Connection result was null: %s", connectionResult.getErrorMessage());
        showErrorInSnackBar(message);
      })
      .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
      .build();
  }

  @Override
  public void onStart() {
    super.onStart();

    Log.d(TAG, "++onStart()");
    if (mAuth.getCurrentUser() != null && mAccount != null) {
      onAuthenticateSuccess();
    }
  }

  /*
      View Override(s)
   */
  @Override
  public void onClick(View view) {

    Log.d(TAG, "++onClick()");
    if (view.getId() == R.id.sign_in_button_google) {
      signInWithGoogle();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d(TAG, "++onActivityResult(int, int, Intent)");
    if (requestCode == RC_SIGN_IN) {
      GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      if (result.isSuccess()) {
        mAccount = result.getSignInAccount();
        if (mAccount  != null) {
          firebaseAuthenticateWithGoogle(mAccount );
        } else {
          String message = String.format(
            Locale.US,
            "Could not get sign-in account: %d - %s",
            result.getStatus().getStatusCode(),
            result.getStatus().getStatusMessage());
          showErrorInSnackBar(message);
        }
      } else {
        String message = String.format(
          Locale.US,
          "Getting task result failed: %d - %s",
          result.getStatus().getStatusCode(),
          result.getStatus().getStatusMessage());
        showErrorInSnackBar(message);
      }
    }
  }

  /*
      Private Method(s)
   */
  private void onAuthenticateSuccess() {

    if (mAuth.getCurrentUser() != null && mAccount != null) {
      Log.d(TAG, "++onAuthenticateSuccess()");
      Crashlytics.setUserIdentifier(mAuth.getCurrentUser().getUid());
      Bundle bundle = new Bundle();
      bundle.putString(FirebaseAnalytics.Param.METHOD, "onAuthenticateSuccess");
      mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

      Intent intent = new Intent(SignInActivity.this, MainActivity.class);
      intent.putExtra(BaseActivity.ARG_FIREBASE_USER_ID, mAuth.getCurrentUser().getUid());
      intent.putExtra(BaseActivity.ARG_USER_NAME, mAuth.getCurrentUser().getDisplayName());
      intent.putExtra(BaseActivity.ARG_EMAIL, mAuth.getCurrentUser().getEmail());
      startActivity(intent);
      finish();
    } else {
      String message = "Authentication did not return expected account information; please try again.";
      showErrorInSnackBar(message);
    }
  }

  private void firebaseAuthenticateWithGoogle(GoogleSignInAccount acct) {

    Log.d(TAG, "++firebaseAuthWithGoogle(GoogleSignInAccount)");
    mProgressBar.setVisibility(View.VISIBLE);
    mProgressBar.setIndeterminate(true);
    AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
    mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {

      if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "firebaseAuthenticateWithGoogle");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
        onAuthenticateSuccess();
      } else {
        Crashlytics.logException(task.getException());
        String message = "Authenticating with Google account failed.";
        showErrorInSnackBar(message);
      }

      mProgressBar.setIndeterminate(false);
    });
  }

  private void showErrorInSnackBar(String message) {

    Log.e(TAG, message);
    mSnackbar = Snackbar.make(
      findViewById(R.id.activity_sign_in),
      message,
      Snackbar.LENGTH_INDEFINITE);
    mSnackbar.setAction(R.string.dismiss, v -> mSnackbar.dismiss());
    mSnackbar.show();
  }

  private void signInWithGoogle() {

    Log.d(TAG, "++signInWithGoogle()");
    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
    startActivityForResult(signInIntent, RC_SIGN_IN);
  }
}
