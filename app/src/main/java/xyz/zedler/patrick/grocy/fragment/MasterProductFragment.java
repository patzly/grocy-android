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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductViewModel;

public class MasterProductFragment extends BaseFragment {

    private final static String TAG = MasterProductFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentMasterProductBinding binding;
    private MasterProductViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterProductBinding.inflate(
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
        MasterProductFragmentArgs args = MasterProductFragmentArgs
                .fromBundle(requireArguments());
        viewModel = new ViewModelProvider(this, new MasterProductViewModel
                .MasterProductViewModelFactory(activity.getApplication(), args)
        ).get(MasterProductViewModel.class);
        binding.setActivity(activity);
        binding.setFormData(viewModel.getFormData());
        binding.setViewModel(viewModel);
        binding.setFragment(this);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        binding.categoryOptional.setOnClickListener(v -> navigate(MasterProductFragmentDirections
                .actionMasterProductFragmentToMasterProductCatOptionalFragment(args.getAction())
                .setProduct(viewModel.getFilledProduct())));
        binding.categoryLocation.setOnClickListener(v -> navigate(MasterProductFragmentDirections
                .actionMasterProductFragmentToMasterProductCatLocationFragment(args.getAction())
                .setProduct(viewModel.getFilledProduct())));
        binding.categoryDueDate.setOnClickListener(v -> navigate(MasterProductFragmentDirections
                .actionMasterProductFragmentToMasterProductCatDueDateFragment(args.getAction())
                .setProduct(viewModel.getFilledProduct())));
        binding.categoryAmount.setOnClickListener(v -> navigate(MasterProductFragmentDirections
                .actionMasterProductFragmentToMasterProductCatAmountFragment(args.getAction())
                .setProduct(viewModel.getFilledProduct())));
        binding.categoryQuantityUnit.setOnClickListener(v -> navigate(MasterProductFragmentDirections
                .actionMasterProductFragmentToMasterProductCatQuantityUnitFragment(args.getAction())
                .setProduct(viewModel.getFilledProduct())));
        binding.categoryBarcodes.setOnClickListener(v -> activity.showMessage(R.string.msg_not_implemented_yet));
        binding.categoryQuConversions.setOnClickListener(v -> activity.showMessage(R.string.msg_not_implemented_yet));
        binding.categoryPicture.setOnClickListener(v -> activity.showMessage(R.string.msg_not_implemented_yet));

        Product product = (Product) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT);
        if(product != null) {
            viewModel.setCurrentProduct(product);
            removeForThisDestination(Constants.ARGUMENT.PRODUCT);
        }

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

        String action = (String) getFromThisDestinationNow(Constants.ARGUMENT.ACTION);
        if(action != null) {
            removeForThisDestination(Constants.ARGUMENT.ACTION);
            new Handler().postDelayed(() -> {
                if(!viewModel.getFormData().isWholeFormValid()) {
                    viewModel.showMessage(getString(R.string.error_missing_information));
                    return;
                }
                activity.navigateUp();
            }, 500);
        }

        viewModel.getIsOnlineLive().observe(getViewLifecycleOwner(), isOnline -> {
            //if(isOnline ) viewModel.downloadData();
        });

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        updateUI(savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.END,
                viewModel.isActionEdit() ? R.menu.menu_shopping_list_item_edit : R.menu.menu_empty,
                animated,
                () -> {}
        );
        activity.updateFab(
                R.drawable.ic_round_backup,
                R.string.action_save,
                Constants.FAB.TAG.SAVE,
                animated,
                () -> viewModel.saveItem()
        );
    }

    public void clearInputFocus() {
        activity.hideKeyboard();
        binding.textInputName.clearFocus();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
