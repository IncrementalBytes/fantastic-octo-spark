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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

import androidx.annotation.GuardedBy;

public class BarcodeProcessor {

  private static final String TAG = BaseActivity.BASE_TAG + "BarcodeProcessor";

  public interface OnBarcodeProcessorListener {

    void onBarcodeProcessed(String barcode);
  }

  @GuardedBy("this")
  private ByteBuffer mLatestImage;

  @GuardedBy("this")
  private FrameMetadata mLatestImageMetaData;

  @GuardedBy("this")
  private ByteBuffer mProcessingImage;

  @GuardedBy("this")
  private FrameMetadata mProcessingMetaData;

  private OnBarcodeProcessorListener mCallback;

  private final FirebaseVisionBarcodeDetector mDetector;

  public BarcodeProcessor(Context context) {

    try {
      mCallback = (OnBarcodeProcessorListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    FirebaseVisionBarcodeDetectorOptions options =
      new FirebaseVisionBarcodeDetectorOptions.Builder()
        .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_UPC_A, FirebaseVisionBarcode.FORMAT_UPC_E)
        .build();
    mDetector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
  }

  synchronized void process(ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {

    Log.d(TAG, "++process(ByteBuffer, FrameMetadata, GraphicOverlay)");
    mLatestImage = data;
    mLatestImageMetaData = frameMetadata;
    if (mProcessingImage == null && mProcessingMetaData == null) {
      processLatestImage(graphicOverlay);
    }
  }

  void stop() {

    Log.d(TAG, "++stop()");
    try {
      mDetector.close();
    } catch (IOException e) {
      Log.e(TAG, "Exception thrown while trying to close Barcode Detector.", e);
    }
  }

  /*
    Private Method(s)
   */
  private void detectInVisionImage(
    final Bitmap originalCameraImage,
    FirebaseVisionImage image,
    final FrameMetadata metadata,
    final GraphicOverlay graphicOverlay) {

    Log.d(TAG, "++detectInVisionImage(Bitmap, FirebaseVisionImage, FrameMetadata, GraphicOverlay)");
    mDetector.detectInImage(image)
      .addOnSuccessListener(firebaseVisionBarcodes -> {

        graphicOverlay.clear();
        if (originalCameraImage != null) {
          CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
          graphicOverlay.add(imageGraphic);
        }

        for (int i = 0; i < firebaseVisionBarcodes.size(); ++i) {
          FirebaseVisionBarcode barcode = firebaseVisionBarcodes.get(i);
          mCallback.onBarcodeProcessed(barcode.getDisplayValue());
        }

        graphicOverlay.postInvalidate();
        processLatestImage(graphicOverlay);
      })
      .addOnFailureListener(e -> Log.e(TAG, "Barcode detection failed.", e));
  }

  private void processImage(ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {

    Log.d(TAG, "++processImage(ByteBuffer, FrameMetadata, GraphicOverlay)");
    FirebaseVisionImageMetadata metadata =
      new FirebaseVisionImageMetadata.Builder()
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        .setWidth(frameMetadata.getWidth())
        .setHeight(frameMetadata.getHeight())
        .setRotation(frameMetadata.getRotation())
        .build();

    Bitmap bitmap = BitmapUtils.getBitmap(data, frameMetadata);
    detectInVisionImage(bitmap, FirebaseVisionImage.fromByteBuffer(data, metadata), frameMetadata, graphicOverlay);
  }

  private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {

    Log.d(TAG, "++processLatestImage(GraphicOverlay)");
    mProcessingImage = mLatestImage;
    mProcessingMetaData = mLatestImageMetaData;
    mLatestImage = null;
    mLatestImageMetaData = null;
    if (mProcessingImage != null && mProcessingMetaData != null) {
      processImage(mProcessingImage, mProcessingMetaData, graphicOverlay);
    }
  }
}
