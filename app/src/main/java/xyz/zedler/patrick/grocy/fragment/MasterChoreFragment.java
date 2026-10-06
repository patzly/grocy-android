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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.DayOfWeek;
import java.util.ArrayList;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterChoreBinding;
import xyz.zedler.patrick.grocy.form.FormDataChore;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.viewmodel.MasterChoreViewModel;
import xyz.zedler.patrick.grocy.util.LocaleUtil;

public class MasterChoreFragment extends BaseFragment {

  private MainActivity activity;
  private FragmentMasterChoreBinding binding;
  private MasterChoreViewModel viewModel;
  private androidx.appcompat.app.AlertDialog weekdaysDialog;
  private MaterialDatePicker<Long> datePicker;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentMasterChoreBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(MasterChoreViewModel.class);
    SavedStateHandle state = viewModel.getState();
    if (!state.contains("start_date")) state.set("start_date", LocalDate.now().toString());
    if (!state.contains("period")) state.set("period", 1);
    if (!state.contains("weekdays")) state.set("weekdays", new ArrayList<String>());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);
    binding.swipe.setEnabled(false);
    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    bindText(binding.editTextName, "name", "");
    bindText(binding.editTextDescription, "description", "");
    bindText(binding.editTextInterval, "interval", "1");
    bindText(binding.editTextMonthDay, "month_day", "1");
    binding.spinnerPeriod.setSelection(state.get("period"));
    binding.spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View selected, int position, long id) {
        state.set("period", position);
        updateSchedule();
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {}
    });
    binding.buttonWeekdays.setOnClickListener(v -> showWeekdays());
    binding.buttonStartDate.setOnClickListener(v -> showStartDate());
    binding.buttonOpenServer.setOnClickListener(v -> startActivity(new Intent(
        Intent.ACTION_VIEW, Uri.parse(activity.getGrocyApi().getBaseUrl() + "/chores")
    )));
    updateSchedule();
    androidx.fragment.app.Fragment restoredPicker = getChildFragmentManager()
        .findFragmentByTag("chore_start_date");
    if (restoredPicker instanceof MaterialDatePicker) {
      // The child dialog can be restored independently of this view after rotation.
      datePicker = (MaterialDatePicker<Long>) restoredPicker;
      bindDatePicker();
    }

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll, true);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_empty, null);
    activity.updateFab(R.drawable.ic_round_backup, R.string.action_save, Constants.FAB.TAG.SAVE,
        savedInstanceState == null, this::saveChore);

    viewModel.getSavingLive().observe(getViewLifecycleOwner(), saving -> {
      binding.swipe.setRefreshing(Boolean.TRUE.equals(saving));
      boolean enabled = !Boolean.TRUE.equals(saving) && !viewModel.hasCreatedChore();
      binding.editTextName.setEnabled(enabled);
      binding.editTextDescription.setEnabled(enabled);
      binding.spinnerPeriod.setEnabled(enabled);
      binding.editTextInterval.setEnabled(enabled);
      binding.editTextMonthDay.setEnabled(enabled);
      binding.buttonWeekdays.setEnabled(enabled);
      binding.buttonStartDate.setEnabled(enabled);
    });
    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain));
      }
    });
  }

  private void bindText(EditText field, String key, String initial) {
    SavedStateHandle state = viewModel.getState();
    field.setText(state.contains(key) ? (String) state.get(key) : initial);
    state.set(key, field.getText().toString());
    field.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        state.set(key, s.toString());
      }
      @Override public void afterTextChanged(Editable s) {}
    });
  }

  private void updateSchedule() {
    int period = viewModel.getState().get("period");
    binding.textInputInterval.setVisibility(period == 0 ? View.GONE : View.VISIBLE);
    binding.textInputMonthDay.setVisibility(period == 3 ? View.VISIBLE : View.GONE);
    binding.buttonWeekdays.setVisibility(period == 2 ? View.VISIBLE : View.GONE);
    ArrayList<String> days = viewModel.getState().get("weekdays");
    ArrayList<String> labels = new ArrayList<>();
    for (String day : days) {
      labels.add(DayOfWeek.of(FormDataChore.WEEKDAYS.indexOf(day) + 1)
          .getDisplayName(TextStyle.FULL, LocaleUtil.getLocale()));
    }
    binding.buttonWeekdays.setText(labels.isEmpty() ? getString(R.string.property_chore_weekdays)
        : String.join(", ", labels));
    LocalDate date = LocalDate.parse(viewModel.getState().get("start_date"));
    binding.buttonStartDate.setText(getString(R.string.property_chore_start_date_value,
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(LocaleUtil.getLocale()))));
  }

  private void showWeekdays() {
    ArrayList<String> selected = viewModel.getState().get("weekdays");
    boolean[] checked = new boolean[7];
    String[] labels = new String[7];
    for (int i = 0; i < 7; i++) {
      checked[i] = selected.contains(FormDataChore.WEEKDAYS.get(i));
      labels[i] = DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL,
          LocaleUtil.getLocale());
    }
    weekdaysDialog = new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.property_chore_weekdays)
        .setMultiChoiceItems(labels, checked, (dialog, which, value) -> checked[which] = value)
        .setPositiveButton(R.string.action_save, (dialog, which) -> {
          ArrayList<String> days = new ArrayList<>();
          for (int i = 0; i < 7; i++) if (checked[i]) days.add(FormDataChore.WEEKDAYS.get(i));
          viewModel.getState().set("weekdays", days);
          updateSchedule();
        })
        .setNegativeButton(R.string.action_cancel, null).show();
  }

  private void showStartDate() {
    if (getChildFragmentManager().findFragmentByTag("chore_start_date") != null) return;
    LocalDate date = LocalDate.parse(viewModel.getState().get("start_date"));
    datePicker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(R.string.property_chore_start_date)
        .setSelection(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        .setTheme(R.style.ThemeOverlay_Grocy_DatePicker).build();
    bindDatePicker();
    datePicker.show(getChildFragmentManager(), "chore_start_date");
  }

  private void bindDatePicker() {
    datePicker.clearOnPositiveButtonClickListeners();
    datePicker.addOnPositiveButtonClickListener(value -> {
      viewModel.getState().set("start_date", java.time.Instant.ofEpochMilli(value)
          .atZone(ZoneOffset.UTC).toLocalDate().toString());
      if (binding != null) updateSchedule();
    });
  }

  private void saveChore() {
    if (Boolean.TRUE.equals(viewModel.getSavingLive().getValue())) return;
    if (!activity.isOnline()) {
      activity.showSnackbar(R.string.msg_no_connection, false);
      return;
    }
    if (viewModel.hasCreatedChore()) {
      viewModel.save(null);
      return;
    }
    binding.textInputName.setError(null);
    binding.textInputInterval.setError(null);
    binding.textInputMonthDay.setError(null);
    SavedStateHandle state = viewModel.getState();
    try {
      String type = FormDataChore.PERIOD_TYPES.get((Integer) state.get("period"));
      int interval = type.equals("manually") ? 1 : parsePositive(state.get("interval"), "period_interval");
      int monthDay = type.equals("monthly") ? parsePositive(state.get("month_day"), "period_days") : 1;
      JSONObject values = new JSONObject(FormDataChore.create(state.get("name"), state.get("description"),
          type, interval, monthDay, state.get("weekdays"), LocalDate.parse(state.get("start_date"))));
      activity.hideKeyboard();
      viewModel.save(values);
    } catch (IllegalArgumentException e) {
      if ("name".equals(e.getMessage())) {
        binding.textInputName.setError(getString(R.string.error_empty));
      } else if ("period_days".equals(e.getMessage())) {
        binding.textInputMonthDay.setError(getString(R.string.error_chore_month_day));
      } else if ("period_config".equals(e.getMessage())) {
        activity.showSnackbar(R.string.error_chore_weekdays, false);
      } else {
        binding.textInputInterval.setError(getString(R.string.error_chore_interval));
      }
    }
  }

  private int parsePositive(String text, String field) {
    try {
      int value = Integer.parseInt(text.trim());
      if (value > 0) return value;
    } catch (NumberFormatException ignored) {}
    throw new IllegalArgumentException(field);
  }

  @Override
  public void onDestroyView() {
    if (weekdaysDialog != null) weekdaysDialog.dismiss();
    if (datePicker != null) datePicker.clearOnPositiveButtonClickListeners();
    binding = null;
    super.onDestroyView();
  }

  @NonNull
  @Override
  public String toString() {
    return MasterChoreFragment.class.getSimpleName();
  }
}
