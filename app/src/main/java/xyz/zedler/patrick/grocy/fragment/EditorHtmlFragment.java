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

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.FAB;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentEditorHtmlBinding;

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

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.summernote);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.summernote, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_empty);
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

    binding.summernote.setOverScrollMode(View.OVER_SCROLL_NEVER);

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
