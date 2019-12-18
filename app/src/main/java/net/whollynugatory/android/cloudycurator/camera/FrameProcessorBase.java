package net.whollynugatory.android.cloudycurator.camera;

import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.nio.ByteBuffer;

import androidx.annotation.GuardedBy;

/** Abstract base class of {@link FrameProcessor}. */
public abstract class FrameProcessorBase<T> implements FrameProcessor {

  private static final String TAG = "FrameProcessorBase";

  // To keep the latest frame and its metadata.
  @GuardedBy("this")
  private ByteBuffer latestFrame;

  @GuardedBy("this")
  private FrameMetadata latestFrameMetaData;

  // To keep the frame and metadata in process.
  @GuardedBy("this")
  private ByteBuffer processingFrame;

  @GuardedBy("this")
  private FrameMetadata processingFrameMetaData;

  @Override
  public synchronized void process(
    ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) {
    latestFrame = data;
    latestFrameMetaData = frameMetadata;
    if (processingFrame == null && processingFrameMetaData == null) {
      processLatestFrame(graphicOverlay);
    }
  }

  private synchronized void processLatestFrame(GraphicOverlay graphicOverlay) {
    processingFrame = latestFrame;
    processingFrameMetaData = latestFrameMetaData;
    latestFrame = null;
    latestFrameMetaData = null;
    if (processingFrame != null && processingFrameMetaData != null) {
      FirebaseVisionImageMetadata metadata =
        new FirebaseVisionImageMetadata.Builder()
          .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
          .setWidth(processingFrameMetaData.width)
          .setHeight(processingFrameMetaData.height)
          .setRotation(processingFrameMetaData.rotation)
          .build();
      FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(processingFrame, metadata);
      long startMs = SystemClock.elapsedRealtime();
      detectInImage(image)
        .addOnSuccessListener(
          results -> {
            Log.d(TAG, "Latency is: " + (SystemClock.elapsedRealtime() - startMs));
            FrameProcessorBase.this.onSuccess(image, results, graphicOverlay);
            processLatestFrame(graphicOverlay);
          })
        .addOnFailureListener(FrameProcessorBase.this::onFailure);
    }
  }

  protected abstract Task<T> detectInImage(FirebaseVisionImage image);

  /** Be called when the detection succeeds. */
  protected abstract void onSuccess(
    FirebaseVisionImage image, T results, GraphicOverlay graphicOverlay);

  protected abstract void onFailure(Exception e);
}
