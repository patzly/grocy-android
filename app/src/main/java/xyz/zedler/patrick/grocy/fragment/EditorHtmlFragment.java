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

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentEditorHtmlBinding;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.FAB;
import xyz.zedler.patrick.grocy.util.Constants.FAB.POSITION;

public class EditorHtmlFragment extends BaseFragment {

  private final static String TAG = EditorHtmlFragment.class.getSimpleName();

  private FragmentEditorHtmlBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentEditorHtmlBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    binding.setActivity(activity);

    activity.updateBottomAppBar(POSITION.END, R.menu.menu_empty);
    activity.updateFab(
        R.drawable.ic_round_done,
        R.string.action_back,
        FAB.TAG.DONE,
        true,
        () -> {
          setForPreviousDestination(Constants.ARGUMENT.DESCRIPTION, binding.summernote.getText());
          activity.navigateUp();
        }
    );


    EditorHtmlFragmentArgs args = EditorHtmlFragmentArgs.fromBundle(getArguments());
    if (args.getText() != null && savedInstanceState == null) {
      binding.summernote.setText(args.getText());
    } else if (savedInstanceState != null) {
      binding.summernote.setText(savedInstanceState.getString("text"));
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putString("text", binding.summernote.getText());
    super.onSaveInstanceState(outState);
  }

  @Override
  public void getActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    binding.summernote.onActivityResult(requestCode, resultCode, data);
  }
}
