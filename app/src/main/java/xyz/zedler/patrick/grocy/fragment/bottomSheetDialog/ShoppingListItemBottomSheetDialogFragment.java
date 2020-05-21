package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ListItem;

public class ShoppingListItemBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static boolean DEBUG = false;
	private final static String TAG = "ShoppingListItemBottomSheet";

	private MainActivity activity;
	private String productName, quantityUnit;
	private int position;
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
		if(bundle == null) {
			dismiss();
			return view;
		}

		productName = bundle.getString(Constants.ARGUMENT.PRODUCT_NAME);
		quantityUnit = bundle.getString(Constants.ARGUMENT.QUANTITY_UNIT);
		shoppingListItem = bundle.getParcelable(Constants.ARGUMENT.SHOPPING_LIST_ITEM);
		position = bundle.getInt(Constants.ARGUMENT.POSITION);

		// VIEWS

		itemName = view.findViewById(R.id.item_shopping_list_item_name);
		itemAmount = view.findViewById(R.id.item_shopping_list_item_amount);
		itemNote = view.findViewById(R.id.item_shopping_list_item_note);
		itemStatus = view.findViewById(R.id.item_shopping_list_item_status);

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_shopping_list_item);
		toolbar.setOnMenuItemClickListener(item -> {
			Fragment fragmentCurrent = activity.getCurrentFragment();
			if(fragmentCurrent.getClass() != ShoppingListFragment.class) return false;
			switch (item.getItemId()) {
				case R.id.action_edit:
					((ShoppingListFragment) fragmentCurrent).editItem(position);
					dismiss();
					return true;
				case R.id.action_delete:
					((ShoppingListFragment) fragmentCurrent).deleteRequest(position);
					dismiss();
					return true;
			}
			return false;
		});

		setData();

		return view;
	}

	private void setData() {
		// NAME
		if(productName != null) {
			itemName.setText(activity.getString(R.string.property_name), productName);
		} else {
			itemName.setText(
					activity.getString(R.string.property_name),
					activity.getString(R.string.subtitle_none)
			);
		}

		// AMOUNT
		if(quantityUnit == null) quantityUnit = "";
		String textAmount = activity.getString(
				R.string.subtitle_amount,
				NumUtil.trim(shoppingListItem.getAmount()),
				quantityUnit
		);
		itemAmount.setText(activity.getString(R.string.property_amount), textAmount);

		// NOTE
		String trimmedNote = TextUtil.getFromHtml(shoppingListItem.getNote());
		if(trimmedNote != null) {
			itemNote.setSingleLine(false);
			itemNote.setText(activity.getString(R.string.property_note), trimmedNote);
		} else {
			itemNote.setSingleLine(true);
			itemNote.setText(
					activity.getString(R.string.property_note),
					activity.getString(R.string.subtitle_none)
			);
		}

		// STATUS
		if(!shoppingListItem.isUndone()) {
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
