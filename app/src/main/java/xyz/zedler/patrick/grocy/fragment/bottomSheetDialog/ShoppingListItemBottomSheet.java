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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ListItem;

public class ShoppingListItemBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = ShoppingListItemBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private String productName, amount;
  private ShoppingListItem shoppingListItem;
  private ListItem itemName, itemAmount, itemNote, itemStatus;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(
        R.layout.fragment_bottomsheet_shopping_list_item,
        container,
        false
    );

    activity = (MainActivity) getActivity();
    assert activity != null;

    Bundle bundle = getArguments();
    if (bundle == null) {
      dismiss();
      return view;
    }

    productName = bundle.getString(Constants.ARGUMENT.PRODUCT_NAME);
    amount = bundle.getString(Constants.ARGUMENT.AMOUNT);
    shoppingListItem = bundle.getParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM);

    // VIEWS

    itemName = view.findViewById(R.id.item_shopping_list_item_name);
    itemAmount = view.findViewById(R.id.item_shopping_list_item_amount);
    itemNote = view.findViewById(R.id.item_shopping_list_item_note);
    itemStatus = view.findViewById(R.id.item_shopping_list_item_status);

    // TOOLBAR

    MaterialToolbar toolbar = view.findViewById(R.id.toolbar_shopping_list_item);
    if (productName == null) {
      view.findViewById(R.id.action_purchase).setVisibility(View.GONE);
    }
    toolbar.setOnMenuItemClickListener(item -> {
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
      Menu menu = toolbar.getMenu();
      menu.findItem(R.id.action_purchase).setVisible(false);
      menu.findItem(R.id.action_edit).setVisible(false);
      menu.findItem(R.id.action_delete).setVisible(false);
    }

    setData();

    return view;
  }

  private void setData() {
    // NAME
    if (productName != null) {
      itemName.setText(activity.getString(R.string.property_name), productName);
    } else {
      itemName.setText(
          activity.getString(R.string.property_name),
          activity.getString(R.string.subtitle_empty)
      );
    }

    // AMOUNT
    itemAmount.setText(activity.getString(R.string.property_amount), amount);

    // NOTE
    String trimmedNote = (String) TextUtil.trimCharSequence(shoppingListItem.getNote());
    if (trimmedNote != null && !trimmedNote.isEmpty()) {
      itemNote.setSingleLine(false);
      itemNote.setText(activity.getString(R.string.property_note), trimmedNote);
    } else {
      itemNote.setSingleLine(true);
      itemNote.setText(
          activity.getString(R.string.property_note),
          activity.getString(R.string.subtitle_empty)
      );
    }

    // STATUS
    if (shoppingListItem.getDoneInt() == 1) {
      itemStatus.setText(
          activity.getString(R.string.property_status),
          activity.getString(R.string.subtitle_done)
      );
    } else {
      itemStatus.setText(
          activity.getString(R.string.property_status),
          activity.getString(R.string.subtitle_undone)
      );
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
