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

import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.ListItem;

public class MasterQuantityUnitBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static String TAG = "MasterQuantityUnitBottomSheet";

	private MainActivity activity;
	private QuantityUnit quantityUnit;
	private ListItem itemName, itemNamePlural, itemDescription;

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
				R.layout.fragment_bottomsheet_master_quantity_unit,
				container,
				false
		);

		activity = (MainActivity) getActivity();
		assert activity != null;

		Bundle bundle = getArguments();
		if(bundle != null) quantityUnit = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);

		// VIEWS

		itemName = view.findViewById(R.id.item_master_quantity_unit_name);
		itemNamePlural = view.findViewById(R.id.item_master_quantity_unit_name_plural);
		itemDescription = view.findViewById(R.id.item_master_quantity_unit_description);

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_master_quantity_unit);
		toolbar.setOnMenuItemClickListener(item -> {
			Fragment fragmentCurrent = activity.getCurrentFragment();
			if(fragmentCurrent.getClass() != MasterQuantityUnitsFragment.class) return false;
			switch (item.getItemId()) {
				case R.id.action_edit:
					((MasterQuantityUnitsFragment) fragmentCurrent).editQuantityUnit(quantityUnit);
					dismiss();
					return true;
				case R.id.action_delete:
					((MasterQuantityUnitsFragment) fragmentCurrent).checkForUsage(quantityUnit);
					dismiss();
					return true;
			}
			return false;
		});

		setData();

		return view;
	}

	private void setData() {
		// NAME (singular & plural form)
		itemName.setText(
				activity.getString(R.string.property_name_singular),
				quantityUnit.getName()
		);
		itemNamePlural.setText(
				activity.getString(R.string.property_name_plural),
				quantityUnit.getNamePlural()
		);

		// DESCRIPTION
		String description = quantityUnit.getDescription();
		if(description != null && !description.isEmpty()) {
			itemDescription.setSingleLine(false);
			itemDescription.setText(activity.getString(R.string.property_description), description);
		} else {
			itemDescription.setVisibility(View.GONE);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
