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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.transition.TransitionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class QuickModeConfirmBottomSheet extends BaseBottomSheet {

  private final static int CONFIRMATION_DURATION = 3000;
  private final static String TAG = QuickModeConfirmBottomSheet.class.getSimpleName();

  private MainActivity activity;

  private ProgressBar progressTimeout;
  private ValueAnimator confirmProgressAnimator;

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
        R.layout.fragment_bottomsheet_scan_mode_confirm, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle args = requireArguments();

    view.findViewById(R.id.header).setOnClickListener(v -> hideAndStopProgress());
    view.findViewById(R.id.container).setOnClickListener(v -> hideAndStopProgress());
    view.findViewById(R.id.button_cancel).setOnClickListener(v -> {
      activity.getCurrentFragment().interruptCurrentProductFlow();
      dismiss();
    });
    view.findViewById(R.id.button_proceed).setOnClickListener(v -> {
      activity.getCurrentFragment().startTransaction();
      dismiss();
    });

    String msg = args.getString(Constants.ARGUMENT.TEXT);
    ((TextView) view.findViewById(R.id.text)).setText(msg);

    progressTimeout = view.findViewById(R.id.progress_timeout);
    startProgress();

    setCancelable(false);

    return view;
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
      startValue = progressTimeout.getProgress();
      if (startValue == 100) {
        startValue = 0;
      }
      confirmProgressAnimator.removeAllListeners();
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    confirmProgressAnimator = ValueAnimator.ofInt(startValue, progressTimeout.getMax());
    confirmProgressAnimator.setDuration((long) CONFIRMATION_DURATION
        * (progressTimeout.getMax() - startValue) / progressTimeout.getMax());
    confirmProgressAnimator.addUpdateListener(
        animation -> progressTimeout.setProgress((Integer) animation.getAnimatedValue())
    );
    confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (progressTimeout.getProgress() != progressTimeout.getMax()) {
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
    progressTimeout.setVisibility(View.GONE);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
