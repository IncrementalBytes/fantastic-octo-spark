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
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

import net.whollynugatory.android.cloudycurator.camera.CameraSizePair;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class Utils {

  private static final String TAG = BaseActivity.BASE_TAG + "Utils";

  public static final float ASPECT_RATIO_TOLERANCE = 0.01f;

  /**
   * Generates a list of acceptable preview sizes. Preview sizes are not acceptable if there is not
   * a corresponding picture size of the same aspect ratio. If there is a corresponding picture size
   * of the same aspect ratio, the picture size is paired up with the preview size.
   *
   * <p>This is necessary because even if we don't use still pictures, the still picture size must
   * be set to a size that is the same aspect ratio as the preview size we choose. Otherwise, the
   * preview images may be distorted on some devices.
   */
  public static List<CameraSizePair> generateValidPreviewSizeList(Camera camera) {

    Log.d(TAG, "++generateValidPreviewSizeList(Camera)");
    Parameters parameters = camera.getParameters();
    List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
    List<Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
    List<CameraSizePair> validPreviewSizes = new ArrayList<>();
    for (Size previewSize : supportedPreviewSizes) {
      float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

      // By looping through the picture sizes in order, we favor the higher resolutions.
      // We choose the highest resolution in order to support taking the full resolution
      // picture later.
      for (Camera.Size pictureSize : supportedPictureSizes) {
        float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
        if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
          validPreviewSizes.add(new CameraSizePair(previewSize, pictureSize));
          break;
        }
      }
    }

    // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all of
    // the preview sizes and hope that the camera can handle it.  Probably unlikely, but we still
    // account for it.
    if (validPreviewSizes.size() == 0) {
      Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size.");
      for (Camera.Size previewSize : supportedPreviewSizes) {
        // The null picture size will let us know that we shouldn't set a picture size.
        validPreviewSizes.add(new CameraSizePair(previewSize, null));
      }
    }

    return validPreviewSizes;
  }

  public static boolean isPortraitMode(Context context) {

    Log.d(TAG, "++isPortraitMode()");
    return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
  }
}
