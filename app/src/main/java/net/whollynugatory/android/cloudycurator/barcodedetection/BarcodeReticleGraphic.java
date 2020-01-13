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

package net.whollynugatory.android.cloudycurator.barcodedetection;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.camera.CameraReticuleAnimator;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;

import androidx.core.content.ContextCompat;

/**
 * A camera reticule that locates at the center of canvas to indicate the system is active but has not detected a barcode yet.
 **/
class BarcodeReticuleGraphic extends BarcodeGraphicBase {

  private final CameraReticuleAnimator mAnimator;

  private final Paint mRipplePaint;
  private final int mRippleSizeOffset;
  private final int mRippleStrokeWidth;
  private final int mRippleAlpha;

  BarcodeReticuleGraphic(GraphicOverlay overlay, CameraReticuleAnimator animator) {
    super(overlay);

    mAnimator = animator;
    Resources resources = overlay.getResources();
    mRipplePaint = new Paint();
    mRipplePaint.setStyle(Paint.Style.STROKE);
    mRipplePaint.setColor(ContextCompat.getColor(context, R.color.reticule_ripple));
    mRippleSizeOffset = resources.getDimensionPixelOffset(R.dimen.barcode_reticule_ripple_size_offset);
    mRippleStrokeWidth = resources.getDimensionPixelOffset(R.dimen.barcode_reticule_ripple_stroke_width);
    mRippleAlpha = mRipplePaint.getAlpha();
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    // Draws the ripple to simulate the breathing animation effect.
    mRipplePaint.setAlpha((int) (mRippleAlpha * mAnimator.getRippleAlphaScale()));
    mRipplePaint.setStrokeWidth(mRippleStrokeWidth * mAnimator.getRippleStrokeWidthScale());
    float offset = mRippleSizeOffset * mAnimator.getRippleSizeScale();
    RectF rippleRect =
      new RectF(
        BoxRect.left - offset,
        BoxRect.top - offset,
        BoxRect.right + offset,
        BoxRect.bottom + offset);
    canvas.drawRoundRect(rippleRect, BoxCornerRadius, BoxCornerRadius, mRipplePaint);
  }
}
