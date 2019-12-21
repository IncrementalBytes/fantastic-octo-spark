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

package net.whollynugatory.android.cloudycurator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.preference.PreferenceManager;

import com.google.android.gms.common.images.Size;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import net.whollynugatory.android.cloudycurator.camera.CameraSizePair;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 *  Utility class to retrieve shared preferences.
 **/
public class PreferenceUtils {

  public static RectF getBarcodeReticleBox(GraphicOverlay overlay) {

    Context context = overlay.getContext();
    float overlayWidth = overlay.getWidth();
    float overlayHeight = overlay.getHeight();
    float boxWidth =
      overlayWidth * getIntPref(context, R.string.pref_key_barcode_reticle_width, 80) / 100;
    float boxHeight =
      overlayHeight * getIntPref(context, R.string.pref_key_barcode_reticle_height, 35) / 100;
    float cx = overlayWidth / 2;
    float cy = overlayHeight / 2;
    return new RectF(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2);
  }

  public static float getProgressToMeetBarcodeSizeRequirement(

    GraphicOverlay overlay, FirebaseVisionBarcode barcode) {
    Context context = overlay.getContext();
    if (getBooleanPref(context, R.string.pref_key_enable_barcode_size_check, false)) {
      float reticleBoxWidth = getBarcodeReticleBox(overlay).width();
      if (barcode != null && barcode.getBoundingBox() != null) {
        float barcodeWidth = overlay.translateX(barcode.getBoundingBox().width());
        float requiredWidth = reticleBoxWidth * getIntPref(context, R.string.pref_key_minimum_barcode_width, 50) / 100;
        return Math.min(barcodeWidth / requiredWidth, 1);
      } else {
        return 1;
      }
    } else {
      return 1;
    }
  }

  @Nullable
  public static CameraSizePair getUserSpecifiedPreviewSize(Context context) {

    try {
      String previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size);
      String pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size);
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
      return new CameraSizePair(
        Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
        Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)));
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isBypass(Context context) {

    return getBooleanPref(context, R.string.pref_key_bypass, false);
  }

  public static boolean isException(Context context) {

    return getBooleanPref(context, R.string.pref_key_exception, false);
  }

  public static void saveBooleanPreference(

    Context context, @StringRes int prefKeyId, boolean value) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(prefKeyId), value)
      .apply();
  }

  public static void saveStringPreference(

    Context context, @StringRes int prefKeyId, @Nullable String value) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putString(context.getString(prefKeyId), value)
      .apply();
  }

  public static boolean shouldDelayLoadingBarcodeResult(Context context) {

    return getBooleanPref(context, R.string.pref_key_delay_loading_barcode_result, true);
  }

  private static boolean getBooleanPref(

    Context context, @StringRes int prefKeyId, boolean defaultValue) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(prefKeyId);
    return sharedPreferences.getBoolean(prefKey, defaultValue);
  }

  private static int getIntPref(Context context, @StringRes int prefKeyId, int defaultValue) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(prefKeyId);
    return sharedPreferences.getInt(prefKey, defaultValue);
  }
}
