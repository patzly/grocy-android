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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ChooseProductAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentPendingPurchasesBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.FAB.POSITION;
import xyz.zedler.patrick.grocy.viewmodel.PendingPurchasesViewModel;

public class PendingPurchasesFragment extends BaseFragment
    implements ChooseProductAdapter.ChooseProductAdapterListener {

  private final static String TAG = PendingPurchasesFragment.class.getSimpleName();

  private MainActivity activity;
  private ClickUtil clickUtil;
  private FragmentPendingPurchasesBinding binding;
  private PendingPurchasesViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentPendingPurchasesBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (binding != null) {
      binding.recycler.animate().cancel();
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    clickUtil = new ClickUtil();
    viewModel = new ViewModelProvider(this).get(PendingPurchasesViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());

    binding.setFragment(this);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());
    binding.setClickUtil(clickUtil);

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
      binding.swipe.setRefreshing(state);
      if (!state) {
        viewModel.setCurrentQueueLoading(null);
      }
    });
    binding.swipe.setOnRefreshListener(() -> viewModel.downloadDataForceUpdate());
    binding.swipe.setProgressBackgroundColorSchemeColor(
        ContextCompat.getColor(activity, R.color.surface)
    );
    binding.swipe.setColorSchemeColors(ContextCompat.getColor(activity, R.color.secondary));

    viewModel.getDisplayedItemsLive().observe(getViewLifecycleOwner(), products -> {
      if (products == null) {
        return;
      }
      if (binding.recycler.getAdapter() instanceof ChooseProductAdapter) {
        ((ChooseProductAdapter) binding.recycler.getAdapter()).updateData(products);
      } else {
        binding.recycler.setAdapter(new ChooseProductAdapter(
            products, this
        ));
        binding.recycler.scheduleLayoutAnimation();
      }
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage msg = (SnackbarMessage) event;
        Snackbar snackbar = msg.getSnackbar(activity, activity.binding.frameMainContainer);
        activity.showSnackbar(snackbar);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.FOCUS_INVALID_VIEWS) {

      }
    });

    // INITIALIZE VIEWS

    binding.back.setOnClickListener(v -> activity.onBackPressed());

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setItemAnimator(new DefaultItemAnimator());
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI
    activity.getScrollBehavior().setUpScroll(binding.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        POSITION.GONE,
        R.menu.menu_empty,
        (OnMenuItemClickListener) null
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
  }

  @Override
  public void onItemRowClicked(Product product) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (product instanceof PendingProduct) {
      setForPreviousDestination(ARGUMENT.PENDING_PRODUCT_ID, product.getId());
    } else {
      setForPreviousDestination(ARGUMENT.PRODUCT_ID, product.getId());
    }
    String barcode = ChooseProductFragmentArgs.fromBundle(requireArguments()).getBarcode();
    setForPreviousDestination(ARGUMENT.BARCODE, barcode);
    setForPreviousDestination(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE, true);
    activity.navigateUp();
  }

  @Override
  public boolean onBackPressed() {
    setForPreviousDestination(ARGUMENT.BACK_FROM_CHOOSE_PRODUCT_PAGE, true);
    return false;
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!online);
    viewModel.downloadData();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
