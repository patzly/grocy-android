package xyz.zedler.patrick.grocy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.BarcodeRipple;

public class ScanInputActivity extends AppCompatActivity
        implements ScanInputCaptureManager.BarcodeListener, DecoratedBarcodeView.TorchListener {

    private ScanInputCaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private BarcodeRipple barcodeRipple;
    private ActionButton actionButtonFlash;
    private boolean isTorchOn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);

        findViewById(R.id.button_scan_close).setOnClickListener(v -> {
            finish();
        });
        findViewById(R.id.button_scan_flash).setOnClickListener(v -> {
            switchTorch();
        });

        barcodeScannerView = findViewById(R.id.barcode_scan);
        barcodeScannerView.setTorchOff();
        barcodeScannerView.setTorchListener(this);
        isTorchOn = false;

        actionButtonFlash = findViewById(R.id.button_scan_flash);
        actionButtonFlash.setIcon(R.drawable.ic_round_flash_off_to_on);

        barcodeRipple = findViewById(R.id.ripple_scan);

        if(!hasFlash()) {
            findViewById(R.id.frame_scan_flash).setVisibility(View.GONE);
        }

        capture = new ScanInputCaptureManager(this, barcodeScannerView, this);
        capture.decode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeScannerView.setTorchOff();
        capture.onDestroy();
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        barcodeRipple.pauseAnimation();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.EXTRA.SCAN_RESULT, result.getText());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void switchTorch() {
        if(isTorchOn) {
            barcodeScannerView.setTorchOff();
        } else {
            barcodeScannerView.setTorchOn();
        }
    }

    @Override
    public void onTorchOn() {
        actionButtonFlash.setIcon(R.drawable.ic_round_flash_off_to_on);
        actionButtonFlash.startIconAnimation();
        isTorchOn = true;
    }

    @Override
    public void onTorchOff() {
        actionButtonFlash.setIcon(R.drawable.ic_round_flash_on_to_off);
        actionButtonFlash.startIconAnimation();
        isTorchOn = false;
    }
}
