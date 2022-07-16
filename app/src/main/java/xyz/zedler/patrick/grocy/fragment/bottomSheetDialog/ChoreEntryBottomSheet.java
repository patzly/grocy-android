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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetChoreEntryBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class ChoreEntryBottomSheet extends BaseBottomSheet {

  private final static String TAG = ChoreEntryBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetChoreEntryBinding binding;
  private Chore chore;

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
    binding = FragmentBottomsheetChoreEntryBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) getActivity();
    assert activity != null;

    Bundle bundle = getArguments();
    chore = bundle != null ? bundle.getParcelable(ARGUMENT.CHORE) : null;
    if (bundle == null || chore == null) {
      dismiss();
      return;
    }

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    DateUtil dateUtil = new DateUtil(activity);
    DownloadHelper dlHelper = new DownloadHelper(activity, TAG);

    binding.name.setText(getString(R.string.property_name), chore.getName());
    if (chore.getDescription() == null || chore.getDescription().trim().isEmpty()) {
      binding.cardDescription.setVisibility(View.GONE);
    } else {
      binding.cardDescription.setText(chore.getDescription());
    }

    binding.toolbar.getMenu().findItem(R.id.action_skip_next_chore_schedule)
        .setVisible(VersionUtil.isGrocyServerMin320(sharedPrefs));
    binding.toolbar.getMenu().findItem(R.id.action_reschedule_next_execution)
        .setVisible(VersionUtil.isGrocyServerMin330(sharedPrefs));

    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_track_chore_execution) {
        activity.getCurrentFragment().trackChoreExecution(chore.getId());
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_skip_next_chore_schedule) {
        activity.getCurrentFragment().skipNextChoreSchedule(chore.getId());
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_reschedule_next_execution) {
        activity.getCurrentFragment().rescheduleNextExecution(chore.getId());
        dismiss();
        return true;
      }
      return false;
    });

    dlHelper.getChoreDetails(chore.getId(), choreDetails -> {
      binding.trackedCount.setText(
          getString(R.string.property_tracked_count),
          String.valueOf(choreDetails.getTrackedCount())
      );
      if (NumUtil.isStringDouble(choreDetails.getAverageExecutionFrequencyHours())) {
        double avgFreq = NumUtil.toDouble(choreDetails.getAverageExecutionFrequencyHours());
        int avgFreqDays = (int) Math.round(avgFreq / 24);
        binding.trackedCount.setText(
            getString(R.string.property_average_execution_frequency),
            dateUtil.getHumanDuration(avgFreqDays)
        );
      } else {
        binding.trackedCount.setText(
            getString(R.string.property_average_execution_frequency),
            getString(R.string.subtitle_none)
        );
      }

      binding.lastTracked.setText(
          getString(R.string.property_last_tracked),
          choreDetails.getLastTracked(),
          chore.getTrackDateOnlyBoolean()
              ? dateUtil.getHumanForDaysFromNow(choreDetails.getLastTracked())
              : dateUtil.getHumanForDaysFromNow(choreDetails.getLastTracked(), true)
      );
      if (choreDetails.getLastDoneBy() != null) {
        binding.lastDoneBy.setText(
            getString(R.string.property_last_done_by),
            choreDetails.getLastDoneBy().getUserName()
        );
      }
    }).perform(dlHelper.getUuid());
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
