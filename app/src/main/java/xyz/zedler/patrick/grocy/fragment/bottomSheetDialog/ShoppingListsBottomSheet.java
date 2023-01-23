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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragmentDirections;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class ShoppingListsBottomSheet extends BaseBottomSheetDialogFragment
    implements ShoppingListAdapter.ShoppingListAdapterListener {

  private final static String TAG = ShoppingListsBottomSheet.class.getSimpleName();

  private static final String DIALOG_DELETE_SHOWING = "dialog_delete_showing";
  private static final String DIALOG_DELETE_SHOPPING_LIST = "dialog_delete_shopping_list";

  private FragmentBottomsheetListSelectionBinding binding;
  private MainActivity activity;
  private AlertDialog dialogDelete;
  private ShoppingList shoppingListDelete;

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
    ViewUtil.centerText(binding.textListSelectionTitle);

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
        activity.navigateFragment(
            ShoppingListFragmentDirections.actionShoppingListFragmentToShoppingListEditFragment()
        );
      });
    }

    if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_DELETE_SHOWING)) {
      shoppingListDelete = savedInstanceState.getParcelable(DIALOG_DELETE_SHOPPING_LIST);
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showDeleteConfirmationDialog, 1
      );
    }

    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    if (dialogDelete != null) {
      // Else it throws an leak exception because the context is somehow from the activity
      dialogDelete.dismiss();
    }
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    boolean isShowing = dialogDelete != null && dialogDelete.isShowing();
    outState.putBoolean(DIALOG_DELETE_SHOWING, isShowing);
    if (isShowing) {
      outState.putParcelable(DIALOG_DELETE_SHOPPING_LIST, shoppingListDelete);
    }
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
    activity.navigateFragment(
        ShoppingListFragmentDirections
            .actionShoppingListFragmentToShoppingListEditFragment()
            .setShoppingList(shoppingList)
    );
  }

  @Override
  public void onClickDelete(ShoppingList shoppingList) {
    shoppingListDelete = shoppingList;
    showDeleteConfirmationDialog();
  }

  private void showDeleteConfirmationDialog() {
    if (shoppingListDelete == null) {
      return;
    }
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            getString(
                R.string.msg_master_delete, getString(R.string.title_shopping_list),
                shoppingListDelete.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          activity.getCurrentFragment().deleteShoppingList(shoppingListDelete);
          dismiss();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerListSelection.setPadding(
        binding.recyclerListSelection.getPaddingLeft(),
        binding.recyclerListSelection.getPaddingTop(),
        binding.recyclerListSelection.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
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
