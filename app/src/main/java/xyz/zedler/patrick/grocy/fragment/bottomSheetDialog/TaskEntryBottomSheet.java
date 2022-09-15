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
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.TransitionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetTaskEntryBinding;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.DateUtil;

public class TaskEntryBottomSheet extends BaseBottomSheetDialogFragment {

  private final static int DELETE_CONFIRMATION_DURATION = 1000;
  private final static String TAG = TaskEntryBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetTaskEntryBinding binding;
  private ProgressBar progressConfirm;
  private ValueAnimator confirmProgressAnimator;
  private Task task;

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
    binding = FragmentBottomsheetTaskEntryBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    super.onDestroyView();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) getActivity();
    assert activity != null;

    Bundle bundle = getArguments();
    task = bundle != null ? bundle.getParcelable(ARGUMENT.TASK) : null;
    if (bundle == null || task == null) {
      dismiss();
      return;
    }

    DateUtil dateUtil = new DateUtil(activity);

    binding.name.setText(getString(R.string.property_name), task.getName());
    if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
      binding.cardDescription.setVisibility(View.GONE);
    } else {
      binding.cardDescription.setText(task.getDescription());
    }
    if (task.getDueDate() != null && !task.getDueDate().isEmpty()) {
      binding.date.setText(
          getString(R.string.property_due_date),
          dateUtil.getLocalizedDate(task.getDueDate()),
          dateUtil.getHumanForDaysFromNow(task.getDueDate())
      );
    } else {
      binding.date.setText(
          getString(R.string.property_due_date),
          getString(R.string.subtitle_none_selected)
      );
    }
    binding.category.setText(
        getString(R.string.property_category),
        bundle.getString(ARGUMENT.TASK_CATEGORY)
    );
    binding.assignedTo.setText(
        getString(R.string.property_assigned_to),
        bundle.getString(ARGUMENT.USER) != null ? bundle.getString(ARGUMENT.USER)
            : getString(R.string.subtitle_none_selected)
    );

    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_toggle_done) {
        activity.getCurrentFragment().toggleDoneStatus(task);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_edit) {
        activity.getCurrentFragment().editTask(task);
        dismiss();
        return true;
      }
      return false;
    });
    binding.delete.setOnTouchListener((v, event) -> {
      onTouchDelete(v, event);
      return true;
    });
    progressConfirm = binding.progressConfirmation;
  }

  public void onTouchDelete(View view, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      showAndStartProgress(view);
    } else if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL) {
      hideAndStopProgress();
    }
  }

  private void showAndStartProgress(View buttonView) {
    assert getView() != null;
    TransitionManager.beginDelayedTransition((ViewGroup) getView());
    progressConfirm.setVisibility(View.VISIBLE);
    int startValue = 0;
    if (confirmProgressAnimator != null) {
      startValue = progressConfirm.getProgress();
      if (startValue == 100) {
        startValue = 0;
      }
      confirmProgressAnimator.removeAllListeners();
      confirmProgressAnimator.cancel();
      confirmProgressAnimator = null;
    }
    confirmProgressAnimator = ValueAnimator.ofInt(startValue, progressConfirm.getMax());
    confirmProgressAnimator.setDuration((long) DELETE_CONFIRMATION_DURATION
        * (progressConfirm.getMax() - startValue) / progressConfirm.getMax());
    confirmProgressAnimator.addUpdateListener(
        animation -> progressConfirm.setProgress((Integer) animation.getAnimatedValue())
    );
    confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        int currentProgress = progressConfirm.getProgress();
        if (currentProgress == progressConfirm.getMax()) {
          TransitionManager.beginDelayedTransition((ViewGroup) requireView());
          progressConfirm.setVisibility(View.GONE);
          ImageView buttonImage = buttonView.findViewById(R.id.image_action_button);
          ((Animatable) buttonImage.getDrawable()).start();
          activity.getCurrentFragment().deleteTask(task);
          dismiss();
          return;
        }
        confirmProgressAnimator = ValueAnimator.ofInt(currentProgress, 0);
        confirmProgressAnimator.setDuration((long) (DELETE_CONFIRMATION_DURATION / 2)
            * currentProgress / progressConfirm.getMax());
        confirmProgressAnimator.setInterpolator(new FastOutSlowInInterpolator());
        confirmProgressAnimator.addUpdateListener(
            anim -> progressConfirm.setProgress((Integer) anim.getAnimatedValue())
        );
        confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            TransitionManager.beginDelayedTransition((ViewGroup) requireView());
            progressConfirm.setVisibility(View.GONE);
          }
        });
        confirmProgressAnimator.start();
      }
    });
    confirmProgressAnimator.start();
  }

  private void hideAndStopProgress() {
    if (confirmProgressAnimator != null) {
      confirmProgressAnimator.cancel();
    }

    if (progressConfirm.getProgress() != 100) {
      Toast.makeText(requireContext(), R.string.msg_press_hold_confirm, Toast.LENGTH_LONG).show();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
