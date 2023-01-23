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
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetMasterDeleteBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MasterDeleteBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = MasterDeleteBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetMasterDeleteBinding binding;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetMasterDeleteBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();

    String entity = requireArguments().getString(Constants.ARGUMENT.ENTITY);
    String textName = requireArguments().getString(Constants.ARGUMENT.OBJECT_NAME);
    int objectId = requireArguments().getInt(Constants.ARGUMENT.OBJECT_ID);

    int entityStrId;
    switch (entity) {
      case GrocyApi.ENTITY.PRODUCTS:
        entityStrId = R.string.property_product;
        break;
      case GrocyApi.ENTITY.QUANTITY_UNITS:
        entityStrId = R.string.property_quantity_unit;
        break;
      case GrocyApi.ENTITY.LOCATIONS:
        entityStrId = R.string.property_location;
        break;
      case GrocyApi.ENTITY.PRODUCT_GROUPS:
        entityStrId = R.string.property_product_group;
        break;
      case GrocyApi.ENTITY.TASK_CATEGORIES:
        entityStrId = R.string.property_task_category;
        break;
      default: // STORES
        entityStrId = R.string.property_store;
    }
    String entityText = getString(entityStrId);

    if (entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
      binding.textMasterDeleteQuestion.setText(
          activity.getString(
              R.string.msg_master_delete_product,
              textName
          )
      );
    } else {
      binding.textMasterDeleteQuestion.setText(
          activity.getString(
              R.string.msg_master_delete,
              entityText,
              textName
          )
      );
    }

    binding.buttonMasterDeleteDelete.setOnClickListener(v -> {
      BaseFragment currentFragment = activity.getCurrentFragment();
      currentFragment.deleteObject(objectId);
      dismiss();
    });

    binding.buttonMasterDeleteCancel.setOnClickListener(v -> dismiss());

    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainerScroll.setPadding(
        binding.linearContainerScroll.getPaddingLeft(),
        binding.linearContainerScroll.getPaddingTop(),
        binding.linearContainerScroll.getPaddingRight(),
        UiUtil.dpToPx(activity, 12) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
