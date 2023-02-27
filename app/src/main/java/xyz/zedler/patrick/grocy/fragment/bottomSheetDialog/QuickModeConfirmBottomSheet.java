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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.transition.TransitionManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetScanModeConfirmBinding;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class QuickModeConfirmBottomSheet extends BaseBottomSheetDialogFragment {

  private final static int CONFIRMATION_DURATION = 3000;
  private final static String TAG = QuickModeConfirmBottomSheet.class.getSimpleName();

  private FragmentBottomsheetScanModeConfirmBinding binding;
  private MainActivity activity;
  private ValueAnimator confirmProgressAnimator;
  private boolean openAction = false;
  private boolean progressStopped = false;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetScanModeConfirmBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle args = requireArguments();

    binding.toolbar.setOnClickListener(v -> hideAndStopProgress());
    binding.container.setOnClickListener(v -> hideAndStopProgress());

    if (args.containsKey(ARGUMENT.ACTION)) {
      openAction = args.getString(ARGUMENT.ACTION).equals(ACTION.OPEN);
      binding.toggleGroupConsumeType.setVisibility(View.VISIBLE);
      updateToggleGroup();
    } else {
      binding.toggleGroupConsumeType.setVisibility(View.GONE);
    }

    if (savedInstanceState != null) {
      openAction = savedInstanceState.getBoolean("open");
      progressStopped = savedInstanceState.getBoolean("stopped");
    }

    binding.toggleConsume.setOnClickListener(v -> {
      openAction = false;
      updateToggleGroup();
      binding.text.setText(openAction
          ? args.getString(ARGUMENT.TEXT_ALTERNATIVE)
          : args.getString(ARGUMENT.TEXT));
    });
    binding.toggleOpen.setOnClickListener(v -> {
      openAction = true;
      updateToggleGroup();
      binding.text.setText(openAction
          ? args.getString(ARGUMENT.TEXT_ALTERNATIVE)
          : args.getString(ARGUMENT.TEXT));
    });

    binding.buttonCancel.setOnClickListener(v -> {
      activity.getCurrentFragment().interruptCurrentProductFlow();
      dismiss();
    });
    binding.buttonProceed.setOnClickListener(v -> {
      activity.getCurrentFragment().startTransaction();
      dismiss();
    });

    if (args.containsKey(ARGUMENT.ACTION)) {
      binding.text.setText(openAction
          ? args.getString(ARGUMENT.TEXT_ALTERNATIVE)
          : args.getString(ARGUMENT.TEXT));
    } else {
      binding.text.setText(args.getString(Constants.ARGUMENT.TEXT));
    }

    if (!progressStopped) {
      startProgress();
    } else {
      binding.progressTimeout.setVisibility(View.GONE);
    }

    setCancelable(false);

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onDestroyView() {
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("open", openAction);
    outState.putBoolean("stopped", progressStopped);
  }

  private void updateToggleGroup() {
    if (openAction) {
      binding.toggleConsume.setChecked(false);
      binding.toggleOpen.setChecked(true);
    } else {
      binding.toggleConsume.setChecked(true);
      binding.toggleOpen.setChecked(false);
    }
  }

  private void startProgress() {
    int startValue = 0;
    if (confirmProgressAnimator != null) {
      startValue = binding.progressTimeout.getProgress();
      if (startValue == 100) {
        startValue = 0;
      }
      confirmProgressAnimator.removeAllListeners();
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    confirmProgressAnimator = ValueAnimator.ofInt(startValue, binding.progressTimeout.getMax());
    confirmProgressAnimator.setDuration((long) CONFIRMATION_DURATION
        * (binding.progressTimeout.getMax() - startValue) / binding.progressTimeout.getMax());
    confirmProgressAnimator.addUpdateListener(
        animation -> binding.progressTimeout.setProgress((Integer) animation.getAnimatedValue())
    );
    confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (binding.progressTimeout.getProgress() != binding.progressTimeout.getMax()) {
          return;
        }
        if (binding.toggleGroupConsumeType.getVisibility() == View.VISIBLE) {
          activity.getCurrentFragment().setMarkAsOpenToggle(openAction);
        }
        activity.getCurrentFragment().startTransaction();
        dismiss();
      }
    });
    confirmProgressAnimator.start();
  }

  private void hideAndStopProgress() {
    confirmProgressAnimator.cancel();
    TransitionManager.beginDelayedTransition((ViewGroup) requireView());
    binding.progressTimeout.setVisibility(View.GONE);
    progressStopped = true;
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.container.setPadding(
        binding.container.getPaddingLeft(),
        binding.container.getPaddingTop(),
        binding.container.getPaddingRight(),
        UiUtil.dpToPx(activity, 12) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
