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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class DateBottomSheet extends BaseBottomSheet {

  private final static String TAG = DateBottomSheet.class.getSimpleName();

  public final static String DATE_TYPE = "date_type";
  public final static int PURCHASED_DATE = 1;
  public final static int DUE_DATE = 2;

  private MainActivity activity;
  private Bundle args;
  private Calendar calendar;
  private SimpleDateFormat dateFormat;
  private String defaultDueDays;
  private DatePicker datePicker;
  private MaterialCheckBox neverExpires;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @SuppressLint("SimpleDateFormat")
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(
        R.layout.fragment_bottomsheet_due_date, container, false
    );

    activity = (MainActivity) requireActivity();
    args = requireArguments();

    calendar = Calendar.getInstance();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    datePicker = view.findViewById(R.id.date_picker_bbd);
    setDatePickerTextColor(datePicker);

    neverExpires = view.findViewById(R.id.checkbox_bbd_never_expires);
    neverExpires.setOnCheckedChangeListener(
        (v, isChecked) -> datePicker.animate()
            .alpha(isChecked ? 0.5f : 1)
            .withEndAction(() -> datePicker.setEnabled(!isChecked))
            .setDuration(200)
            .start()
    );

    if (args.getInt(DATE_TYPE) == DUE_DATE) {
      view.findViewById(R.id.linear_bbd_never_expires).setOnClickListener(
          v -> neverExpires.setChecked(!neverExpires.isChecked())
      );
    } else {
      view.findViewById(R.id.linear_bbd_never_expires).setVisibility(View.GONE);
      ((TextView) view.findViewById(R.id.text_bbd_title))
          .setText(R.string.property_purchased_date);
    }
    view.findViewById(R.id.button_bbd_reset).setOnClickListener(
        v -> {
          calendar = Calendar.getInstance();
          fillForm(null);
        }
    );
    view.findViewById(R.id.button_bbd_save).setOnClickListener(
        v -> dismiss()
    );

    String selectedDate = args.getString(Constants.ARGUMENT.SELECTED_DATE);
    defaultDueDays = args.getString(Constants.ARGUMENT.DEFAULT_DAYS_FROM_NOW);

    fillForm(selectedDate);

    return view;
  }

  private void fillForm(String selectedBestBeforeDate) {
    if (selectedBestBeforeDate != null
        && selectedBestBeforeDate.equals(Constants.DATE.NEVER_OVERDUE)) {

      datePicker.setEnabled(false);
      datePicker.setAlpha(0.5f);
      neverExpires.setChecked(true);

    } else if (selectedBestBeforeDate != null) {

      try {
        Date date = dateFormat.parse(selectedBestBeforeDate);
        if (date != null) {
          calendar.setTime(date);
        }
      } catch (ParseException e) {
        fillForm(null);
        activity.showMessage(activity.getString(R.string.error_undefined));
        return;
      }
      datePicker.setEnabled(true);
      datePicker.setAlpha(1.0f);
      neverExpires.setChecked(false);

    } else if (defaultDueDays != null) {

      if (Integer.parseInt(defaultDueDays) < 0) {
        datePicker.setEnabled(false);
        datePicker.setAlpha(0.5f);
        neverExpires.setChecked(true);
      } else {
        datePicker.setEnabled(true);
        datePicker.setAlpha(1.0f);
        neverExpires.setChecked(false);
        calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(defaultDueDays));
      }

    } else {

      datePicker.setEnabled(false);
      datePicker.setAlpha(0.5f);
      neverExpires.setChecked(true);

    }

    datePicker.updateDate(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    );
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    String date;
    if (!neverExpires.isChecked()) {
      calendar.set(
          datePicker.getYear(),
          datePicker.getMonth(),
          datePicker.getDayOfMonth()
      );
      date = dateFormat.format(calendar.getTime());
    } else {
      date = Constants.DATE.NEVER_OVERDUE;
    }

    BaseFragment currentFragment = activity.getCurrentFragment();
    if (args.getInt(DATE_TYPE) == DUE_DATE) {
      currentFragment.selectDueDate(date);
    } else {
      currentFragment.selectPurchasedDate(date);
    }
    currentFragment.onBottomSheetDismissed();
  }

  private void setDatePickerTextColor(DatePicker datePicker) {
    View linearLayout = datePicker.getChildAt(0);
    if (!(linearLayout instanceof LinearLayout)
        || ((LinearLayout) linearLayout).getChildCount() == 0) return;
    View linearLayoutPickersView = ((LinearLayout) linearLayout)
        .getChildAt(0);
    if (!(linearLayoutPickersView instanceof LinearLayout)) return;

    @ColorInt int color = ContextCompat.getColor(requireContext(), R.color.on_background);

    LinearLayout linearLayoutPickers = (LinearLayout) linearLayoutPickersView;
    for (int i=0; i < linearLayoutPickers.getChildCount(); i++) {
      NumberPicker numberPicker = (NumberPicker) linearLayoutPickers.getChildAt(i);
      try {
        @SuppressLint("PrivateApi")
        Field selectorWheelPaintField = numberPicker.getClass()
            .getDeclaredField("mSelectorWheelPaint");
        selectorWheelPaintField.setAccessible(true);
        Paint paint = (Paint) selectorWheelPaintField.get(numberPicker);
        assert paint != null;
        paint.setColor(color);
      }
      catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException ignored) {}

      for(int j = 0; j < numberPicker.getChildCount(); j++){
        View child = numberPicker.getChildAt(j);
        if(child instanceof EditText) ((EditText) child).setTextColor(color);
      }
      numberPicker.invalidate();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
