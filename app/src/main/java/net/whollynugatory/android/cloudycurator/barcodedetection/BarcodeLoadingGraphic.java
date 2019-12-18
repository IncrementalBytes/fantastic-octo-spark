package net.whollynugatory.android.cloudycurator.barcodedetection;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;

import net.whollynugatory.android.cloudycurator.camera.GraphicOverlay;

/** Draws the graphic to indicate the barcode result is in loading. */
class BarcodeLoadingGraphic extends BarcodeGraphicBase {

  private final ValueAnimator loadingAnimator;
  private final PointF[] boxClockwiseCoordinates;
  private final Point[] coordinateOffsetBits;
  private final PointF lastPathPoint = new PointF();

  BarcodeLoadingGraphic(GraphicOverlay overlay, ValueAnimator loadingAnimator) {
    super(overlay);

    this.loadingAnimator = loadingAnimator;
    boxClockwiseCoordinates = new PointF[] {
        new PointF(BoxRect.left, BoxRect.top), new PointF(BoxRect.right, BoxRect.top),
        new PointF(BoxRect.right, BoxRect.bottom), new PointF(BoxRect.left, BoxRect.bottom)
      };
    coordinateOffsetBits = new Point[] {new Point(1, 0), new Point(0, 1), new Point(-1, 0), new Point(0, -1)};
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    float boxPerimeter = (BoxRect.width() + BoxRect.height()) * 2;
    Path path = new Path();
    // The distance between the box's left-top corner and the starting point of white colored path.
    float offsetLen = (boxPerimeter * (float) loadingAnimator.getAnimatedValue()) % boxPerimeter;
    int i;
    for (i = 0; i < 4; i++) {
      float edgeLen = (i % 2 == 0) ? BoxRect.width() : BoxRect.height();
      if (offsetLen <= edgeLen) {
        lastPathPoint.x = boxClockwiseCoordinates[i].x + coordinateOffsetBits[i].x * offsetLen;
        lastPathPoint.y = boxClockwiseCoordinates[i].y + coordinateOffsetBits[i].y * offsetLen;
        path.moveTo(lastPathPoint.x, lastPathPoint.y);
        break;
      }

      offsetLen -= edgeLen;
    }

    // Computes the path based on the determined starting point and path length.
    float pathLen = boxPerimeter * 0.3f;
    for (int j = 0; j < 4; j++) {
      int index = (i + j) % 4;
      int nextIndex = (i + j + 1) % 4;
      // The length between path's current end point and reticle box's next coordinate point.
      float lineLen =
        Math.abs(boxClockwiseCoordinates[nextIndex].x - lastPathPoint.x)
          + Math.abs(boxClockwiseCoordinates[nextIndex].y - lastPathPoint.y);
      if (lineLen >= pathLen) {
        path.lineTo(
          lastPathPoint.x + pathLen * coordinateOffsetBits[index].x,
          lastPathPoint.y + pathLen * coordinateOffsetBits[index].y);
        break;
      }

      lastPathPoint.x = boxClockwiseCoordinates[nextIndex].x;
      lastPathPoint.y = boxClockwiseCoordinates[nextIndex].y;
      path.lineTo(lastPathPoint.x, lastPathPoint.y);
      pathLen -= lineLen;
    }

    canvas.drawPath(path, PathPaint);
  }
}