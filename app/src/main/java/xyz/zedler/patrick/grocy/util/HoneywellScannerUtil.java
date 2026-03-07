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
import androidx.preference.PreferenceManager;

import java.util.Set;

import xyz.zedler.patrick.grocy.Constants;

public class HoneywellScannerUtil {
    private static final String TAG = HoneywellScannerUtil.class.getSimpleName();

    private static final String ACTION_BARCODE_DATA = "xyz.zedler.patrick.grocy.BARCODE_DATA";

    private static final String ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER";
    private static final String ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER";

    private static final String EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE";
    private static final String EXTRA_PROFILE_VARIANT_DEFAULT = "DEFAULT";

    private static final String EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES";

    private static final String EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER";
    private static final String EXTRA_SCANNER_VARIANT_INTERNAL = "dcs.scanner.imager";
    private static final String EXTRA_SCANNER_VARIANT_EXTERNAL = "dcs.scanner.ring";

    private static final String PROPERTY_DATA_PROCESSOR_INTENT = "DPR_DATA_INTENT";
    private static final String PROPERTY_DATA_PROCESSOR_INTENT_ACTION = "DPR_DATA_INTENT_ACTION";
    private static final String PROPERTY_CODE_128_ENABLED = "DEC_CODE128_ENABLED";
    private static final String PROPERTY_CODE_39_ENABLED = "DEC_CODE39_ENABLED";
    private static final String PROPERTY_CODE_39_CHECK_DIGIT_MODE = "DEC_CODE39_CHECK_DIGIT_MODE";
    private static final String PROPERTY_CODE_39_CHECK_DIGIT_MODE_VARIANT_CHECK = "check";
    private static final String PROPERTY_CODE_39_CHECK_DIGIT_MODE_VARIANT_CHECK_AND_STRIP = "checkAndStrip";
    private static final String PROPERTY_CODE_39_CHECK_DIGIT_MODE_VARIANT_NO_CHECK = "noCheck";
    private static final String PROPERTY_CODE_93_ENABLED = "DEC_CODE93_ENABLED";
    private static final String PROPERTY_CODABAR_ENABLED = "DEC_CODABAR_ENABLED";
    private static final String PROPERTY_CODABAR_CHECK_DIGIT_MODE = "DEC_CODABAR_CHECK_DIGIT_MODE";
    private static final String PROPERTY_CODABAR_CHECK_DIGIT_MODE_VARIANT_CHECK = "check";
    private static final String PROPERTY_CODABAR_CHECK_DIGIT_MODE_VARIANT_CHECK_AND_STRIP = "checkAndStrip";
    private static final String PROPERTY_CODABAR_CHECK_DIGIT_MODE_VARIANT_NO_CHECK = "noCheck";
    private static final String PROPERTY_EAN_13_ENABLED = "DEC_EAN13_ENABLED";
    private static final String PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_EAN13_CHECK_DIGIT_TRANSMIT";
    private static final String PROPERTY_EAN_8_ENABLED = "DEC_EAN8_ENABLED";
    private static final String PROPERTY_EAN_8_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_EAN8_CHECK_DIGIT_TRANSMIT";
    private static final String PROPERTY_UPC_A_ENABLED = "DEC_UPCA_ENABLE"; // note: missing 'd' is according to reference impl!
    private static final String PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_UPCA_CHECK_DIGIT_TRANSMIT";
    private static final String PROPERTY_UPC_E_ENABLED = "DEC_UPCE0_ENABLED"; // note: there is also UPCE1/UPCE_E1
    private static final String PROPERTY_UPC_E_CHECK_DIGIT_TRANSMIT_ENABLED = "DEC_UPCE_CHECK_DIGIT_TRANSMIT";
    private static final String PROPERTY_QR_ENABLED = "DEC_QR_ENABLED";
    private static final String PROPERTY_PDF_417_ENABLED = "DEC_PDF417_ENABLED";
    private static final String PROPERTY_AZTEC_ENABLED = "DEC_AZTEC_ENABLED";
    private static final String PROPERTY_DATAMATRIX_ENABLED = "DEC_DATAMATRIX_ENABLED";
    private static final String PROPERTY_RSS_ENABLED = "DEC_RSS_14_ENABLED";
    private static final String PROPERTY_RSS_EXPANDED_ENABLED = "DEC_RSS_EXPANDED_ENABLED";


    private final Activity activity;
    private final SharedPreferences sharedPreferences;
    private final BarcodeListener barcodeListener;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String barcode = intent.getStringExtra("data");
            byte[] barcodeBytes = intent.getByteArrayExtra("dataBytes");
            Log.i(TAG, "Received barcode: " + barcode);
            HoneywellScannerUtil.this.barcodeListener.onBarcodeRecognized(barcode, barcodeBytes);
        }
    };

    public HoneywellScannerUtil(Activity activity, BarcodeListener barcodeListener) {
        this.activity = activity;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
        this.barcodeListener = barcodeListener;
    }

    public void activate() {
        ContextCompat.registerReceiver(this.activity, broadcastReceiver, new IntentFilter(ACTION_BARCODE_DATA), ContextCompat.RECEIVER_EXPORTED);
        claimScanner();
    }

    public void deactivate() {
        this.activity.unregisterReceiver(broadcastReceiver);
        releaseScanner();
    }

    private void claimScanner() {
        this.activity.sendBroadcast(
                new Intent(ACTION_CLAIM_SCANNER)
                    .putExtra(EXTRA_PROPERTIES, buildClaimProperties())
                    .putExtra(EXTRA_SCANNER, EXTRA_SCANNER_VARIANT_INTERNAL)
                    .putExtra(EXTRA_PROFILE, EXTRA_PROFILE_VARIANT_DEFAULT)
        );
    }

    private Bundle buildClaimProperties() {
        Bundle properties = new Bundle();

        properties.putBoolean(PROPERTY_DATA_PROCESSOR_INTENT, true);
        properties.putString(PROPERTY_DATA_PROCESSOR_INTENT_ACTION, ACTION_BARCODE_DATA);

        Set<String> enabledBarcodeFormatsSet = this.sharedPreferences.getStringSet(
                Constants.SETTINGS.SCANNER.BARCODE_FORMATS,
                Constants.SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
        );

        for (String barcodeFormat : enabledBarcodeFormatsSet) {
            switch (barcodeFormat) {
                case Constants.BarcodeFormats.BARCODE_FORMAT_CODE128:
                    properties.putBoolean(PROPERTY_CODE_128_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_CODE39:
                    properties.putBoolean(PROPERTY_CODE_39_ENABLED, true);
                    properties.putString(PROPERTY_CODE_39_CHECK_DIGIT_MODE, PROPERTY_CODE_39_CHECK_DIGIT_MODE_VARIANT_CHECK);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_CODE93:
                    properties.putBoolean(PROPERTY_CODE_93_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_EAN13:
                    properties.putBoolean(PROPERTY_EAN_13_ENABLED, true);
                    properties.putBoolean(PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_EAN8:
                    properties.putBoolean(PROPERTY_EAN_8_ENABLED, true);
                    properties.putBoolean(PROPERTY_EAN_8_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_ITF:
                    // not supported
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_UPCA:
                    properties.putBoolean(PROPERTY_UPC_A_ENABLED, true);
                    properties.putBoolean(PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_UPCE:
                    properties.putBoolean(PROPERTY_UPC_E_ENABLED, true);
                    properties.putBoolean(PROPERTY_UPC_E_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_QR:
                    properties.putBoolean(PROPERTY_QR_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_PDF417:
                    properties.putBoolean(PROPERTY_PDF_417_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_MATRIX:
                    properties.putBoolean(PROPERTY_DATAMATRIX_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_RSS14:
                    properties.putBoolean(PROPERTY_RSS_ENABLED, true);
                    break;
                case Constants.BarcodeFormats.BARCODE_FORMAT_RSSE:
                    properties.putBoolean(PROPERTY_RSS_EXPANDED_ENABLED, true);
                    break;
            }
        }

        return properties;
    }

    private void releaseScanner() {
        this.activity.sendBroadcast(new Intent(ACTION_RELEASE_SCANNER));
    }

    public interface BarcodeListener {
        void onBarcodeRecognized(String barcode, byte[] barcodeBytes);
    }
}
