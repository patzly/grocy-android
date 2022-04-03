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
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.UserAdapter;
import xyz.zedler.patrick.grocy.adapter.UserAdapter.UserAdapterListener;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class UsersBottomSheet extends BaseBottomSheet
    implements UserAdapterListener {

  private final static String TAG = UsersBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetListSelectionBinding binding;

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
    binding = FragmentBottomsheetListSelectionBinding
        .inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    setSkipCollapsedInPortrait();
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) requireActivity();
    Bundle bundle = requireArguments();

    ArrayList<User> usersArg = bundle
        .getParcelableArrayList(ARGUMENT.USERS);
    assert usersArg != null;
    ArrayList<User> users = new ArrayList<>(usersArg);

    SortUtil.sortUsersByName(requireContext(), users, true);
    if (bundle.getBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, false)) {
      users.add(0, new User(-1, getString(R.string.subtitle_none_selected)));
    }
    int selected = bundle.getInt(ARGUMENT.SELECTED_ID, -1);

    binding.textListSelectionTitle.setText(activity.getString(R.string.property_users));
    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recyclerListSelection.setItemAnimator(new DefaultItemAnimator());
    binding.recyclerListSelection.setAdapter(new UserAdapter(users, selected, this));;
  }

  @Override
  public void onItemRowClicked(User user) {
    BaseFragment currentFragment = activity.getCurrentFragment();
    currentFragment.selectUser(user);
    dismiss();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
