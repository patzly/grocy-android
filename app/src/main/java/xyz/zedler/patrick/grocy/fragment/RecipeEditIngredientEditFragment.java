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

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeEditIngredientEditBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipeEditIngredientEditViewModel;
import xyz.zedler.patrick.grocy.viewmodel.RecipeEditIngredientEditViewModel.RecipeEditIngredientEditViewModelFactory;

public class RecipeEditIngredientEditFragment extends BaseFragment implements EmbeddedFragmentScanner.BarcodeListener {

  private final static String TAG = RecipeEditIngredientEditFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentRecipeEditIngredientEditBinding binding;
  private RecipeEditIngredientEditViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup group, Bundle state) {
    binding = FragmentRecipeEditIngredientEditBinding.inflate(inflater, group, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
            this,
            binding.containerScanner,
            this
    );
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
    RecipeEditIngredientEditFragmentArgs args = RecipeEditIngredientEditFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(
        this,
        new RecipeEditIngredientEditViewModelFactory(activity.getApplication(), args)
    ).get(RecipeEditIngredientEditViewModel.class);
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

    binding.categoryQuantityUnit.setOnClickListener(v -> {
      ArrayList<QuantityUnit> quantityUnits = viewModel.getQuantityUnits();
      Bundle bundle = new Bundle();
      bundle.putParcelableArrayList(
              Constants.ARGUMENT.QUANTITY_UNITS,
              quantityUnits
      );
      QuantityUnit quantityUnit = viewModel.getFormData().getQuantityUnitLive().getValue();
      bundle.putInt(Constants.ARGUMENT.SELECTED_ID, quantityUnit != null ? quantityUnit.getId() : -1);
      activity.showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
    });

    viewModel.getFormData().getOnlyCheckSingleUnitInStockLive().observe(getViewLifecycleOwner(), state -> {
      if (state) return;
      viewModel.setStockQuantityUnit();
    });
    viewModel.getFormData().getQuantityUnitLive().observe(
        getViewLifecycleOwner(), value -> binding.textQuantityUnitLabel.setTextColor(
            ResUtil.getColorAttr(
                activity, value == null ? R.attr.colorError : R.attr.colorOnSurfaceVariant
            )
        )
    );

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage message = (SnackbarMessage) event;
        Snackbar snack = message.getSnackbar(activity, activity.binding.coordinatorMain);
        activity.showSnackbar(snack);
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    if (savedInstanceState == null && args.getAction().equals(ACTION.CREATE)) {
      if (binding.autoCompleteProduct.getText() == null
          || binding.autoCompleteProduct.getText().length() == 0) {
        new Handler().postDelayed(
            () -> activity.showKeyboard(binding.autoCompleteProduct),
            50
        );
      }
    }

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
      InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
          InfoFullscreen.ERROR_OFFLINE,
          () -> updateConnectivity(true)
      ) : null;
      viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
    });

    embeddedFragmentScanner.setScannerVisibilityLive(
            viewModel.getFormData().getScannerVisibilityLive()
    );

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isActionEdit()
            ? R.menu.menu_recipe_edit_edit
            : R.menu.menu_recipe_edit_create,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        savedInstanceState == null,
        () -> {
          if (!viewModel.getFormData().isFormValid()) {
            clearInputFocus();
          } else {
            viewModel.saveEntry();
          }
        }
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.container.requestFocus();
    binding.autoCompleteProduct.clearFocus();
    binding.switchOnlyCheckSingleUnitInStock.clearFocus();
    binding.textInputAmount.clearFocus();
    binding.textInputVariableAmount.clearFocus();
    binding.switchDoNotCheckStockFulfillment.clearFocus();
    binding.textInputIngredientGroup.clearFocus();
    binding.textInputNotes.clearFocus();
  }

  public void clearAmountFieldAndFocusIt() {
    binding.editTextAmount.setText("");
    activity.showKeyboard(binding.editTextAmount);
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);

    clearInputFocus();
    if (product == null) {
      return;
    }
    viewModel.setProduct(product.getId(), null, null, null);
  }

  public void clearFocusAndCheckProductInput() {
    clearInputFocus();
    viewModel.checkProductInput();
  }

  public void clearFocusAndCheckProductInputExternal() {
    clearInputFocus();
    String input = viewModel.getFormData().getProductNameLive().getValue();
    if (input == null || input.isEmpty()) return;
    viewModel.onBarcodeRecognized(viewModel.getFormData().getProductNameLive().getValue());
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_delete) {
      ViewUtil.startIcon(item);
      viewModel.deleteEntry();
      return true;
    } else if (item.getItemId() == R.id.action_clear_form) {
      clearInputFocus();
      viewModel.getFormData().clearForm();
      return true;
    }
    return false;
  }

  public void toggleScannerVisibility() {
    viewModel.getFormData().toggleScannerVisibility();
    if (viewModel.getFormData().isScannerVisible()) {
      clearInputFocus();
    }
  }

  @Override
  public void selectQuantityUnit(QuantityUnit quantityUnit) {
    viewModel.getFormData().setQuantityUnit(quantityUnit);
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    clearInputFocus();
    viewModel.getFormData().toggleScannerVisibility();
    viewModel.onBarcodeRecognized(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
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
