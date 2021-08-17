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

package xyz.zedler.patrick.grocy.barcode;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
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
import java.util.Timer;
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

  protected static final String MANUAL_TESTING_LOG = "LogTagForTest";
  private static final String TAG = "VisionProcessorBase";

  private final ActivityManager activityManager;
  private final Timer fpsTimer = new Timer();
  private final ScopedExecutor executor;

  // Whether this processor is already shut down
  private boolean isShutdown;

  // Used to calculate latency, running in the same thread, no sync needed.
  private int numRuns = 0;
  private long totalFrameMs = 0;
  private long maxFrameMs = 0;
  private long minFrameMs = Long.MAX_VALUE;
  private long totalDetectorMs = 0;
  private long maxDetectorMs = 0;
  private long minDetectorMs = Long.MAX_VALUE;

  // Frame count that have been processed so far in an one second interval to calculate FPS.
  private int frameProcessedInOneSecondInterval = 0;
  private int framesPerSecond = 0;


  protected VisionProcessorBase(Context context) {
    activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    /*fpsTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            framesPerSecond = frameProcessedInOneSecondInterval;
            frameProcessedInOneSecondInterval = 0;
          }
        },
        *//* delay= *//* 0,
        *//* period= *//* 1000);*/
  }

  // -----------------Code for processing live preview frame from CameraX API-----------------------
  @Override
  @RequiresApi(VERSION_CODES.KITKAT)
  @ExperimentalGetImage
  public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
    long frameStartMs = SystemClock.elapsedRealtime();
    if (isShutdown) {
      image.close();
      return;
    }

    requestDetectInImage(
            InputImage.fromByteArray(
                croppedNV21(image.getImage(), image.getCropRect()),
                image.getCropRect().width(),
                image.getCropRect().height(),
                image.getImageInfo().getRotationDegrees(),
                InputImage.IMAGE_FORMAT_NV21
            ),
            graphicOverlay,
            /* shouldShowFps= */ false,
            frameStartMs)
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

    return cropByteArray(nv21, mediaImage.getWidth(), cropRect);
  }

  private byte[] cropByteArray(byte[] array, int imageWidth, Rect cropRect) {
    byte[] croppedArray = new byte[cropRect.width() * cropRect.height()];

    int j=0;
    for (int i=0; i<array.length; i++) {
      int x = i % imageWidth;
      int y = i / imageWidth;

      if (cropRect.left <= x && x < cropRect.right && cropRect.top <= y && y < cropRect.bottom) {
        croppedArray[j] = array[i];
        j++;
      }
    }

    return croppedArray;
  }


  // -----------------Common processing logic-------------------------------------------------------
  private Task<T> requestDetectInImage(
      final InputImage image,
      final GraphicOverlay graphicOverlay,
      boolean shouldShowFps,
      long frameStartMs) {
    return setUpListener(
        detectInImage(image), graphicOverlay, shouldShowFps, frameStartMs);
  }

  private Task<T> setUpListener(
      Task<T> task,
      final GraphicOverlay graphicOverlay,
      boolean shouldShowFps,
      long frameStartMs) {
    final long detectorStartMs = SystemClock.elapsedRealtime();
    return task.addOnSuccessListener(
            executor,
            results -> {
              /*long endMs = SystemClock.elapsedRealtime();
              long currentFrameLatencyMs = endMs - frameStartMs;
              long currentDetectorLatencyMs = endMs - detectorStartMs;
              if (numRuns >= 500) {
                resetLatencyStats();
              }
              numRuns++;
              frameProcessedInOneSecondInterval++;
              totalFrameMs += currentFrameLatencyMs;
              maxFrameMs = max(currentFrameLatencyMs, maxFrameMs);
              minFrameMs = min(currentFrameLatencyMs, minFrameMs);
              totalDetectorMs += currentDetectorLatencyMs;
              maxDetectorMs = max(currentDetectorLatencyMs, maxDetectorMs);
              minDetectorMs = min(currentDetectorLatencyMs, minDetectorMs);

              // Only log inference info once per second. When frameProcessedInOneSecondInterval is
              // equal to 1, it means this is the first frame processed during the current second.
              if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Num of Runs: " + numRuns);
                Log.d(
                    TAG,
                    "Frame latency: max="
                        + maxFrameMs
                        + ", min="
                        + minFrameMs
                        + ", avg="
                        + totalFrameMs / numRuns);
                Log.d(
                    TAG,
                    "Detector latency: max="
                        + maxDetectorMs
                        + ", min="
                        + minDetectorMs
                        + ", avg="
                        + totalDetectorMs / numRuns);
                MemoryInfo mi = new MemoryInfo();
                activityManager.getMemoryInfo(mi);
                long availableMegs = mi.availMem / 0x100000L;
                Log.d(TAG, "Memory available in system: " + availableMegs + " MB");
                temperatureMonitor.logTemperature();
              }*/
              /*if (originalCameraImage != null) {
                graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
              }*/
              graphicOverlay.clear();
              VisionProcessorBase.this.onSuccess(results, graphicOverlay);
              /*if (!PreferenceUtils.shouldHideDetectionInfo(graphicOverlay.getContext())) {
                graphicOverlay.add(
                    new InferenceInfoGraphic(
                        graphicOverlay,
                        currentFrameLatencyMs,
                        currentDetectorLatencyMs,
                        shouldShowFps ? framesPerSecond : null));
              }*/
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
    resetLatencyStats();
    //fpsTimer.cancel();
  }

  private void resetLatencyStats() {
    numRuns = 0;
    totalFrameMs = 0;
    maxFrameMs = 0;
    minFrameMs = Long.MAX_VALUE;
    totalDetectorMs = 0;
    maxDetectorMs = 0;
    minDetectorMs = Long.MAX_VALUE;
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
