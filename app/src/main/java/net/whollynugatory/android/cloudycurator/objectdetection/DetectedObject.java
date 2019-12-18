package net.whollynugatory.android.cloudycurator.objectdetection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import androidx.annotation.Nullable;

/**
 * Holds the detected object and its related image info.
 */
public class DetectedObject {

  private static final String TAG = "DetectedObject";
  private static final int MAX_IMAGE_WIDTH = 640;

  private final FirebaseVisionObject object;
  private final int objectIndex;
  private final FirebaseVisionImage image;

  @Nullable
  private Bitmap bitmap = null;
  @Nullable
  private byte[] jpegBytes = null;

  public DetectedObject(FirebaseVisionObject object, int objectIndex, FirebaseVisionImage image) {
    this.object = object;
    this.objectIndex = objectIndex;
    this.image = image;
  }

  @Nullable
  public Integer getObjectId() {
    return object.getTrackingId();
  }

  public int getObjectIndex() {
    return objectIndex;
  }

  public Rect getBoundingBox() {
    return object.getBoundingBox();
  }

  public synchronized Bitmap getBitmap() {
    if (bitmap == null) {
      Rect boundingBox = object.getBoundingBox();
      bitmap =
        Bitmap.createBitmap(
          image.getBitmap(),
          boundingBox.left,
          boundingBox.top,
          boundingBox.width(),
          boundingBox.height());
      if (bitmap.getWidth() > MAX_IMAGE_WIDTH) {
        int dstHeight = (int) ((float) MAX_IMAGE_WIDTH / bitmap.getWidth() * bitmap.getHeight());
        bitmap = Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_WIDTH, dstHeight, /* filter= */ false);
      }
    }

    return bitmap;
  }

  @Nullable
  public synchronized byte[] getImageData() {
    if (jpegBytes == null) {
      try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
        getBitmap().compress(Bitmap.CompressFormat.JPEG, /* quality= */ 100, stream);
        jpegBytes = stream.toByteArray();
      } catch (IOException e) {
        Log.e(TAG, "Error getting object image data!");
      }
    }

    return jpegBytes;
  }
}
