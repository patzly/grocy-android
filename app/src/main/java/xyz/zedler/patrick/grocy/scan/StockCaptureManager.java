package xyz.zedler.patrick.grocy.scan;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.InactivityTimer;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CameraPreview;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;
import java.util.Map;

import xyz.zedler.patrick.grocy.R;

public class StockCaptureManager {

    private static final String TAG = StockCaptureManager.class.getSimpleName();

    private static int cameraPermissionReqCode = 250;

    private Activity activity;
    private DecoratedBarcodeView barcodeView;
    private boolean returnBarcodeImagePath = false;

    private boolean showDialogIfMissingCameraPermission = true;
    private String missingCameraPermissionDialogMessage = "";

    private boolean destroyed = false;

    private InactivityTimer inactivityTimer;

    private Handler handler;

    private boolean finishWhenClosed = false;

    private BarcodeListener barcodeListener;

    public BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(final BarcodeResult result) {
            barcodeView.pause();

            barcodeListener.onBarcodeResult(result);
            //handler.post(() -> returnResult(result));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    };

    public interface BarcodeListener {
        void onBarcodeResult(BarcodeResult result);
    }

    private final CameraPreview.StateListener stateListener = new CameraPreview.StateListener() {
        @Override
        public void previewSized() {

        }

        @Override
        public void previewStarted() {

        }

        @Override
        public void previewStopped() {

        }

        @Override
        public void cameraError(Exception error) {
            displayFrameworkBugMessageAndExit(
                    activity.getString(R.string.zxing_msg_camera_framework_bug)
            );
        }

        @Override
        public void cameraClosed() {
            if (finishWhenClosed) {
                Log.d(TAG, "Camera closed; finishing activity");
                finish();
            }
        }
    };

    public StockCaptureManager(Activity activity, DecoratedBarcodeView barcodeView, BarcodeListener barcodeListener) {
        this.activity = activity;
        this.barcodeView = barcodeView;
        this.barcodeListener = barcodeListener;
        barcodeView.getBarcodeView().addStateListener(stateListener);

        handler = new Handler();

        inactivityTimer = new InactivityTimer(activity, () -> {
            Log.d(TAG, "Finishing due to inactivity");
            finish();
        });
    }

    /**
     * Start decoding.
     */
    public void decode() {
        barcodeView.decodeContinuous(callback);
    }

    /**
     * Call from Activity#onResume().
     */
    public void onResume() {
        if (Build.VERSION.SDK_INT >= 23) {
            openCameraWithPermission();
        } else {
            barcodeView.resume();
        }
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        inactivityTimer.start();
    }

    private boolean askedPermission = false;

    @TargetApi(23)
    private void openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this.activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        } else if (!askedPermission) {
            ActivityCompat.requestPermissions(this.activity,
                    new String[]{Manifest.permission.CAMERA},
                    cameraPermissionReqCode);
            askedPermission = true;
        } // else wait for permission result
    }

    /**
     * Call from Activity#onRequestPermissionsResult
     * @param requestCode The request code passed in {@link ActivityCompat#requestPermissions(Activity, String[], int)}.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link PackageManager#PERMISSION_GRANTED}
     *     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == cameraPermissionReqCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                barcodeView.resume();
            } else {
                setMissingCameraPermissionResult();

                if (showDialogIfMissingCameraPermission) {
                    displayFrameworkBugMessageAndExit(missingCameraPermissionDialogMessage);
                } else {
                    closeAndFinish();
                }
            }
        }
    }

    /**
     * Call from Activity#onPause().
     */
    public void onPause() {
        activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        inactivityTimer.cancel();
        barcodeView.pauseAndWait();
    }

    /**
     * Call from Activity#onDestroy().
     */
    public void onDestroy() {
        destroyed = true;
        inactivityTimer.cancel();
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * Create a intent to return as the Activity result.
     *
     * @param rawResult the BarcodeResult, must not be null.
     * @return the Intent
     */
    public static Intent resultIntent(BarcodeResult rawResult) {
        Intent intent = new Intent(Intents.Scan.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
        intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
        byte[] rawBytes = rawResult.getRawBytes();
        if (rawBytes != null && rawBytes.length > 0) {
            intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
        }
        Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                        metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
            }
            Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
            if (orientation != null) {
                intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
            }
            String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
            if (ecLevel != null) {
                intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
            }
            @SuppressWarnings("unchecked")
            Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
            if (byteSegments != null) {
                int i = 0;
                for (byte[] byteSegment : byteSegments) {
                    intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
                    i++;
                }
            }
        }
        return intent;
    }

    private void finish() {
        activity.finish();
    }

    protected void closeAndFinish() {
        if (barcodeView.getBarcodeView().isCameraClosed()) {
            //finish();
        } else {
            finishWhenClosed = true;
        }

        barcodeView.pause();
        inactivityTimer.cancel();
    }

    private void setMissingCameraPermissionResult() {
        Intent intent = new Intent(Intents.Scan.ACTION);
        intent.putExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, true);
        activity.setResult(Activity.RESULT_CANCELED, intent);
    }

    protected void returnResultTimeout() {
        Intent intent = new Intent(Intents.Scan.ACTION);
        intent.putExtra(Intents.Scan.TIMEOUT, true);
        activity.setResult(Activity.RESULT_CANCELED, intent);
        closeAndFinish();
    }

    protected void returnResult(BarcodeResult rawResult) {
        Intent intent = resultIntent(rawResult);
        activity.setResult(Activity.RESULT_OK, intent);
        closeAndFinish();
    }

    protected void displayFrameworkBugMessageAndExit(String message) {
        if (activity.isFinishing() || this.destroyed || finishWhenClosed) {
            return;
        }

        if (message.isEmpty()) {
            message = activity.getString(R.string.zxing_msg_camera_framework_bug);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.zxing_app_name));
        builder.setMessage(message);
        builder.setPositiveButton(R.string.zxing_button_ok, (dialog, which) -> finish());
        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    public static int getCameraPermissionReqCode() {
        return cameraPermissionReqCode;
    }

    public static void setCameraPermissionReqCode(int cameraPermissionReqCode) {
        StockCaptureManager.cameraPermissionReqCode = cameraPermissionReqCode;
    }

    /**
     * If set to true, shows the default error dialog if camera permission is missing.
     * <p>
     * If set to false, instead the capture manager just finishes.
     * <p>
     * In both cases, the activity result is set to {@link Intents.Scan#MISSING_CAMERA_PERMISSION}
     * and cancelled
     */
    public void setShowMissingCameraPermissionDialog(boolean visible) {
        setShowMissingCameraPermissionDialog(visible, "");
    }

    /**
     * If set to true, shows the specified error dialog message if camera permission is missing.
     * <p>
     * If set to false, instead the capture manager just finishes.
     * <p>
     * In both cases, the activity result is set to {@link Intents.Scan#MISSING_CAMERA_PERMISSION}
     * and cancelled
     */
    public void setShowMissingCameraPermissionDialog(boolean visible, String message) {
        showDialogIfMissingCameraPermission = visible;
        missingCameraPermissionDialogMessage = message != null ? message : "";
    }
}
