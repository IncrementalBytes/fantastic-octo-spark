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
package net.whollynugatory.android.cloudycurator.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceFragmentCompat;
import java.util.Locale;

import net.whollynugatory.android.cloudycurator.BuildConfig;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.CloudyCuratorException;
import net.whollynugatory.android.cloudycurator.db.entity.UserEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import androidx.annotation.NonNull;

public class UserPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = BaseActivity.BASE_TAG + "UserPreferenceFragment";

  public static final String FORCE_EXCEPTION_PREFERENCE = "preference_force_exception";

  public interface OnPreferencesListener {

    void onPreferenceChanged() throws CloudyCuratorException;
  }

  private OnPreferencesListener mCallback;

  private UserEntity mUser;

  public static UserPreferenceFragment newInstance(UserEntity user) {

    Log.d(TAG, "++newInstance()");
    UserPreferenceFragment fragment = new UserPreferenceFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_USER, user);
    fragment.setArguments(args);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnPreferencesListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mUser = (UserEntity) arguments.getSerializable(BaseActivity.ARG_USER);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Log.d(TAG, "++onCreatePreferences(Bundle, String)");
    addPreferencesFromResource(R.xml.app_preferences);
    SwitchPreference switchPreference = findPreference(FORCE_EXCEPTION_PREFERENCE);
    if (switchPreference != null) {
      if (BuildConfig.DEBUG) {
        switchPreference.setVisible(true);
      } else {
        switchPreference.setVisible(false);
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    Log.d(TAG, "++onPause()");
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {

    Log.d(TAG, "++onSharedPreferenceChanged(SharedPreferences, String)");
    getPreferenceScreen().getSharedPreferences().edit().apply();
    try {
      mCallback.onPreferenceChanged();
    } catch (CloudyCuratorException e) {
      Log.d(TAG, "Exception!", e);
    }
  }
}
