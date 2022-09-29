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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragmentDirections;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil.TouchProgressBarUtil;

public class ShoppingListsBottomSheet extends BaseBottomSheetDialogFragment
    implements ShoppingListAdapter.ShoppingListAdapterListener {

  private final static String TAG = ShoppingListsBottomSheet.class.getSimpleName();

  private FragmentBottomsheetListSelectionBinding binding;
  private MainActivity activity;
  private TouchProgressBarUtil touchProgressBarUtil;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetListSelectionBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    boolean multipleListsFeature = sharedPrefs.getBoolean(
        Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true
    );

    MutableLiveData<Integer> selectedIdLive = activity.getCurrentFragment()
        .getSelectedShoppingListIdLive();
    int selectedId = getArguments() != null
        ? getArguments().getInt(ARGUMENT.SELECTED_ID, -1) : -1;
    if (selectedIdLive == null && selectedId == -1) {
      dismiss();
      return binding.getRoot();
    }

    binding.textListSelectionTitle.setText(activity.getString(R.string.property_shopping_lists));
    if (!UiUtil.isFullWidth(activity)) {
      binding.textListSelectionTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recyclerListSelection.setItemAnimator(new DefaultItemAnimator());
    binding.recyclerListSelection.setAdapter(new ShoppingPlaceholderAdapter());
    ViewUtil.setOnlyOverScrollStretchEnabled(binding.recyclerListSelection);

    ShoppingListRepository repository = new ShoppingListRepository(activity.getApplication());
    repository.getShoppingListsLive().observe(getViewLifecycleOwner(), shoppingLists -> {
      if (shoppingLists == null) {
        return;
      }
      if (binding.recyclerListSelection.getAdapter() == null
          || !(binding.recyclerListSelection.getAdapter() instanceof ShoppingListAdapter)
      ) {
        binding.recyclerListSelection.setAdapter(new ShoppingListAdapter(
            shoppingLists,
            selectedIdLive != null && selectedIdLive.getValue() != null
                ? selectedIdLive.getValue() : selectedId,
            this,
            activity.getCurrentFragment() instanceof ShoppingListFragment
                && activity.isOnline()
        ));
      } else {
        ((ShoppingListAdapter) binding.recyclerListSelection.getAdapter()).updateData(
            shoppingLists,
            selectedIdLive != null && selectedIdLive.getValue() != null
                ? selectedIdLive.getValue() : selectedId
        );
      }
    });

    if (selectedIdLive != null) {
      selectedIdLive.observe(getViewLifecycleOwner(), selectedIdNew -> {
        if (binding.recyclerListSelection.getAdapter() == null
            || !(binding.recyclerListSelection.getAdapter() instanceof ShoppingListAdapter)
        ) {
          return;
        }
        ((ShoppingListAdapter) binding.recyclerListSelection.getAdapter()).updateSelectedId(
            selectedIdNew
        );
      });
    }

    if (activity.isOnline() && multipleListsFeature
        && activity.getCurrentFragment() instanceof ShoppingListFragment
    ) {
      binding.buttonListSelectionNew.setVisibility(View.VISIBLE);
      binding.buttonListSelectionNew.setOnClickListener(v -> {
        dismiss();
        navigate(ShoppingListFragmentDirections
            .actionShoppingListFragmentToShoppingListEditFragment());
      });
    }

    touchProgressBarUtil = new TouchProgressBarUtil(
        binding.progressConfirmation,
        null,
        object -> activity.getCurrentFragment().deleteShoppingList((ShoppingList) object)
    );

    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    if (touchProgressBarUtil != null) {
      touchProgressBarUtil.onDestroy();
      touchProgressBarUtil = null;
    }
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(ShoppingList shoppingList) {
    activity.getCurrentFragment().selectShoppingList(shoppingList);
    dismiss();
  }

  @Override
  public void onClickEdit(ShoppingList shoppingList) {
    if (!activity.isOnline()) {
      showMessage(R.string.error_offline);
      return;
    }
    dismiss();
    navigate(ShoppingListFragmentDirections
        .actionShoppingListFragmentToShoppingListEditFragment()
        .setShoppingList(shoppingList));
  }

  @Override
  public void onTouchDelete(View view, MotionEvent event, ShoppingList shoppingList) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (!activity.isOnline()) {
        showMessage(R.string.error_offline);
        return;
      }
      touchProgressBarUtil.showAndStartProgress(view, shoppingList);
    } else if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL) {
      if (!activity.isOnline()) {
        return;
      }
      touchProgressBarUtil.hideAndStopProgress();
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerListSelection.setPadding(
        binding.recyclerListSelection.getPaddingLeft(),
        binding.recyclerListSelection.getPaddingTop(),
        binding.recyclerListSelection.getPaddingRight(),
        binding.recyclerListSelection.getPaddingBottom() + bottom
    );
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    activity.getCurrentFragment().onBottomSheetDismissed();
    super.onDismiss(dialog);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
