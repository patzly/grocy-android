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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.ProductBarcodeAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatBarcodesBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatBarcodesViewModel;

public class MasterProductCatBarcodesFragment extends BaseFragment implements ProductBarcodeAdapter.ProductBarcodeAdapterListener {

    private final static String TAG = MasterProductCatBarcodesFragment.class.getSimpleName();

    private MainActivity activity;
    private ClickUtil clickUtil;
    private FragmentMasterProductCatBarcodesBinding binding;
    private MasterProductCatBarcodesViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterProductCatBarcodesBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(infoFullscreenHelper != null) {
            infoFullscreenHelper.destroyInstance();
            infoFullscreenHelper = null;
        }
        if(binding != null) {
            binding.recycler.animate().cancel();
            binding = null;
        }
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        clickUtil = new ClickUtil();

        MasterProductFragmentArgs args = MasterProductFragmentArgs
                .fromBundle(requireArguments());
        viewModel = new ViewModelProvider(this, new MasterProductCatBarcodesViewModel
                .MasterProductCatBarcodesViewModelFactory(activity.getApplication(), args)
        ).get(MasterProductCatBarcodesViewModel.class);

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
            } else if(event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
            }
        });

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);
        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
            if(!isLoading) viewModel.setCurrentQueueLoading(null);
        });

        binding.recycler.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        binding.recycler.setItemAnimator(new DefaultItemAnimator());
        binding.recycler.setAdapter(new MasterPlaceholderAdapter());

        viewModel.getProductBarcodesLive().observe(getViewLifecycleOwner(), barcodes -> {
            if(barcodes == null) return;
            if(barcodes.isEmpty()) {
                InfoFullscreen info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_PRODUCT_BARCODES);
                viewModel.getInfoFullscreenLive().setValue(info);
            } else {
                viewModel.getInfoFullscreenLive().setValue(null);
            }
            if(binding.recycler.getAdapter() instanceof ProductBarcodeAdapter) {
                ((ProductBarcodeAdapter) binding.recycler.getAdapter()).updateData(barcodes);
            } else {
                binding.recycler.setAdapter(new ProductBarcodeAdapter(
                        barcodes,
                        this
                ));
            }
        });

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        updateUI(savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.getScrollBehavior().setUpScroll(R.id.scroll);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.END,
                viewModel.isActionEdit() ? R.menu.menu_master_product_edit : R.menu.menu_empty,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_backup,
                R.string.action_save,
                Constants.FAB.TAG.SAVE,
                animated,
                () -> {
                    setForDestination(
                            R.id.masterProductFragment,
                            Constants.ARGUMENT.ACTION,
                            Constants.ACTION.SAVE
                    );
                    activity.onBackPressed();
                }
        );
    }

    @Override
    public void onItemRowClicked(ProductBarcode productBarcode) {
        if(clickUtil.isDisabled()) return;
    }

    public void clearInputFocus() {
        activity.hideKeyboard();
    }

    public void setUpBottomMenu() {
        MenuItem delete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(delete != null) {
            delete.setOnMenuItemClickListener(item -> {
                setForDestination(
                        R.id.masterProductFragment,
                        Constants.ARGUMENT.ACTION,
                        Constants.ACTION.DELETE
                );
                activity.onBackPressed();
                return true;
            });
        }
    }

    @Override
    public boolean onBackPressed() {
        setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.PRODUCT,
                viewModel.getFilledProduct()
        );
        return false;
    }

    @Override
    public void updateConnectivity(boolean isOnline) {
        if(!isOnline == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!isOnline);
        if(isOnline) viewModel.downloadData();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
