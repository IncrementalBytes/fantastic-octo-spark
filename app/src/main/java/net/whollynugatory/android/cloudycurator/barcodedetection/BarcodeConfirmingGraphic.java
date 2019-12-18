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
import android.graphics.Path;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import net.whollynugatory.android.cloudycurator.PreferenceUtils;
import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;

/**
 * Guides user to move camera closer to confirm the detected barcode.
 **/
class BarcodeConfirmingGraphic extends BarcodeGraphicBase {

  private final FirebaseVisionBarcode mBarcode;

  BarcodeConfirmingGraphic(GraphicOverlay overlay, FirebaseVisionBarcode barcode) {
    super(overlay);

    mBarcode = barcode;
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    // Draws a highlighted path to indicate the current progress to meet size requirement.
    float sizeProgress = PreferenceUtils.getProgressToMeetBarcodeSizeRequirement(overlay, mBarcode);
    Path path = new Path();
    if (sizeProgress > 0.95f) { // To have a completed path with all corners rounded.
      path.moveTo(BoxRect.left, BoxRect.top);
      path.lineTo(BoxRect.right, BoxRect.top);
      path.lineTo(BoxRect.right, BoxRect.bottom);
      path.lineTo(BoxRect.left, BoxRect.bottom);
      path.close();
    } else {
      path.moveTo(BoxRect.left, BoxRect.top + BoxRect.height() * sizeProgress);
      path.lineTo(BoxRect.left, BoxRect.top);
      path.lineTo(BoxRect.left + BoxRect.width() * sizeProgress, BoxRect.top);
      path.moveTo(BoxRect.right, BoxRect.bottom - BoxRect.height() * sizeProgress);
      path.lineTo(BoxRect.right, BoxRect.bottom);
      path.lineTo(BoxRect.right - BoxRect.width() * sizeProgress, BoxRect.bottom);
    }

    canvas.drawPath(path, PathPaint);
  }
}
