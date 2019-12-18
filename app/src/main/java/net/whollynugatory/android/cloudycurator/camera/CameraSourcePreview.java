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

package net.whollynugatory.android.cloudycurator.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.gms.common.images.Size;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.Utils;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Preview the camera image in the screen. */
public class CameraSourcePreview extends FrameLayout {

  private static final String TAG = BaseActivity.BASE_TAG + "CameraSourcePreview";

  private final SurfaceView mSurfaceView;
  private GraphicOverlay mGraphicOverlay;
  private boolean mStartRequested = false;
  private boolean mSurfaceAvailable = false;
  private CameraSource mCameraSource;
  private Size mCameraPreviewSize;

  public CameraSourcePreview(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    mSurfaceView = new SurfaceView(context);
    mSurfaceView.getHolder().addCallback(new SurfaceCallback());
    addView(mSurfaceView);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mGraphicOverlay = findViewById(R.id.camera_preview_graphic_overlay);
  }

  public void start(CameraSource cameraSource) throws IOException {

    mCameraSource = cameraSource;
    mStartRequested = true;
    startIfReady();
  }

  public void stop() {

    if (mCameraSource != null) {
      mCameraSource.stop();
      mCameraSource = null;
      mStartRequested = false;
    }
  }

  private void startIfReady() throws IOException {

    if (mStartRequested && mSurfaceAvailable) {
      mCameraSource.start(mSurfaceView.getHolder());
      requestLayout();

      if (mGraphicOverlay != null) {
        mGraphicOverlay.setCameraInfo(mCameraSource);
        mGraphicOverlay.clear();
      }

      mStartRequested = false;
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

    int layoutWidth = right - left;
    int layoutHeight = bottom - top;

    if (mCameraSource != null && mCameraSource.getPreviewSize() != null) {
      mCameraPreviewSize = mCameraSource.getPreviewSize();
    }

    float previewSizeRatio = (float) layoutWidth / layoutHeight;
    if (mCameraPreviewSize != null) {
      if (Utils.isPortraitMode(getContext())) {
        // Camera's natural orientation is landscape, so need to swap width and height.
        previewSizeRatio = (float) mCameraPreviewSize.getHeight() / mCameraPreviewSize.getWidth();
      } else {
        previewSizeRatio = (float) mCameraPreviewSize.getWidth() / mCameraPreviewSize.getHeight();
      }
    }

    // Match the width of the child view to its parent.
    int childWidth = layoutWidth;
    int childHeight = (int) (childWidth / previewSizeRatio);
    if (childHeight <= layoutHeight) {
      for (int i = 0; i < getChildCount(); ++i) {
        getChildAt(i).layout(0, 0, childWidth, childHeight);
      }
    } else {
      // When the child view is too tall to be fitted in its parent: If the child view is static
      // overlay view container (contains views such as bottom prompt chip), we apply the size of
      // the parent view to it. Otherwise, we offset the top/bottom position equally to position it
      // in the center of the parent.
      int excessLenInHalf = (childHeight - layoutHeight) / 2;
      for (int i = 0; i < getChildCount(); ++i) {
        View childView = getChildAt(i);
        if (childView.getId() == R.id.static_overlay_container) {
          childView.layout(0, 0, childWidth, layoutHeight);
        } else {
          childView.layout(0, -excessLenInHalf, childWidth, layoutHeight + excessLenInHalf);
        }
      }
    }

    try {
      startIfReady();
    } catch (IOException e) {
      Log.e(TAG, "Could not start camera source.", e);
    }
  }

  private class SurfaceCallback implements SurfaceHolder.Callback {

    @Override
    public void surfaceCreated(SurfaceHolder surface) {

      mSurfaceAvailable = true;
      try {
        startIfReady();
      } catch (IOException e) {
        Log.e(TAG, "Could not start camera source.", e);
      }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surface) {
      mSurfaceAvailable = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
  }
}
