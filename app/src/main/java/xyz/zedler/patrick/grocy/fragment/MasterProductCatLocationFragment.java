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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatLocationBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatLocationViewModel;

public class MasterProductCatLocationFragment extends BaseFragment {

  private final static String TAG = MasterProductCatLocationFragment.class.getSimpleName();
  private final static String IS_CONSUME_LOCATION = "is_consume_location";

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
    MasterProductCatLocationFragmentArgs args = MasterProductCatLocationFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatLocationViewModel
        .MasterProductCatLocationViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatLocationViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeMasterProductSimple);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> {
      onBackPressed();
      activity.navUtil.navigateUp();
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    Object newLocationId = getFromThisDestinationNow(ARGUMENT.OBJECT_ID);
    if (newLocationId != null) {  // if user created a new location and navigates back to this fragment this is the new locationId
      removeForThisDestination(ARGUMENT.OBJECT_ID);
      String idForValue = (String) getFromThisDestinationNow(ARGUMENT.OBJECT_NAME);
      viewModel.setQueueEmptyAction(() -> {
        Bundle bundle = new Bundle();
        bundle.putBoolean(
            IS_CONSUME_LOCATION,
            idForValue != null && idForValue.equals(IS_CONSUME_LOCATION)
        );
        List<Location> locations = viewModel.getFormData().getLocationsLive().getValue();
        if (locations == null) return;
        Location location = Location.getFromId(locations, (Integer) newLocationId);
        selectLocation(location, bundle);
      });
    }

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFormData().getLocationErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textLocation.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnSurfaceVariant)
        )
    );

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
            : R.menu.menu_master_product_create,
        menuItem -> {
          if (menuItem.getItemId() == R.id.action_delete) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                Constants.ACTION.DELETE
            );
            activity.onBackPressed();
            return true;
          }
          if (menuItem.getItemId() == R.id.action_save) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                ACTION.SAVE_CLOSE
            );
            activity.onBackPressed();
            return true;
          }
          return false;
        }
    );
    boolean showSaveWithCloseButton = viewModel.isActionEdit() || args.getForceSaveWithClose();
    activity.updateFab(
        showSaveWithCloseButton ? R.drawable.ic_round_save : R.drawable.ic_round_save_as,
        showSaveWithCloseButton ? R.string.action_save : R.string.action_save_not_close,
        showSaveWithCloseButton ? Constants.FAB.TAG.SAVE : Constants.FAB.TAG.SAVE_NOT_CLOSE,
        savedInstanceState == null,
        () -> {
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              showSaveWithCloseButton ? ACTION.SAVE_CLOSE : ACTION.SAVE_NOT_CLOSE
          );
          activity.onBackPressed();
        }
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
  }

  public void showLocationsBottomSheet(boolean consumeLocation) {
    List<Location> locations = viewModel.getFormData().getLocationsLive().getValue();
    if (locations == null) {
      viewModel.showNetworkErrorMessage(null);
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, new ArrayList<>(locations));
    Location location;
    if (consumeLocation) {
      bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
      location = viewModel.getFormData().getLocationConsumeLive().getValue();
    } else {
      location = viewModel.getFormData().getLocationLive().getValue();
    }
    bundle.putBoolean(ARGUMENT.DISPLAY_NEW_OPTION, true);
    int locationId = location != null ? location.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, locationId);
    bundle.putBoolean(IS_CONSUME_LOCATION, consumeLocation);
    activity.showBottomSheet(new LocationsBottomSheet(), bundle);
  }

  public void showStoresBottomSheet() {
    List<Store> stores = viewModel.getFormData().getStoresLive().getValue();
    if (stores == null) {
      viewModel.showNetworkErrorMessage(null);
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, new ArrayList<>(stores));
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    Store store = viewModel.getFormData().getStoreLive().getValue();
    int storeId = store != null ? store.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, storeId);
    activity.showBottomSheet(new StoresBottomSheet(), bundle);
  }

  @Override
  public void selectLocation(Location location, Bundle args) {
    if (args.getBoolean(IS_CONSUME_LOCATION, false)) {
      viewModel.getFormData().getLocationConsumeLive().setValue(location != null
          && location.getId() != -1 ? location : null);
      viewModel.getFormData().disableMoveOnOpenIfNecessary();
    } else {
      viewModel.getFormData().getLocationLive().setValue(location);
    }
  }

  @Override
  public void createLocation(Bundle args) {
    activity.navUtil.navigateFragment(MasterProductCatLocationFragmentDirections
        .actionMasterProductCatLocationFragmentToMasterLocationFragment()
        .setIdForReturnValue(args.getBoolean(IS_CONSUME_LOCATION, false)
            ? IS_CONSUME_LOCATION : null));
  }

  @Override
  public void selectStore(Store store) {
    viewModel.getFormData().getStoreLive().setValue(
        store == null || store.getId() == -1 ? null : store
    );
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
    viewModel.downloadData(false);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
