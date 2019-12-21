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

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.whollynugatory.android.cloudycurator.BuildConfig;
import net.whollynugatory.android.cloudycurator.PreferenceUtils;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.Utils;
import net.whollynugatory.android.cloudycurator.camera.CameraSizePair;
import net.whollynugatory.android.cloudycurator.camera.CameraSource;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

public class UserPreferenceFragment extends PreferenceFragmentCompat {

  private static final String TAG = BaseActivity.BASE_TAG + "UserPreferenceFragment";

  public static UserPreferenceFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new UserPreferenceFragment();
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    Log.d(TAG, "++onCreatePreferences(Bundle, String)");
    setPreferencesFromResource(R.xml.app_preferences, rootKey);
    setUpRearCameraPreviewSizePreference();
    if (BuildConfig.DEBUG) {
      setUpBypassPreference();
      setUpExceptionPreference();
    }
  }

  private void setUpBypassPreference() {

    Log.d(TAG, "++setUpBypassPreference()");
    SwitchPreference debugPreference = findPreference(getString(R.string.pref_key_bypass));
    if (debugPreference == null) {
      return;
    }

    debugPreference.setOnPreferenceChangeListener(
      (preference, newValue) -> {

        Log.d(TAG, "++setUpBypassPreference::onPreferenceChange()");
        PreferenceUtils.saveBooleanPreference(
          getActivity(),
          R.string.pref_key_bypass,
          (boolean) newValue);
        return true;
      });
  }

  private void setUpExceptionPreference() {

    Log.d(TAG, "++setUpExceptionPreference()");
    SwitchPreference debugPreference = findPreference(getString(R.string.pref_key_exception));
    if (debugPreference == null) {
      return;
    }

    debugPreference.setOnPreferenceChangeListener(
      (preference, newValue) -> {

        Log.d(TAG, "++setUpExceptionPreference::onPreferenceChange()");
        PreferenceUtils.saveBooleanPreference(
          getActivity(),
          R.string.pref_key_exception,
          (boolean) newValue);
        return true;
      });
  }

  private void setUpRearCameraPreviewSizePreference() {

    Log.d(TAG, "++setUpRearCameraPreviewSizePreference()");
    ListPreference previewSizePreference = findPreference(getString(R.string.pref_key_rear_camera_preview_size));
    if (previewSizePreference == null) {
      return;
    }

    Camera camera = null;
    try {
      camera = Camera.open(CameraSource.CAMERA_FACING_BACK);
      List<CameraSizePair> previewSizeList = Utils.generateValidPreviewSizeList(camera);
      String[] previewSizeStringValues = new String[previewSizeList.size()];
      Map<String, String> previewToPictureSizeStringMap = new HashMap<>();
      for (int i = 0; i < previewSizeList.size(); i++) {
        CameraSizePair sizePair = previewSizeList.get(i);
        previewSizeStringValues[i] = sizePair.preview.toString();
        if (sizePair.picture != null) {
          previewToPictureSizeStringMap.put(
            sizePair.preview.toString(), sizePair.picture.toString());
        }
      }

      previewSizePreference.setEntries(previewSizeStringValues);
      previewSizePreference.setEntryValues(previewSizeStringValues);
      previewSizePreference.setSummary(previewSizePreference.getEntry());
      previewSizePreference.setOnPreferenceChangeListener(
        (preference, newValue) -> {

          Log.d(TAG, "++setUpRearCameraPreviewSizePreference::onPreferenceChange()");
          String newPreviewSizeStringValue = (String) newValue;
          previewSizePreference.setSummary(newPreviewSizeStringValue);
          PreferenceUtils.saveStringPreference(
            getActivity(),
            R.string.pref_key_rear_camera_picture_size,
            previewToPictureSizeStringMap.get(newPreviewSizeStringValue));
          return true;
        });
    } catch (Exception e) { // If there's no camera for the given camera id, hide the corresponding preference.
      if (previewSizePreference.getParent() != null) {
        previewSizePreference.getParent().removePreference(previewSizePreference);
      }
    } finally {
      if (camera != null) {
        camera.release();
      }
    }
  }
}
