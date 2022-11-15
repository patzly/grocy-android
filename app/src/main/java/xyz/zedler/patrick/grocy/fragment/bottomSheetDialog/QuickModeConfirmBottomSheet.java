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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.transition.TransitionManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetScanModeConfirmBinding;

public class QuickModeConfirmBottomSheet extends BaseBottomSheetDialogFragment {

  private final static int CONFIRMATION_DURATION = 3000;
  private final static String TAG = QuickModeConfirmBottomSheet.class.getSimpleName();

  private FragmentBottomsheetScanModeConfirmBinding binding;
  private MainActivity activity;
  private ValueAnimator confirmProgressAnimator;

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

    binding.header.setOnClickListener(v -> hideAndStopProgress());
    binding.container.setOnClickListener(v -> hideAndStopProgress());
    binding.buttonCancel.setOnClickListener(v -> {
      activity.getCurrentFragment().interruptCurrentProductFlow();
      dismiss();
    });
    binding.buttonProceed.setOnClickListener(v -> {
      activity.getCurrentFragment().startTransaction();
      dismiss();
    });

    String msg = args.getString(Constants.ARGUMENT.TEXT);
    binding.text.setText(msg);

    startProgress();

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
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.container.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
