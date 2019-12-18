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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import net.whollynugatory.android.cloudycurator.PreferenceUtils;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay.Graphic;

import androidx.core.content.ContextCompat;

abstract class BarcodeGraphicBase extends Graphic {

  private final Paint mBoxPaint;
  private final Paint mScrimPaint;
  private final Paint mEraserPaint;

  final int BoxCornerRadius;
  final Paint PathPaint;
  final RectF BoxRect;

  BarcodeGraphicBase(GraphicOverlay overlay) {
    super(overlay);

    mBoxPaint = new Paint();
    mBoxPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_stroke));
    mBoxPaint.setStyle(Paint.Style.STROKE);
    mBoxPaint.setStrokeWidth(context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width));

    mScrimPaint = new Paint();
    mScrimPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_background));
    mEraserPaint = new Paint();
    mEraserPaint.setStrokeWidth(mBoxPaint.getStrokeWidth());
    mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

    BoxCornerRadius = context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius);

    PathPaint = new Paint();
    PathPaint.setColor(Color.WHITE);
    PathPaint.setStyle(Paint.Style.STROKE);
    PathPaint.setStrokeWidth(mBoxPaint.getStrokeWidth());
    PathPaint.setPathEffect(new CornerPathEffect(BoxCornerRadius));

    BoxRect = PreferenceUtils.getBarcodeReticleBox(overlay);
  }

  @Override
  protected void draw(Canvas canvas) {

    // Draws the dark background scrim and leaves the box area clear.
    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mScrimPaint);
    // As the stroke is always centered, so erase twice with FILL and STROKE respectively to clear
    // all area that the box rect would occupy.
    mEraserPaint.setStyle(Paint.Style.FILL);
    canvas.drawRoundRect(BoxRect, BoxCornerRadius, BoxCornerRadius, mEraserPaint);
    mEraserPaint.setStyle(Paint.Style.STROKE);
    canvas.drawRoundRect(BoxRect, BoxCornerRadius, BoxCornerRadius, mEraserPaint);

    // Draws the box.
    canvas.drawRoundRect(BoxRect, BoxCornerRadius, BoxCornerRadius, mBoxPaint);
  }
}
