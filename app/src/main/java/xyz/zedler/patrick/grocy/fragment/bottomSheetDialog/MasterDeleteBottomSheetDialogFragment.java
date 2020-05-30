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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterLocationFragment;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductSimpleFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoreFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoresFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterDeleteBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "MasterDeleteBottomSheet";

    private MainActivity activity;

    private Product product;
    private Location location;
    private Store store;
    private QuantityUnit quantityUnit;
    private ProductGroup productGroup;

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
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_master_delete, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        String textType = "";
        String textName = "";

        Bundle bundle = getArguments();
        if(bundle != null) {
            String type = bundle.getString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.PRODUCT);
            switch (type) {
                case Constants.ARGUMENT.LOCATION:
                    location = bundle.getParcelable(Constants.ARGUMENT.LOCATION);
                    if(location != null) {
                        textType = activity.getString(R.string.type_location);
                        textName = location.getName();
                    }
                    break;
                case Constants.ARGUMENT.STORE:
                    store = bundle.getParcelable(Constants.ARGUMENT.STORE);
                    if(store != null) {
                        textType = activity.getString(R.string.type_store);
                        textName = store.getName();
                    }
                    break;
                case Constants.ARGUMENT.QUANTITY_UNIT:
                    quantityUnit = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);
                    if(quantityUnit != null) {
                        textType = activity.getString(R.string.type_quantity_unit);
                        textName = quantityUnit.getName();
                    }
                    break;
                case Constants.ARGUMENT.PRODUCT_GROUP:
                    productGroup = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_GROUP);
                    if(productGroup != null) {
                        textType = activity.getString(R.string.type_product_group);
                        textName = productGroup.getName();
                    }
                    break;
                default:
                    product = bundle.getParcelable(Constants.ARGUMENT.PRODUCT);
                    if(product != null) {
                        textType = activity.getString(R.string.type_product);
                        textName = product.getName();
                    }
            }
        }

        TextView textView = view.findViewById(R.id.text_master_delete_question);
        textView.setText(
                activity.getString(
                        R.string.msg_master_delete,
                        textType,
                        textName
                )
        );

        view.findViewById(R.id.button_master_delete_delete).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == MasterProductsFragment.class) {
                ((MasterProductsFragment) current).deleteProduct(product);
            } else if(current.getClass() == MasterProductSimpleFragment.class) {
                ((MasterProductSimpleFragment) current).deleteProduct(product);
            } else if(current.getClass() == MasterLocationsFragment.class) {
                ((MasterLocationsFragment) current).deleteLocation(location);
            } else if(current.getClass() == MasterLocationFragment.class) {
                ((MasterLocationFragment) current).deleteLocation(location);
            } else if(current.getClass() == MasterStoresFragment.class) {
                ((MasterStoresFragment) current).deleteStore(store);
            } else if(current.getClass() == MasterStoreFragment.class) {
                ((MasterStoreFragment) current).deleteStore(store);
            } else if(current.getClass() == MasterQuantityUnitsFragment.class) {
                ((MasterQuantityUnitsFragment) current).deleteQuantityUnit(quantityUnit);
            } else if(current.getClass() == MasterQuantityUnitFragment.class) {
                ((MasterQuantityUnitFragment) current).deleteQuantityUnit(quantityUnit);
            } else if(current.getClass() == MasterProductGroupsFragment.class) {
                ((MasterProductGroupsFragment) current).deleteProductGroup(productGroup);
            } else if(current.getClass() == MasterProductGroupFragment.class) {
                ((MasterProductGroupFragment) current).deleteProductGroup(productGroup);
            }
            dismiss();
        });

        view.findViewById(R.id.button_master_delete_cancel).setOnClickListener(v -> dismiss());

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
