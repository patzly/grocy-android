package xyz.zedler.patrick.grocy.fragment;

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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.KeyEvent;
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
import xyz.zedler.patrick.grocy.databinding.FragmentScanInputBinding;
import xyz.zedler.patrick.grocy.scan.ScanInputCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.BarcodeRipple;

public class ScanInputFragment extends BaseFragment
        implements ScanInputCaptureManager.BarcodeListener {

    private final static String TAG = ScanInputFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentScanInputBinding binding;
    private ScanInputCaptureManager capture;
    private BarcodeRipple barcodeRipple;
    private DecoratedBarcodeView barcodeScannerView;

    private boolean isTorchOn;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentScanInputBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();

        // GET PREFERENCES
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        binding.frameBack.setOnClickListener(v -> activity.onBackPressed());

        barcodeRipple = view.findViewById(R.id.ripple_scan);

        barcodeScannerView = binding.barcodeScanInput;
        barcodeScannerView.setTorchOff();
        isTorchOn = false;
        CameraSettings cameraSettings = new CameraSettings();
        cameraSettings.setRequestedCameraId(
                sharedPrefs.getBoolean(Constants.PREF.USE_FRONT_CAM, false) ? 1 : 0
        );
        barcodeScannerView.getBarcodeView().setCameraSettings(cameraSettings);

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);

        capture = new ScanInputCaptureManager(activity, barcodeScannerView, this);
        capture.decode();
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setHideOnScroll(false);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_scan_input,
                animated,
                this::setUpBottomMenu
        );
    }

    public void setUpBottomMenu() {
        MenuItem menuItemTorch;
        menuItemTorch = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
        if(menuItemTorch == null) return;

        if(!hasFlash()) menuItemTorch.setVisible(false);
        if(hasFlash()) menuItemTorch.setOnMenuItemClickListener(item -> {
            switchTorch();
            return true;
        });
    }

    private void switchTorch() {
        MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_toggle_flash);
        if(menuItem == null) return;
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

    private boolean hasFlash() {
        return activity.getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
        );
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        barcodeScannerView.setTorchOff();
        capture.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        if(result.getText().isEmpty()) resumeScan();
        barcodeRipple.pauseAnimation();
        setForPreviousDestination(Constants.ARGUMENT.BARCODE, result.getText());
        activity.navigateUp();
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
