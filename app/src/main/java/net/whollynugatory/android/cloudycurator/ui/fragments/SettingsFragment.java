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

import net.whollynugatory.android.cloudycurator.PreferenceUtils;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.Utils;
import net.whollynugatory.android.cloudycurator.camera.CameraSizePair;
import net.whollynugatory.android.cloudycurator.camera.CameraSource;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

/**
 *  Configures App settings.
 **/
public class SettingsFragment extends PreferenceFragmentCompat {

  private static final String TAG = BaseActivity.BASE_TAG + "SettingsFragment";

  @Override
  public void onCreatePreferences(Bundle bundle, String rootKey) {

    Log.d(TAG, "++onCreatePreferences(Bundle, String)");
    setPreferencesFromResource(R.xml.app_preferences, rootKey);
    setUpRearCameraPreviewSizePreference();
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

          Log.d(TAG, "++onPreferenceChange()");
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
