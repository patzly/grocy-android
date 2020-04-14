package xyz.zedler.patrick.grocy;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.ViewfinderView;

import xyz.zedler.patrick.grocy.scan.StockCaptureManager;
import xyz.zedler.patrick.grocy.view.BarcodeRipple;

public class ScanActivity extends AppCompatActivity implements StockCaptureManager.BarcodeListener {

    private StockCaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private Button switchFlashlightButton;
    private ViewfinderView viewfinderView;
    private BarcodeRipple barcodeRipple;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);

        findViewById(R.id.button_scan_close).setOnClickListener(v -> {
            finish();
        });

        barcodeScannerView = findViewById(R.id.barcode_scan);
        barcodeRipple = findViewById(R.id.ripple_scan);
        //barcodeScannerView.setTorchListener(this);

        //switchFlashlightButton = findViewById(R.id.switch_flashlight);

        //viewfinderView = findViewById(R.id.viewfinder_scan);

        // if the device does not have flashlight in its camera,
        // then remove the switch flashlight button...
        if (!hasFlash()) {
            //switchFlashlightButton.setVisibility(View.GONE);
        }

        capture = new StockCaptureManager(this, barcodeScannerView, this);
        capture.initializeFromIntent(getIntent());
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
        capture.onDestroy();
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
        );
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        barcodeRipple.pauseAnimation();
        Toast.makeText(this, result.getText(), Toast.LENGTH_LONG).show();
        new Handler().postDelayed(() -> {
            barcodeRipple.resumeAnimation();
            capture.onResume();
        }, 3000);
    }
}
