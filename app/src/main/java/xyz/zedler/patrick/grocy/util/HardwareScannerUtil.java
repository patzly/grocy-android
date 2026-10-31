package xyz.zedler.patrick.grocy.util;

import android.app.Activity;

/**
 * Single entry point for built-in hardware scanners (Honeywell, Zebra).
 * Fragments call activate() in onResume() and deactivate() in onPause().
 */
public class HardwareScannerUtil {

  private final HoneywellScannerUtil honeywellScannerUtil;
  private final ZebraScannerUtil zebraScannerUtil;

  public HardwareScannerUtil(Activity activity, BarcodeListener barcodeListener) {
    honeywellScannerUtil = new HoneywellScannerUtil(activity, barcodeListener);
    zebraScannerUtil = new ZebraScannerUtil(activity, barcodeListener);
  }

  public void activate() {
    honeywellScannerUtil.activate();
    zebraScannerUtil.activate();
  }

  public void deactivate() {
    honeywellScannerUtil.deactivate();
    zebraScannerUtil.deactivate();
  }

  public interface BarcodeListener {
    void onBarcodeRecognized(String barcode, byte[] barcodeBytes);
  }
}
