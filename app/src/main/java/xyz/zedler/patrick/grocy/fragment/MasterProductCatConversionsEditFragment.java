/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.snackbar.Snackbar;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatConversionsEditBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatConversionsEditViewModel;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatConversionsEditViewModel.MasterProductCatConversionsEditViewModelFactory;

public class MasterProductCatConversionsEditFragment extends BaseFragment {

  private final static String TAG = MasterProductCatConversionsEditFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductCatConversionsEditBinding binding;
  private MasterProductCatConversionsEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
    binding = FragmentMasterProductCatConversionsEditBinding.inflate(inflater, group, false);
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
    MasterProductCatConversionsEditFragmentArgs args = MasterProductCatConversionsEditFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatConversionsEditViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatConversionsEditViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFormData(viewModel.getFormData());
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage message = (SnackbarMessage) event;
        Snackbar snack = message.getSnackbar(activity, activity.binding.coordinatorMain);
        activity.showSnackbar(snack);
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
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
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getFormData().getQuantityUnitFromErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textQuantityUnitFrom.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );
    viewModel.getFormData().getQuantityUnitToErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textQuantityUnitTo.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
      InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
          InfoFullscreen.ERROR_OFFLINE,
          () -> updateConnectivity(true)
      ) : null;
      viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
    });

    ColorRoles roles = ResUtil.getHarmonizedRoles(activity, R.color.blue);
    binding.textInputFactor.setHelperTextColor(ColorStateList.valueOf(roles.getAccent()));

    // necessary because else getValue() doesn't give current value (?)
    viewModel.getFormData().getQuantityUnitsLive().observe(getViewLifecycleOwner(), qUs -> {
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isActionEdit()
            ? R.menu.menu_master_product_edit
            : R.menu.menu_empty,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        true,
        () -> viewModel.saveItem()
    );
  }

  public void clearAmountFieldAndFocusIt() {
    binding.editTextFactor.setText("");
    activity.showKeyboard(binding.editTextFactor);
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.textInputFactor.clearFocus();
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit, Bundle args) {
    if (args.getBoolean(MasterProductCatConversionsEditViewModel.QUANTITY_UNIT_IS_FROM)) {
      if (quantityUnit != null && quantityUnit.getId() == -1) {
        viewModel.getFormData().getQuantityUnitFromLive().setValue(null);
      } else {
        viewModel.getFormData().getQuantityUnitFromLive().setValue(quantityUnit);
      }
    } else {
      if (quantityUnit != null && quantityUnit.getId() == -1) {
        viewModel.getFormData().getQuantityUnitToLive().setValue(null);
      } else {
        viewModel.getFormData().getQuantityUnitToLive().setValue(quantityUnit);
      }
    }
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_delete) {
      ViewUtil.startIcon(item);
      viewModel.deleteItem();
      return true;
    }
    return false;
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
    if (systemBarBehavior != null) {
      systemBarBehavior.refresh();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
