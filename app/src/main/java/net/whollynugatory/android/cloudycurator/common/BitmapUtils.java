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
package net.whollynugatory.android.cloudycurator.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.Nullable;

class BitmapUtils {

  private static final String TAG = BaseActivity.BASE_TAG + "BitmapUtils";

  /**
   * Convert NV21 format byte buffer to bitmap.
   */
  @Nullable
  static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata) {

    data.rewind();
    byte[] imageInBuffer = new byte[data.limit()];
    data.get(imageInBuffer, 0, imageInBuffer.length);
    try {
      YuvImage image = new YuvImage(imageInBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);
      Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
      stream.close();
      return rotateBitmap(bmp, metadata.getRotation(), metadata.getCameraFacing());
    } catch (Exception e) {
      Log.e(TAG, "Error in get bitmap.", e);
    }

    return null;
  }

  /**
   * Rotates a bitmap if it is converted from a bytebuffer.
   */
  private static Bitmap rotateBitmap(Bitmap bitmap, int rotation, int facing) {

    Matrix matrix = new Matrix();
    int rotationDegree = 0;
    switch (rotation) {
      case FirebaseVisionImageMetadata.ROTATION_90:
        rotationDegree = 90;
        break;
      case FirebaseVisionImageMetadata.ROTATION_180:
        rotationDegree = 180;
        break;
      case FirebaseVisionImageMetadata.ROTATION_270:
        rotationDegree = 270;
        break;
      default:
        break;
    }

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegree);
    if (facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
      return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    } else {
      // Mirror the image along X axis for front-facing camera image.
      matrix.postScale(-1.0f, 1.0f);
      return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
  }
}
