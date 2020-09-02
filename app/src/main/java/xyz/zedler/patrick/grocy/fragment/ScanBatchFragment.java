package xyz.zedler.patrick.grocy.fragment;

import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentScanBatchBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ExitScanBatchBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.scan.ScanBatchCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.BarcodeRipple;

public class ScanBatchFragment extends BaseFragment
        implements ScanBatchCaptureManager.BarcodeListener {

    private final static String TAG = Constants.UI.BATCH_SCAN;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private FragmentScanBatchBinding binding;
    private ScanBatchCaptureManager capture;
    private BarcodeRipple barcodeRipple;
    private DecoratedBarcodeView barcodeScannerView;

    private boolean isTorchOn = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentScanBatchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // GET PREFERENCES
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        barcodeRipple = view.findViewById(R.id.ripple_scan);

        barcodeScannerView = binding.barcodeScanBatch;
        barcodeScannerView.setTorchOff();
        //barcodeScannerView.setTorchListener(this);
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setRequestedCameraId(
                sharedPrefs.getBoolean(Constants.PREF.USE_FRONT_CAM, false) ? 1 : 0
        );
        barcodeScannerView.getBarcodeView().setCameraSettings(cameraSettings);

        capture = new ScanBatchCaptureManager(activity, barcodeScannerView, this);
        capture.decode();

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setHideOnScroll(false);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_scan_batch,
                animated,
                this::setUpBottomMenu
        );
    }

    public void setUpBottomMenu() {
        MenuItem menuItemTorch, menuItemConfig;
        menuItemTorch = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
        menuItemConfig = activity.getBottomMenu().findItem(R.id.action_open_config);
        if(menuItemTorch == null || menuItemConfig == null) return;

        menuItemTorch.setOnMenuItemClickListener(item -> {
            switchTorch();
            return true;
        });
    }

    private void switchTorch() {
        MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
        if(isTorchOn) {
            barcodeScannerView.setTorchOff();
            menuItem.setIcon(R.drawable.ic_round_flash_off_to_on);
            if(menuItem.getIcon() instanceof Animatable) ((Animatable) menuItem.getIcon()).start();
            isTorchOn = false;
        } else {
            barcodeScannerView.setTorchOn();
            menuItem.setIcon(R.drawable.ic_round_flash_on_to_off);
            if(menuItem.getIcon() instanceof Animatable) ((Animatable) menuItem.getIcon()).start();
            isTorchOn = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseScan();
    }

    @Override
    public boolean onBackPressed() {
        activity.showBottomSheet(new ExitScanBatchBottomSheetDialogFragment(), null);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {

    }

    @Override
    public void pauseScan() {
        barcodeRipple.pauseAnimation();
        capture.onPause();
    }

    @Override
    public void resumeScan() {
        barcodeRipple.resumeAnimation();
        capture.onResume();
    }
}
