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

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentScanExternalInputBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.viewmodel.ScanExternalInputViewModel;

public class ScanExternalInputFragment extends BaseFragment {

    private final static String TAG = ScanExternalInputFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentScanExternalInputBinding binding;
    private ScanExternalInputViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;
    private String barcode = "";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentScanExternalInputBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        viewModel = new ViewModelProvider(this).get(ScanExternalInputViewModel.class);
        binding.setActivity(activity);
        binding.setViewModel(viewModel);
        binding.setFragment(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
            if(event.getType() == Event.SNACKBAR_MESSAGE) {
                SnackbarMessage message = (SnackbarMessage) event;
                Snackbar snack = message.getSnackbar(activity, activity.binding.frameMainContainer);
                activity.showSnackbar(snack);
            } else if(event.getType() == Event.NAVIGATE_UP) {
                activity.navigateUp();
            } else if(event.getType() == Event.SET_SHOPPING_LIST_ID) {
                int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
                setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
            } else if(event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
            }
        });

        infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
            if(!isLoading) viewModel.setCurrentQueueLoading(null);
        });

        updateUI(savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_empty,
                animated,
                () -> {}
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_1:
                barcode += "1";
                break;
            case KeyEvent.KEYCODE_2:
                barcode += "2";
                break;
            case KeyEvent.KEYCODE_3:
                barcode += "3";
                break;
            case KeyEvent.KEYCODE_4:
                barcode += "4";
                break;
            case KeyEvent.KEYCODE_5:
                barcode += "5";
                break;
            case KeyEvent.KEYCODE_6:
                barcode += "6";
                break;
            case KeyEvent.KEYCODE_7:
                barcode += "7";
                break;
            case KeyEvent.KEYCODE_8:
                barcode += "8";
                break;
            case KeyEvent.KEYCODE_9:
                barcode += "9";
                break;
            case KeyEvent.KEYCODE_0:
                barcode += "0";
                break;
            case KeyEvent.KEYCODE_TAB:
                if(barcode.isEmpty()) return true;
                setForPreviousDestination(Constants.ARGUMENT.BARCODE, barcode);
                activity.navigateUp();
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    public void navigateToScannerSettings() {
        navigateDeepLink(
                getString(R.string.deep_link_settingsFragment),
                new SettingsFragmentArgs.Builder()
                        .setShowCategory(Constants.SETTINGS.SCANNER.class.getSimpleName())
                        .build().toBundle()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
