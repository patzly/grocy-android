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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentWebsiteServerBinding;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.FAB;
import xyz.zedler.patrick.grocy.util.Constants.FAB.POSITION;

public class WebsiteServerFragment extends BaseFragment {

  private final static String TAG = WebsiteServerFragment.class.getSimpleName();

  private FragmentWebsiteServerBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentWebsiteServerBinding.inflate(inflater, container, false);
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

    activity.updateBottomAppBar(
        POSITION.END,
        R.menu.menu_empty,
        () -> {}
    );
    activity.updateFab(
        R.drawable.ic_round_done,
        R.string.action_back,
        FAB.TAG.DONE,
        true,
        () -> {
          activity.navigateUp();
        }
    );

    /*EditorHtmlFragmentArgs args = EditorHtmlFragmentArgs.fromBundle(getArguments());
    if (args.getHtmlText() != null && savedInstanceState == null) {
      binding.visual.fromHtml(args.getHtmlText(), true);
    }*/
  }
}
