/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zedler.patrick.grocy.scanner;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import java.util.List;

/** Barcode Detector Demo. */
public class BarcodeScannerProcessor extends VisionProcessorBase<List<Barcode>> {

  private static final String TAG = "BarcodeProcessor";

  private final BarcodeScanner barcodeScanner;

  public BarcodeScannerProcessor(
      Context context,
      BarcodeScannerOptions options,
      boolean cropImageToPreviewRect
  ) {
    super(context, cropImageToPreviewRect);
    barcodeScanner = BarcodeScanning.getClient(options);
  }

  @Override
  public void stop() {
    super.stop();
    barcodeScanner.close();
  }

  @Override
  protected Task<List<Barcode>> detectInImage(InputImage image) {
    return barcodeScanner.process(image);
  }

  @Override
  protected void onSuccess(
      @NonNull List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay) {
    for (int i = 0; i < barcodes.size(); ++i) {
      Barcode barcode = barcodes.get(i);
      graphicOverlay.add(new BarcodeGraphic(graphicOverlay, barcode));
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Barcode detection failed " + e);
  }

  /** Graphic instance for rendering Barcode position and content information in an overlay view. */
  public static class BarcodeGraphic extends GraphicOverlay.Graphic {

    private static final int TEXT_COLOR = Color.BLACK;
    private static final int MARKER_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;
    private final Paint barcodePaint;
    private final Barcode barcode;
    private final Paint labelPaint;

    BarcodeGraphic(GraphicOverlay overlay, Barcode barcode) {
      super(overlay);

      this.barcode = barcode;

      rectPaint = new Paint();
      rectPaint.setColor(MARKER_COLOR);
      rectPaint.setStyle(Paint.Style.STROKE);
      rectPaint.setStrokeWidth(STROKE_WIDTH);

      barcodePaint = new Paint();
      barcodePaint.setColor(TEXT_COLOR);
      barcodePaint.setTextSize(TEXT_SIZE);

      labelPaint = new Paint();
      labelPaint.setColor(MARKER_COLOR);
      labelPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Draws the barcode block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
      if (barcode == null) {
        throw new IllegalStateException("Attempting to draw a null barcode.");
      }

      // Draws the bounding box around the BarcodeBlock.
      RectF rect = new RectF(barcode.getBoundingBox());
      // If the image is flipped, the left will be translated to right, and the right to left.
      float x0 = translateX(rect.left);
      float x1 = translateX(rect.right);
      rect.left = min(x0, x1);
      rect.right = max(x0, x1);
      rect.top = translateY(rect.top);
      rect.bottom = translateY(rect.bottom);
      canvas.drawRect(rect, rectPaint);

      // Draws other object info.
      float lineHeight = TEXT_SIZE + (2 * STROKE_WIDTH);
      float textWidth = barcodePaint.measureText(barcode.getDisplayValue());
      canvas.drawRect(
          rect.left - STROKE_WIDTH,
          rect.top - lineHeight,
          rect.left + textWidth + (2 * STROKE_WIDTH),
          rect.top,
          labelPaint);
      // Renders the barcode at the bottom of the box.
      canvas.drawText(barcode.getDisplayValue(), rect.left, rect.top - STROKE_WIDTH, barcodePaint);
    }
  }
}
