package xyz.zedler.patrick.grocy.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.BarcodeFormats;

/**
 * Receives scans from the built-in scanner of Zebra devices through DataWedge.
 *
 * A DataWedge profile is created and associated with this app, with intent output (broadcast)
 * enabled and keystroke output disabled, so each scan arrives exactly once. On non-Zebra devices
 * nothing listens to the DataWedge API broadcasts, so this is a no-op there.
 */
public class ZebraScannerUtil {

  private static final String TAG = ZebraScannerUtil.class.getSimpleName();

  private static final String PROFILE_NAME = "Grocy Android";
  private static final String ACTION_BARCODE_DATA = "xyz.zedler.patrick.grocy.ZEBRA_BARCODE_DATA";

  private static final String DW_ACTION = "com.symbol.datawedge.api.ACTION";
  private static final String DW_EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE";
  private static final String DW_EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";

  private static final String DW_EXTRA_DATA_STRING = "com.symbol.datawedge.data_string";
  private static final String DW_EXTRA_DECODE_DATA = "com.symbol.datawedge.decode_data";

  private static final String INTENT_DELIVERY_BROADCAST = "2";

  // Barcode formats last written to the DataWedge profile in this process,
  // so the profile is only rewritten when the app setting changes.
  private static Set<String> appliedBarcodeFormats;

  private final Activity activity;
  private final SharedPreferences sharedPreferences;
  private final HardwareScannerUtil.BarcodeListener barcodeListener;

  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String barcode = intent.getStringExtra(DW_EXTRA_DATA_STRING);
      if (barcode == null || barcode.isEmpty()) {
        return;
      }
      Log.i(TAG, "Received barcode: " + barcode);
      barcodeListener.onBarcodeRecognized(barcode, getFirstDecodeData(intent));
    }
  };

  public ZebraScannerUtil(
      Activity activity, HardwareScannerUtil.BarcodeListener barcodeListener
  ) {
    this.activity = activity;
    this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
        activity.getBaseContext()
    );
    this.barcodeListener = barcodeListener;
  }

  public void activate() {
    ContextCompat.registerReceiver(
        activity,
        broadcastReceiver,
        new IntentFilter(ACTION_BARCODE_DATA),
        ContextCompat.RECEIVER_EXPORTED
    );
    configureProfileIfNeeded();
  }

  public void deactivate() {
    activity.unregisterReceiver(broadcastReceiver);
  }

  private void configureProfileIfNeeded() {
    Set<String> barcodeFormats = new HashSet<>(sharedPreferences.getStringSet(
        Constants.SETTINGS.SCANNER.BARCODE_FORMATS,
        Constants.SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
    ));
    synchronized (ZebraScannerUtil.class) {
      if (barcodeFormats.equals(appliedBarcodeFormats)) {
        return;
      }
      appliedBarcodeFormats = barcodeFormats;
    }

    // Ignored by DataWedge if the profile already exists
    activity.sendBroadcast(
        new Intent(DW_ACTION).putExtra(DW_EXTRA_CREATE_PROFILE, PROFILE_NAME)
    );

    // One plug-in per SET_CONFIG intent, as DataWedge < 6.6 can't configure several at once
    sendSetConfig(buildPluginConfig("BARCODE", buildBarcodeParams(barcodeFormats)));

    Bundle intentParams = new Bundle();
    intentParams.putString("intent_output_enabled", "true");
    intentParams.putString("intent_action", ACTION_BARCODE_DATA);
    intentParams.putString("intent_delivery", INTENT_DELIVERY_BROADCAST);
    sendSetConfig(buildPluginConfig("INTENT", intentParams));

    Bundle keystrokeParams = new Bundle();
    keystrokeParams.putString("keystroke_output_enabled", "false");
    sendSetConfig(buildPluginConfig("KEYSTROKE", keystrokeParams));
  }

  private void sendSetConfig(Bundle pluginConfig) {
    Bundle profileConfig = new Bundle();
    profileConfig.putString("PROFILE_NAME", PROFILE_NAME);
    profileConfig.putString("PROFILE_ENABLED", "true");
    profileConfig.putString("CONFIG_MODE", "UPDATE");

    Bundle appConfig = new Bundle();
    appConfig.putString("PACKAGE_NAME", activity.getPackageName());
    appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
    profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});

    profileConfig.putBundle("PLUGIN_CONFIG", pluginConfig);

    activity.sendBroadcast(new Intent(DW_ACTION).putExtra(DW_EXTRA_SET_CONFIG, profileConfig));
  }

  private static Bundle buildPluginConfig(String pluginName, Bundle params) {
    Bundle pluginConfig = new Bundle();
    pluginConfig.putString("PLUGIN_NAME", pluginName);
    // Keep the user's other settings in this plug-in, only change the given params
    pluginConfig.putString("RESET_CONFIG", "false");
    pluginConfig.putBundle("PARAM_LIST", params);
    return pluginConfig;
  }

  private static Bundle buildBarcodeParams(Set<String> enabledFormats) {
    Bundle params = new Bundle();
    params.putString("scanner_input_enabled", "true");
    params.putString("scanner_selection", "auto");

    putDecoder(params, "decoder_code128", enabledFormats, BarcodeFormats.BARCODE_FORMAT_CODE128);
    putDecoder(params, "decoder_code39", enabledFormats, BarcodeFormats.BARCODE_FORMAT_CODE39);
    putDecoder(params, "decoder_code93", enabledFormats, BarcodeFormats.BARCODE_FORMAT_CODE93);
    putDecoder(params, "decoder_codabar", enabledFormats, BarcodeFormats.BARCODE_FORMAT_CODABAR);
    putDecoder(params, "decoder_ean13", enabledFormats, BarcodeFormats.BARCODE_FORMAT_EAN13);
    putDecoder(params, "decoder_ean8", enabledFormats, BarcodeFormats.BARCODE_FORMAT_EAN8);
    putDecoder(params, "decoder_i2of5", enabledFormats, BarcodeFormats.BARCODE_FORMAT_ITF);
    putDecoder(params, "decoder_upca", enabledFormats, BarcodeFormats.BARCODE_FORMAT_UPCA);
    putDecoder(params, "decoder_upce0", enabledFormats, BarcodeFormats.BARCODE_FORMAT_UPCE);
    putDecoder(params, "decoder_qrcode", enabledFormats, BarcodeFormats.BARCODE_FORMAT_QR);
    putDecoder(params, "decoder_pdf417", enabledFormats, BarcodeFormats.BARCODE_FORMAT_PDF417);
    putDecoder(params, "decoder_aztec", enabledFormats, BarcodeFormats.BARCODE_FORMAT_AZTEC);
    putDecoder(params, "decoder_datamatrix", enabledFormats, BarcodeFormats.BARCODE_FORMAT_MATRIX);
    putDecoder(params, "decoder_gs1_databar", enabledFormats, BarcodeFormats.BARCODE_FORMAT_RSS14);
    putDecoder(params, "decoder_gs1_databar_exp", enabledFormats, BarcodeFormats.BARCODE_FORMAT_RSSE);

    // Same as for the camera scanners: product barcodes include their check digit
    params.putString("decoder_upca_report_check_digit", "true");
    params.putString("decoder_upce0_report_check_digit", "true");
    return params;
  }

  private static void putDecoder(
      Bundle params, String decoderParam, Set<String> enabledFormats, String format
  ) {
    params.putString(decoderParam, String.valueOf(enabledFormats.contains(format)));
  }

  private static byte[] getFirstDecodeData(Intent intent) {
    List<?> decodeData = IntentCompat.getSerializableExtra(
        intent, DW_EXTRA_DECODE_DATA, ArrayList.class
    );
    if (decodeData == null || decodeData.isEmpty()) {
      return null;
    }
    Object first = decodeData.get(0);
    return first instanceof byte[] ? (byte[]) first : null;
  }
}
