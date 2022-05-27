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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.ShoppingListAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingPlaceholderAdapter;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragmentDirections;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.repository.ShoppingListRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.ViewUtil.TouchProgressBarUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class ShoppingListsBottomSheet extends BaseBottomSheet
    implements ShoppingListAdapter.ShoppingListAdapterListener {

  private final static String TAG = ShoppingListsBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private TouchProgressBarUtil touchProgressBarUtil;

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
        R.layout.fragment_bottomsheet_list_selection, container, false
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
      return view;
    }

    ShoppingListRepository repository = new ShoppingListRepository(activity.getApplication());

    TextView textViewTitle = view.findViewById(R.id.text_list_selection_title);
    textViewTitle.setText(activity.getString(R.string.property_shopping_lists));

    RecyclerView recyclerView = view.findViewById(R.id.recycler_list_selection);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(new ShoppingPlaceholderAdapter());

    repository.getShoppingListsLive().observe(getViewLifecycleOwner(), shoppingLists -> {
      if (shoppingLists == null) {
        return;
      }
      if (recyclerView.getAdapter() == null
          || !(recyclerView.getAdapter() instanceof ShoppingListAdapter)
      ) {
        recyclerView.setAdapter(new ShoppingListAdapter(
            shoppingLists,
            selectedIdLive != null && selectedIdLive.getValue() != null
                ? selectedIdLive.getValue() : selectedId,
            this,
            activity.getCurrentFragment() instanceof ShoppingListFragment
                && activity.isOnline()
        ));
      } else {
        ((ShoppingListAdapter) recyclerView.getAdapter()).updateData(
            shoppingLists,
            selectedIdLive != null && selectedIdLive.getValue() != null
                ? selectedIdLive.getValue() : selectedId
        );
      }
    });

    if (selectedIdLive != null) {
      selectedIdLive.observe(getViewLifecycleOwner(), selectedIdNew -> {
        if (recyclerView.getAdapter() == null
            || !(recyclerView.getAdapter() instanceof ShoppingListAdapter)
        ) {
          return;
        }
        ((ShoppingListAdapter) recyclerView.getAdapter()).updateSelectedId(selectedIdNew);
      });
    }

    ActionButton buttonNew = view.findViewById(R.id.button_list_selection_new);
    if (activity.isOnline() && multipleListsFeature
        && activity.getCurrentFragment() instanceof ShoppingListFragment
    ) {
      buttonNew.setVisibility(View.VISIBLE);
      buttonNew.setOnClickListener(v -> {
        dismiss();
        navigate(ShoppingListFragmentDirections
            .actionShoppingListFragmentToShoppingListEditFragment());
      });
    }

    touchProgressBarUtil = new TouchProgressBarUtil(
        view.findViewById(R.id.progress_confirmation),
        null,
        object -> activity.getCurrentFragment().deleteShoppingList((ShoppingList) object)
    );

    return view;
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
