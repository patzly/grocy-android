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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ExpandableCard;
import xyz.zedler.patrick.grocy.view.ListItem;

public class MasterProductBottomSheet extends BaseBottomSheet {

	private final static String TAG = MasterProductBottomSheet.class.getSimpleName();

	private MainActivity activity;
	private Product product;
	private Location location;
	private QuantityUnit quantityUnitPurchase, quantityUnitStock;
	private ProductGroup productGroup;
	private ListItem
			itemName,
			itemLocation,
			itemMinStockAmount,
			itemQuPurchase,
			itemQuStock,
			itemQuFactor,
			itemProductGroup,
			itemBarcodes;
	private ExpandableCard cardDescription;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new BottomSheetDialog(
				requireContext(),
				R.style.Theme_Grocy_BottomSheetDialog
		);
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState
	) {
		View view = inflater.inflate(
				R.layout.fragment_bottomsheet_master_product,
				container,
				false
		);

		activity = (MainActivity) getActivity();
		assert activity != null;

		Bundle bundle = getArguments();
		if(bundle != null) {
			product = bundle.getParcelable(Constants.ARGUMENT.PRODUCT);
			location = bundle.getParcelable(Constants.ARGUMENT.LOCATION);
			quantityUnitPurchase = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE);
			quantityUnitStock = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT_STOCK);
			productGroup = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_GROUP);
		}

		// VIEWS

		itemName = view.findViewById(R.id.item_master_product_name);
		cardDescription = view.findViewById(R.id.card_master_product_description);
		itemLocation = view.findViewById(R.id.item_master_product_location);
		itemMinStockAmount = view.findViewById(R.id.item_master_product_min_stock_amount);
		itemQuPurchase = view.findViewById(R.id.item_master_product_qu_purchase);
		itemQuStock = view.findViewById(R.id.item_master_product_qu_stock);
		itemQuFactor = view.findViewById(R.id.item_master_product_qu_factor);
		itemProductGroup = view.findViewById(R.id.item_master_product_product_group);
		itemBarcodes = view.findViewById(R.id.item_master_product_barcodes);

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_master_product);
		toolbar.setOnMenuItemClickListener(item -> {
			BaseFragment fragmentCurrent = activity.getCurrentFragment();
			if(item.getItemId() == R.id.action_edit) {
				fragmentCurrent.editObject(product);
			} else if(item.getItemId() == R.id.action_copy) {
				fragmentCurrent.copyProduct(product);
			} else if(item.getItemId() == R.id.action_delete) {
				fragmentCurrent.deleteObjectSafely(product);
			}
			dismiss();
			return true;
		});

		setData();

		return view;
	}

	private void setData() {
		// NAME
		itemName.setText(
				activity.getString(R.string.property_name),
				product.getName()
		);

		// DESCRIPTION
		Spanned description = product.getDescription() != null
				? Html.fromHtml(product.getDescription())
				: null;
		description = (Spanned) TextUtil.trimCharSequence(description);
		if(description != null && !description.toString().isEmpty()) {
			cardDescription.setText(description.toString());
		} else {
			cardDescription.setVisibility(View.GONE);
		}

		// LOCATION
		if(location != null) {
			itemLocation.setText(
					activity.getString(R.string.property_location),
					location.getName()
			);
		} else {
			itemLocation.setVisibility(View.GONE);
		}

		// MIN STOCK AMOUNT
		itemMinStockAmount.setText(
				activity.getString(R.string.property_amount_min_stock),
				NumUtil.trim(product.getMinStockAmountDouble())
		);

		// QUANTITY UNIT PURCHASE
		if(quantityUnitPurchase != null) {
			itemQuPurchase.setText(
					activity.getString(R.string.property_default_qu_purchase),
					quantityUnitPurchase.getName()
			);
		} else {
			itemQuPurchase.setVisibility(View.GONE);
		}

		// QUANTITY UNIT STOCK
		if(quantityUnitStock != null) {
			itemQuStock.setText(
					activity.getString(R.string.property_qu_stock),
					quantityUnitStock.getName()
			);
		} else {
			itemQuStock.setVisibility(View.GONE);
		}

		// QUANTITY UNIT FACTOR
		itemQuFactor.setText(
				activity.getString(R.string.property_qu_factor),
				NumUtil.trim(product.getQuFactorPurchaseToStockDouble())
		);

		// PRODUCT GROUP
		if(product.getProductGroupId() != null && productGroup != null) {
			itemProductGroup.setText(
					activity.getString(R.string.property_product_group),
					productGroup.getName()
			);
		} else {
			itemProductGroup.setVisibility(View.GONE);
		}

		// BARCODES
		if(product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
			itemBarcodes.setSingleLine(false);
			itemBarcodes.setText(
					activity.getString(R.string.property_barcodes),
					TextUtils.join(", ", product.getBarcode().split(","))
			);
		} else {
			itemBarcodes.setVisibility(View.GONE);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
