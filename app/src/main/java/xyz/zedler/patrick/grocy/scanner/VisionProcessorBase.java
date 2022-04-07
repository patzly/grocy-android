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

import android.graphics.Rect;
import android.media.Image;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.mlkit.vision.common.InputImage;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for vision frame processors. Subclasses need to implement {@link
 * #onSuccess(Object, GraphicOverlay)} to define what they want to with the detection results and
 * {@link #detectInImage(InputImage)} to specify the detector object.
 *
 * @param <T> The type of the detected feature.
 */
public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

  private static final String TAG = "VisionProcessorBase";

  private final ScopedExecutor executor;

  private final boolean cropImageToPreviewRect;
  // Whether this processor is already shut down
  private boolean isShutdown;


  protected VisionProcessorBase(boolean cropImageToPreviewRect) {
    executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    this.cropImageToPreviewRect = cropImageToPreviewRect;
  }

  // -----------------Code for processing live preview frame from CameraX API-----------------------
  @Override
  @RequiresApi(VERSION_CODES.KITKAT)
  @ExperimentalGetImage
  public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
    if (image == null || image.getImage() == null) return;

    if (isShutdown) {
      image.close();
      return;
    }

    InputImage inputImage;
    if (cropImageToPreviewRect) {
      inputImage = InputImage.fromByteArray(
          croppedNV21(image.getImage(), image.getCropRect()),
          image.getCropRect().width(),
          image.getCropRect().height(),
          image.getImageInfo().getRotationDegrees(),
          InputImage.IMAGE_FORMAT_NV21
      );
    } else {
      inputImage = InputImage.fromMediaImage(
          image.getImage(),
          image.getImageInfo().getRotationDegrees()
      );
    }

    requestDetectInImage(inputImage, graphicOverlay)
        // When the image is from CameraX analysis use case, must call image.close() on received
        // images when finished using them. Otherwise, new images may not be received or the camera
        // may stall.
        .addOnCompleteListener(results -> image.close());
  }

  private byte[] croppedNV21(Image mediaImage, Rect cropRect) {
    ByteBuffer yBuffer = mediaImage.getPlanes()[0].getBuffer(); // Y
    ByteBuffer vuBuffer = mediaImage.getPlanes()[2].getBuffer(); // VU

    int ySize = yBuffer.remaining();
    int vuSize = vuBuffer.remaining();

    byte[] nv21 = new byte[ySize + vuSize];

    yBuffer.get(nv21, 0, ySize);
    vuBuffer.get(nv21, ySize, vuSize);

    return clipNV21(
        nv21,
        mediaImage.getWidth(),
        mediaImage.getHeight(),
        cropRect.left,
        cropRect.top,
        cropRect.width(),
        cropRect.height()
    );
  }

  /**
   * NV21 cropping algorithm efficiency 3ms
   *
   * Source: https://www.programmersought.com/article/75461140907/
   *
   * @param src source data
   * @param width source width
   * @param height source height
   * @param left vertex coordinates
   * @param top vertex coordinates
   * @param clip_w Cropped width
   * @param clip_h High after cropping
   * @return Cropped data
   */
  public static byte[] clipNV21(byte[] src, int width, int height, int left, int top, int clip_w, int clip_h) {
    if (left > width || top > height) {
      return null;
    }
    int x = left * 2 / 2, y = top * 2 / 2;
    int w = clip_w * 2 / 2, h = clip_h * 2 / 2;
    int y_unit = w * h;
    int uv = y_unit / 2;
    byte[] nData = new byte[y_unit + uv];
    int uv_index_dst = w * h - y / 2 * w;
    int uv_index_src = width * height + x;
    int srcPos0 = y * width;
    int destPos0 = 0;
    int uvSrcPos0 = uv_index_src;
    int uvDestPos0 = uv_index_dst;
    for (int i = y; i < y + h; i++) {
      System.arraycopy(src, srcPos0 + x, nData, destPos0, w);//y memory block copy
      srcPos0 += width;
      destPos0 += w;
      if ((i & 1) == 0) {
        System.arraycopy(src, uvSrcPos0, nData, uvDestPos0, w);//uv memory block copy
        uvSrcPos0 += width;
        uvDestPos0 += w;
      }
    }
    return nData;
  }

  // -----------------Common processing logic-------------------------------------------------------
  private Task<T> requestDetectInImage(final InputImage image, final GraphicOverlay graphicOverlay) {
    return setUpListener(detectInImage(image), graphicOverlay);
  }

  private Task<T> setUpListener(Task<T> task, final GraphicOverlay graphicOverlay) {
    return task.addOnSuccessListener(
            executor,
            results -> {
              graphicOverlay.clear();
              VisionProcessorBase.this.onSuccess(results, graphicOverlay);
              graphicOverlay.postInvalidate();
            })
        .addOnFailureListener(
            executor,
            e -> {
              graphicOverlay.clear();
              graphicOverlay.postInvalidate();
              String error = "Failed to process. Error: " + e.getLocalizedMessage();
              Toast.makeText(
                      graphicOverlay.getContext(),
                      error + "\nCause: " + e.getCause(),
                      Toast.LENGTH_SHORT)
                  .show();
              Log.d(TAG, error);
              e.printStackTrace();
              VisionProcessorBase.this.onFailure(e);
            });
  }

  @Override
  public void stop() {
    executor.shutdown();
    isShutdown = true;
  }

  protected abstract Task<T> detectInImage(InputImage image);

  protected abstract void onSuccess(@NonNull T results, @NonNull GraphicOverlay graphicOverlay);

  protected abstract void onFailure(@NonNull Exception e);

  public static class ScopedExecutor implements Executor {

    private final Executor executor;
    private final AtomicBoolean shutdown = new AtomicBoolean();

    public ScopedExecutor(@NonNull Executor executor) {
      this.executor = executor;
    }

    @Override
    public void execute(@NonNull Runnable command) {
      // Return early if this object has been shut down.
      if (shutdown.get()) {
        return;
      }
      executor.execute(
          () -> {
            // Check again in case it has been shut down in the mean time.
            if (shutdown.get()) {
              return;
            }
            command.run();
          });
    }

    /**
     * After this method is called, no runnables that have been submitted or are subsequently
     * submitted will start to execute, turning this executor into a no-op.
     *
     * <p>Runnables that have already started to execute will continue.
     */
    public void shutdown() {
      shutdown.set(true);
    }
  }
}
