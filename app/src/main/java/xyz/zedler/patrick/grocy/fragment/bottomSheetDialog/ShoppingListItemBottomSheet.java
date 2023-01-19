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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetShoppingListItemBinding;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class ShoppingListItemBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = ShoppingListItemBottomSheet.class.getSimpleName();

  private FragmentBottomsheetShoppingListItemBinding binding;
  private MainActivity activity;
  private String productName, amount;
  private ShoppingListItem shoppingListItem;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetShoppingListItemBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();

    Bundle bundle = getArguments();
    if (bundle == null) {
      dismiss();
      return binding.getRoot();
    }

    productName = bundle.getString(Constants.ARGUMENT.PRODUCT_NAME);
    amount = bundle.getString(Constants.ARGUMENT.AMOUNT);
    shoppingListItem = bundle.getParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM);

    // TOOLBAR

    ResUtil.tintMenuItemIcons(requireContext(), binding.toolbarShoppingListItem.getMenu());
    MenuItem itemPurchase = binding.toolbarShoppingListItem.getMenu()
        .findItem(R.id.action_purchase);
    if (itemPurchase != null) {
      itemPurchase.setVisible(productName != null);
    }
    binding.toolbarShoppingListItem.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_toggle_done) {
        activity.getCurrentFragment().toggleDoneStatus(shoppingListItem);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_purchase) {
        activity.getCurrentFragment().purchaseItem(shoppingListItem);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_edit) {
        activity.getCurrentFragment().editItem(shoppingListItem);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_delete) {
        activity.getCurrentFragment().deleteItem(shoppingListItem);
        dismiss();
        return true;
      }
      return false;
    });

    if (bundle.getBoolean(Constants.ARGUMENT.SHOW_OFFLINE)) {
      Menu menu = binding.toolbarShoppingListItem.getMenu();
      menu.findItem(R.id.action_purchase).setVisible(false);
      menu.findItem(R.id.action_edit).setVisible(false);
      menu.findItem(R.id.action_delete).setVisible(false);
    }

    setData();

    return binding.getRoot();
  }

  private void setData() {
    // NAME
    if (productName != null) {
      binding.itemShoppingListItemName.setText(
          activity.getString(R.string.property_name), productName
      );
    } else {
      binding.itemShoppingListItemName.setText(
          activity.getString(R.string.property_name),
          activity.getString(R.string.subtitle_empty)
      );
    }

    // AMOUNT
    binding.itemShoppingListItemAmount.setText(
        activity.getString(R.string.property_amount), amount
    );

    // NOTE
    String trimmedNote = (String) TextUtil.trimCharSequence(shoppingListItem.getNote());
    if (trimmedNote != null && !trimmedNote.isEmpty()) {
      binding.itemShoppingListItemNote.setSingleLine(false);
      binding.itemShoppingListItemNote.setText(
          activity.getString(R.string.property_note), trimmedNote
      );
    } else {
      binding.itemShoppingListItemNote.setSingleLine(true);
      binding.itemShoppingListItemNote.setText(
          activity.getString(R.string.property_note),
          activity.getString(R.string.subtitle_empty)
      );
    }

    // STATUS
    if (shoppingListItem.getDoneInt() == 1) {
      binding.itemShoppingListItemStatus.setText(
          activity.getString(R.string.property_status),
          activity.getString(R.string.subtitle_done)
      );
    } else {
      binding.itemShoppingListItemStatus.setText(
          activity.getString(R.string.property_status),
          activity.getString(R.string.subtitle_undone)
      );
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainerScroll.setPadding(
        binding.linearContainerScroll.getPaddingLeft(),
        binding.linearContainerScroll.getPaddingTop(),
        binding.linearContainerScroll.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
