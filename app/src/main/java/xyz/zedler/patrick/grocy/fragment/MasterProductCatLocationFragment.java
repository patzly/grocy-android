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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatLocationBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatLocationViewModel;

public class MasterProductCatLocationFragment extends BaseFragment {

  private final static String TAG = MasterProductCatLocationFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductCatLocationBinding binding;
  private MasterProductCatLocationViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatLocationBinding.inflate(
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
    viewModel = new ViewModelProvider(this, new MasterProductCatLocationViewModel
        .MasterProductCatLocationViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatLocationViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage message = (SnackbarMessage) event;
        Snackbar snack = message.getSnackbar(activity, activity.binding.frameMainContainer);
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

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI(savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(R.id.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.END,
        viewModel.isActionEdit() ? R.menu.menu_master_product_edit : R.menu.menu_empty,
        menuItem -> {
          if (menuItem.getItemId() != R.id.action_delete) {
            return false;
          }
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              Constants.ACTION.DELETE
          );
          activity.onBackPressed();
          return true;
        }
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

  public void clearInputFocus() {
    activity.hideKeyboard();
  }

  public void showLocationsBottomSheet() {
    ArrayList<Location> locations = viewModel.getFormData().getLocationsLive().getValue();
    if (locations == null) {
      viewModel.showErrorMessage();
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
    Location location = viewModel.getFormData().getLocationLive().getValue();
    int locationId = location != null ? location.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, locationId);
    activity.showBottomSheet(new LocationsBottomSheet(), bundle);
  }

  public void showStoresBottomSheet() {
    ArrayList<Store> stores = viewModel.getFormData().getStoresLive().getValue();
    if (stores == null) {
      viewModel.showErrorMessage();
      return;
    }
    if (stores != null && !stores.isEmpty() && stores.get(0).getId() != -1) {
      stores.add(0, new Store(-1, getString(R.string.subtitle_none_selected)));
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
    Store store = viewModel.getFormData().getStoreLive().getValue();
    int storeId = store != null ? store.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, storeId);
    activity.showBottomSheet(new StoresBottomSheet(), bundle);
  }

  @Override
  public void selectLocation(Location location) {
    viewModel.getFormData().getLocationLive().setValue(location);
  }

  @Override
  public void selectStore(Store store) {
    viewModel.getFormData().getStoreLive().setValue(store);
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
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
