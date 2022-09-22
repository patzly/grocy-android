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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetDateBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.FormDataMasterProductCatDueDate;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.DATE;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class DateBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = DateBottomSheet.class.getSimpleName();

  public final static String DATE_TYPE = "date_type";
  public final static int PURCHASED_DATE = 1;
  public final static int DUE_DATE = 2;
  public final static int DUE_DAYS_DEFAULT = 3;

  private MainActivity activity;
  private FragmentBottomsheetDateBinding binding;
  private Bundle args;
  private Calendar calendar;
  private DateUtil dateUtil;
  private SimpleDateFormat dateFormatGrocy;
  private SimpleDateFormat dateFormatKeyboardInput;
  private SimpleDateFormat dateFormatKeyboardInputShort;
  private String defaultDueDays;
  private boolean keyboardInputEnabled;

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
    binding = FragmentBottomsheetDateBinding.inflate(inflater, container, false);
    activity = (MainActivity) requireActivity();
    args = requireArguments();

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);;
    keyboardInputEnabled = sharedPrefs.getBoolean(
        BEHAVIOR.DATE_KEYBOARD_INPUT,
        SETTINGS_DEFAULT.BEHAVIOR.DATE_KEYBOARD_INPUT
    );
    boolean reverseDateFormat = sharedPrefs.getBoolean(
        BEHAVIOR.DATE_KEYBOARD_REVERSE,
        SETTINGS_DEFAULT.BEHAVIOR.DATE_KEYBOARD_REVERSE
    );

    calendar = Calendar.getInstance();
    dateUtil = new DateUtil(requireContext());
    dateFormatGrocy = new SimpleDateFormat("yyyy-MM-dd");
    if (reverseDateFormat) {
      dateFormatKeyboardInput = new SimpleDateFormat("ddMMyy");
      dateFormatKeyboardInputShort = new SimpleDateFormat("ddMM");
      binding.textInputDate.setHint("DDMM | DDMMYY");
    } else {
      dateFormatKeyboardInput = new SimpleDateFormat("yyMMdd");
      dateFormatKeyboardInputShort = new SimpleDateFormat("MMdd");
    }

    String selectedDate = args.getString(Constants.ARGUMENT.SELECTED_DATE);
    defaultDueDays = args.getString(Constants.ARGUMENT.DEFAULT_DAYS_FROM_NOW);

    binding.frameHelpButton.setOnClickListener(v -> {
      if (keyboardInputEnabled) {
        binding.helpKeyboard.setVisibility(View.VISIBLE);
      } else {
        binding.help.setVisibility(View.VISIBLE);
      }
    });
    binding.help.setOnClickListener(v -> navigateToSettingsCatBehavior());
    binding.helpKeyboard.setOnClickListener(v -> navigateToSettingsCatBehavior());

    if (keyboardInputEnabled) {
      binding.linearBodyPicker.setVisibility(View.GONE);
      binding.linearBodyKeyboard.setVisibility(View.VISIBLE);

      if (selectedDate == null || selectedDate.equals(DATE.NEVER_OVERDUE)) {
        binding.editTextDate.setText("");
      } else {
        try {
          Date date = dateFormatGrocy.parse(selectedDate);
          if (date != null) {
            calendar.setTime(date);
            binding.editTextDate.setText(dateFormatKeyboardInput.format(calendar.getTime()));
          } else {
            binding.editTextDate.setText("");
          }
        } catch (ParseException e) {
          binding.editTextDate.setText("");
        }
      }

      if (savedInstanceState == null) {
        new Handler().postDelayed(() -> activity.showKeyboard(binding.editTextDate), 50);
      }

      updateDateHint();
      binding.editTextDate.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }
        @Override
        public void afterTextChanged(Editable editable) {
          updateDateHint();
        }
      });
      binding.editTextDate.setOnEditorActionListener(
          (TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && getTextFieldDate() != null) {
              dismiss();
              return true;
            }
            return false;
          });

      ActionButton moreMonth = reverseDateFormat ? binding.moreMonthReverse : binding.moreMonth;
      ActionButton lessMonth = reverseDateFormat ? binding.lessMonthReverse : binding.lessMonth;
      binding.linearMonth.setVisibility(reverseDateFormat ? View.GONE : View.VISIBLE);
      binding.linearMonthReverse.setVisibility(reverseDateFormat ? View.VISIBLE : View.GONE);

      moreMonth.setOnClickListener(view -> {
        Date date = getTextFieldDate();
        if (date != null) {
          String input = binding.editTextDate.getText() != null
              ? binding.editTextDate.getText().toString().trim()
              : "";
          calendar.setTime(date);
          calendar.add(Calendar.MONTH, 1);
          if (input.length() == 6) {
            binding.editTextDate.setText(dateFormatKeyboardInput.format(calendar.getTime()));
          } else if (input.length() == 4) {
            binding.editTextDate.setText(dateFormatKeyboardInputShort.format(calendar.getTime()));
          }
        }
      });
      binding.moreDay.setOnClickListener(view -> {
        Date date = getTextFieldDate();
        if (date != null) {
          String input = binding.editTextDate.getText() != null
              ? binding.editTextDate.getText().toString().trim()
              : "";
          calendar.setTime(date);
          calendar.add(Calendar.DAY_OF_MONTH, 1);
          if (input.length() == 6) {
            binding.editTextDate.setText(dateFormatKeyboardInput.format(calendar.getTime()));
          } else if (input.length() == 4) {
            binding.editTextDate.setText(dateFormatKeyboardInputShort.format(calendar.getTime()));
          }
        }
      });
      lessMonth.setOnClickListener(view -> {
        Date date = getTextFieldDate();
        if (date != null) {
          String input = binding.editTextDate.getText() != null
              ? binding.editTextDate.getText().toString().trim()
              : "";
          calendar.setTime(date);
          calendar.add(Calendar.MONTH, -1);
          if (input.length() == 6) {
            binding.editTextDate.setText(dateFormatKeyboardInput.format(calendar.getTime()));
          } else if (input.length() == 4) {
            binding.editTextDate.setText(dateFormatKeyboardInputShort.format(calendar.getTime()));
          }
        }
      });
      binding.lessDay.setOnClickListener(view -> {
        Date date = getTextFieldDate();
        if (date != null) {
          String input = binding.editTextDate.getText() != null
              ? binding.editTextDate.getText().toString().trim()
              : "";
          calendar.setTime(date);
          calendar.add(Calendar.DAY_OF_MONTH, -1);
          if (input.length() == 6) {
            binding.editTextDate.setText(dateFormatKeyboardInput.format(calendar.getTime()));
          } else if (input.length() == 4) {
            binding.editTextDate.setText(dateFormatKeyboardInputShort.format(calendar.getTime()));
          }
        }
      });
      binding.clear.setOnClickListener(v -> {
        binding.editTextDate.setText("");
        activity.showKeyboard(binding.editTextDate);
      });

    } else {
      initDatePickerLayout();
      fillDatePickerForm(selectedDate);
    }

    setSkipCollapsedInPortrait();

    return binding.getRoot();
  }

  private void initDatePickerLayout() {
    setDatePickerTextColor(binding.datePicker);

    binding.checkboxNeverExpires.setOnCheckedChangeListener(
        (v, isChecked) -> binding.datePicker.animate()
            .alpha(isChecked ? 0.5f : 1)
            .withEndAction(() -> binding.datePicker.setEnabled(!isChecked))
            .setDuration(200)
            .start()
    );

    if (args.getInt(DATE_TYPE) == DUE_DATE) {
      binding.linearNeverExpires.setOnClickListener(
          v -> binding.checkboxNeverExpires.setChecked(!binding.checkboxNeverExpires.isChecked())
      );
      if (!args.getBoolean(ARGUMENT.SHOW_OPTION_NEVER_EXPIRES, true)) {
        binding.linearNeverExpires.setVisibility(View.GONE);
      }
    } else if (args.getInt(DATE_TYPE) == PURCHASED_DATE) {
      binding.linearNeverExpires.setVisibility(View.GONE);
      binding.title.setText(R.string.property_purchased_date);
    } else {
      if (!(args.getInt(FormDataMasterProductCatDueDate.DUE_DAYS_ARG, -1)
          == FormDataMasterProductCatDueDate.DUE_DAYS)) {
        binding.linearNeverExpires.setVisibility(View.GONE);
      }
      binding.title.setText(R.string.property_due_days_default);
    }
    binding.reset.setOnClickListener(
        v -> {
          calendar = Calendar.getInstance();
          fillDatePickerForm(null);
        }
    );
    binding.save.setOnClickListener(
        v -> dismiss()
    );
  }

  private void fillDatePickerForm(String selectedBestBeforeDate) {
    if (selectedBestBeforeDate != null
        && selectedBestBeforeDate.equals(Constants.DATE.NEVER_OVERDUE)) {

      binding.datePicker.setEnabled(false);
      binding.datePicker.setAlpha(0.5f);
      binding.checkboxNeverExpires.setChecked(true);

    } else if (selectedBestBeforeDate != null) {

      try {
        Date date = dateFormatGrocy.parse(selectedBestBeforeDate);
        if (date != null) {
          calendar.setTime(date);
        }
      } catch (ParseException e) {
        fillDatePickerForm(null);
        activity.showSnackbar(activity.getString(R.string.error_undefined));
        return;
      }
      binding.datePicker.setEnabled(true);
      binding.datePicker.setAlpha(1.0f);
      binding.checkboxNeverExpires.setChecked(false);

    } else if (defaultDueDays != null) {

      if (Integer.parseInt(defaultDueDays) < 0) {
        binding.datePicker.setEnabled(false);
        binding.datePicker.setAlpha(0.5f);
        binding.checkboxNeverExpires.setChecked(true);
      } else {
        binding.datePicker.setEnabled(true);
        binding.datePicker.setAlpha(1.0f);
        binding.checkboxNeverExpires.setChecked(false);
        calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(defaultDueDays));
      }

    } else {

      binding.datePicker.setEnabled(false);
      binding.datePicker.setAlpha(0.5f);
      binding.checkboxNeverExpires.setChecked(true);

    }

    binding.datePicker.updateDate(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    );
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    String date;

    if (!keyboardInputEnabled) {
      if (!binding.checkboxNeverExpires.isChecked()) {
        calendar.set(
            binding.datePicker.getYear(),
            binding.datePicker.getMonth(),
            binding.datePicker.getDayOfMonth()
        );
        date = dateFormatGrocy.format(calendar.getTime());
      } else {
        date = Constants.DATE.NEVER_OVERDUE;
      }
    } else {
      Date textFieldDate = getTextFieldDate();
      if (textFieldDate != null) {
        date = dateFormatGrocy.format(textFieldDate);
      } else {
        date = Constants.DATE.NEVER_OVERDUE;
      }
    }

    BaseFragment currentFragment = activity.getCurrentFragment();
    if (args.getInt(DATE_TYPE) == DUE_DATE) {
      currentFragment.selectDueDate(date);
    } else if (args.getInt(DATE_TYPE) == PURCHASED_DATE) {
      currentFragment.selectPurchasedDate(date);
    } else {
      activity.getCurrentFragment().saveInput(
          date.equals(Constants.DATE.NEVER_OVERDUE)
              ? String.valueOf(-1)
              : String.valueOf(DateUtil.getDaysFromNow(date)),
          requireArguments()
      );
    }
    currentFragment.onBottomSheetDismissed();
  }

  private void updateDateHint() {
    Date date = getTextFieldDate();
    Date neverOverdueDate = null;
    try {
      neverOverdueDate = dateFormatGrocy.parse(DATE.NEVER_OVERDUE);
    } catch (ParseException ignored) { }
    if (date != null && date.equals(neverOverdueDate)) {
      binding.textInputHint.setText(
          getString(R.string.subtitle_date_from_input, getString(R.string.subtitle_never_overdue))
      );
    } else if (date != null) {
      binding.textInputHint.setText(getString(
          R.string.subtitle_date_from_input,
          dateUtil.getLocalizedDate(dateFormatGrocy.format(date), DateUtil.FORMAT_SHORT)
      ));
    } else {
      binding.textInputHint.setText(getString(
          R.string.subtitle_date_from_input,
          getString(R.string.error_invalid_date_format)
      ));
    }
  }

  private Date getTextFieldDate() {
    String input = binding.editTextDate.getText() != null
        ? binding.editTextDate.getText().toString().trim()
        : "";
    Date date;
    if (input.length() == 0) {
      date = parseDate(dateFormatGrocy, Constants.DATE.NEVER_OVERDUE);
    } else if (input.length() == 6) {
      date = parseDate(dateFormatKeyboardInput, input);
    } else if (input.length() == 4) {
      date = parseDate(dateFormatKeyboardInputShort, input);
      if (date != null) {
        calendar.setTime(date);
        calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
        if (calendar.before(Calendar.getInstance())) {
          calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)+1);
        }
        date = calendar.getTime();
      }
    } else {
      date = null;
    }
    return date;
  }

  private Date parseDate(SimpleDateFormat dateFormat, String date) {
    try {
      return dateFormat.parse(date);
    } catch (ParseException e) {
      return null;
    }
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

  public void navigateToSettingsCatBehavior() {
    dismiss();
    navigateDeepLink(R.string.deep_link_settingsCatBehaviorFragment);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
