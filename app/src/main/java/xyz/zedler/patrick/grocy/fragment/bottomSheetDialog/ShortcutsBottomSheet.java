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

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetShortcutsBinding;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.TaskEntryEditFragmentArgs;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.ShortcutUtil;

public class ShortcutsBottomSheet extends BaseBottomSheet {

  private final static String TAG = ShortcutsBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetShortcutsBinding binding;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetShortcutsBinding
        .inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    setSkipCollapsedInPortrait();
    super.onViewCreated(view, savedInstanceState);
    activity = (MainActivity) requireActivity();
    binding.setActivity(activity);
    binding.setSheet(this);

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
      binding.text.setText(R.string.msg_shortcuts_not_supported);
      binding.checkboxContainer.setVisibility(View.GONE);
      binding.save.setVisibility(View.GONE);
      return;
    }

    List<ShortcutInfo> shortcutInfos = ShortcutUtil.getDynamicShortcuts(requireContext());
    for (ShortcutInfo shortcutInfo : shortcutInfos) {
      if (shortcutInfo.getId().equals(ShortcutUtil.STOCK_OVERVIEW)) {
        setCheckBoxChecked(R.id.stock_overview);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.SHOPPING_LIST)) {
        setCheckBoxChecked(R.id.shopping_list);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.ADD_TO_SHOPPING_LIST)) {
        setCheckBoxChecked(R.id.add_to_shopping_list);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.SHOPPING_MODE)) {
        setCheckBoxChecked(R.id.shopping_mode);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.PURCHASE)) {
        setCheckBoxChecked(R.id.purchase);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.CONSUME)) {
        setCheckBoxChecked(R.id.consume);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.TRANSFER)) {
        setCheckBoxChecked(R.id.transfer);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.INVENTORY)) {
        setCheckBoxChecked(R.id.inventory);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.TASKS)) {
        setCheckBoxChecked(R.id.tasks);
      } else if (shortcutInfo.getId().equals(ShortcutUtil.ADD_TASK)) {
        setCheckBoxChecked(R.id.task_add);
      }
    }

    checkLimitReached();
  }

  public void checkLimitReached() {
    int countEnabled = 0;
    for (int i = 0; i <= binding.checkboxContainer.getChildCount(); i++) {
      MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
      if (checkBox == null) {
        continue;
      }
      if (checkBox.isChecked()) {
        countEnabled++;
      }
    }
    for (int i = 0; i <= binding.checkboxContainer.getChildCount(); i++) {
      MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
      if (checkBox == null) {
        continue;
      }
      checkBox.setEnabled(countEnabled < 4 || checkBox.isChecked());
    }
  }

  private void setCheckBoxChecked(@IdRes int id) {
    View view = binding.checkboxContainer.findViewById(id);
    if (view != null) {
      ((MaterialCheckBox) view).setChecked(true);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  public void saveShortcuts() {
    ShortcutManager shortcutManager = activity.getSystemService(ShortcutManager.class);
    List<ShortcutInfo> shortcutInfos = new ArrayList<>();
    Context context = requireContext();
    for (int i = 0; i <= binding.checkboxContainer.getChildCount(); i++) {
      MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
      if (checkBox == null || !checkBox.isChecked()) {
        continue;
      }
      if (checkBox.getId() == R.id.stock_overview) {
        shortcutInfos.add(ShortcutUtil.createShortcutStockOverview(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.shopping_list) {
        shortcutInfos.add(ShortcutUtil.createShortcutShoppingList(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.add_to_shopping_list) {
        Uri uriWithArgs = getUriWithArgs(getString(R.string.deep_link_shoppingListItemEditFragment),
            new ShoppingListItemEditFragmentArgs.Builder(Constants.ACTION.CREATE)
                .build().toBundle()
        );
        shortcutInfos.add(ShortcutUtil.createShortcutAddToShoppingList(
            context, uriWithArgs, checkBox.getText()
        ));
      } else if (checkBox.getId() == R.id.shopping_mode) {
        shortcutInfos.add(ShortcutUtil.createShortcutShoppingMode(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.purchase) {
        shortcutInfos.add(ShortcutUtil.createShortcutPurchase(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.consume) {
        shortcutInfos.add(ShortcutUtil.createShortcutConsume(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.inventory) {
        shortcutInfos.add(ShortcutUtil.createShortcutInventory(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.transfer) {
        shortcutInfos.add(ShortcutUtil.createShortcutTransfer(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.tasks) {
        shortcutInfos.add(ShortcutUtil.createShortcutTasks(context, checkBox.getText()));
      } else if (checkBox.getId() == R.id.task_add) {
        Uri uriWithArgs = getUriWithArgs(getString(R.string.deep_link_taskEntryEditFragment),
            new TaskEntryEditFragmentArgs.Builder(Constants.ACTION.CREATE)
                .build().toBundle()
        );
        shortcutInfos.add(ShortcutUtil.createShortcutTaskAdd(
            context, uriWithArgs, checkBox.getText()
        ));
      }
    }

    shortcutManager.removeAllDynamicShortcuts();
    shortcutManager.setDynamicShortcuts(shortcutInfos);
    activity.getCurrentFragment().updateShortcuts();
    dismiss();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
