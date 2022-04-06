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

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants.DATE;

public class DateUtil {

  private static final String TAG = DateUtil.class.getSimpleName();

  public static final int FORMAT_LONG = 2;
  public static final int FORMAT_MEDIUM = 1;
  public static final int FORMAT_SHORT = 0;

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd", Locale.ENGLISH
  );
  private static final SimpleDateFormat DATE_FORMAT_WITH_TIME = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH
  );
  private final Context context;

  public DateUtil(Context context) {
    this.context = context;
  }

  public static Date getDate(String dateString) {
    return getDate(dateString, false);
  }

  public static Date getDate(String dateString, boolean withTime) {
    if (dateString == null) {
      return null;
    }
    Date date = null;
    try {
      date = withTime ? DATE_FORMAT_WITH_TIME.parse(dateString) : DATE_FORMAT.parse(dateString);
    } catch (ParseException e) {
      Log.e(TAG, "getDate: ");
    }
    return date;
  }

  public static String getDateStringToday() {
    return DATE_FORMAT.format(getCurrentDate());
  }

  public static int getDaysFromNow(String dateString) {
    return getDaysFromNow(dateString, DATE_FORMAT);
  }

  public static int getDaysFromNowWithTime(String dateString) {
    return getDaysFromNow(dateString, DATE_FORMAT_WITH_TIME);
  }

  public static int getDaysFromNow(String dateString, SimpleDateFormat format) {
    if (dateString == null || dateString.isEmpty()) {
      return 0;
    }
    Date date = null;
    try {
      date = format.parse(dateString);
    } catch (ParseException e) {
      Log.e(TAG, "getDaysFromNow: " + e);
    }
    if (date == null) {
      return 0;
    }
    long diff = date.getTime() - getCurrentDate().getTime();
    return (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
  }

  public static String getTodayWithDaysAdded(int daysToAdd) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);
    return DATE_FORMAT.format(calendar.getTime());
  }

  private static Date getCurrentDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static Date getCurrentDateWithTime() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public String getCurrentDateWithTimeStr() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MILLISECOND, 0);
    return DATE_FORMAT_WITH_TIME.format(cal.getTime());
  }

  public boolean isTimeLessThanOneMinuteAway(String dateWithTimeStr) {
    if (dateWithTimeStr == null) {
      return true;
    }
    Date currentDateWithTime = getCurrentDateWithTime();
    Date askedDateWithTime = null;
    try {
      askedDateWithTime = DATE_FORMAT_WITH_TIME.parse(dateWithTimeStr);
    } catch (ParseException e) {
      Log.e(TAG, "isTimeMoreThanOneMinuteAway: " + e);
    }
    if (askedDateWithTime == null) {
      return true;
    }
    long diff = currentDateWithTime.getTime() - askedDateWithTime.getTime();
    long secondsDiff = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS);
    return Math.abs(secondsDiff) < 60;
  }

  public String getLocalizedDate(String dateString, int format) {
    if (dateString == null || dateString.isEmpty()) {
      return context.getString(R.string.date_unknown);
    }
    if (dateString.equals(DATE.NEVER_OVERDUE)) {
      return context.getString(R.string.subtitle_never_overdue);
    }
    Date date = null;
    try {
      date = DATE_FORMAT.parse(dateString);
    } catch (ParseException e) {
      Log.e(TAG, "getLocalizedDate: " + e);
    }
    if (date == null) {
      return "";
    }
    String localized;
    if (format == FORMAT_LONG) {
      localized = android.text.format.DateFormat.getLongDateFormat(context).format(date);
    } else if (format == FORMAT_MEDIUM) {
      localized = android.text.format.DateFormat.getMediumDateFormat(context).format(date);
    } else {
      localized = android.text.format.DateFormat.getDateFormat(context).format(date);
    }
    return localized;
  }

  public String getLocalizedDate(String dateString) {
    return getLocalizedDate(dateString, FORMAT_LONG);
  }

  public String getHumanForDaysFromNow(String dateString) {
    return getHumanForDaysFromNow(dateString, false);
  }

  public String getHumanForDaysFromNow(String dateString, boolean withTime) {
    if (dateString == null || dateString.isEmpty()) {
      return context.getString(R.string.date_unknown);
    } else if (withTime && dateString.equals(DATE.NEVER_OVERDUE_WITH_TIME)
        || !withTime && dateString.equals(Constants.DATE.NEVER_OVERDUE)) {
      return context.getString(R.string.date_never);
    } else {
      return getHumanFromToday(withTime ? getDaysFromNowWithTime(dateString)
          : getDaysFromNow(dateString));
    }
  }

  public String getHumanFromToday(int days) {
    if (days == 0) {
      return context.getString(R.string.date_today);
    } else if (days > 0) {
      if (days < 30) {
        return context.getResources().getQuantityString(
            R.plurals.date_days_from_now,
            days, days
        );
      } else if (days < 365) {
        return context.getResources().getQuantityString(
            R.plurals.date_months_from_now,
            days / 30, days / 30
        );
      } else {
        return context.getResources().getQuantityString(
            R.plurals.date_years_from_now,
            days / 365, days / 365
        );
      }
    } else {
      if (days > -30) {
        return context.getResources().getQuantityString(
            R.plurals.date_days_ago,
            days * -1, days * -1
        );
      }
      if (days > -365) {
        return context.getResources().getQuantityString(
            R.plurals.date_months_ago,
            days * -1 / 30, days * -1 / 30
        );
      } else {
        return context.getResources().getQuantityString(
            R.plurals.date_years_ago,
            days * -1 / 365, days * -1 / 365
        );
      }
    }
  }

  public String getHumanDuration(int days) {
    if (days == 0) {
      return context.getString(R.string.date_never);
    } else if (days > 0) {
      if (days < 30) {
        return context.getResources().getQuantityString(R.plurals.date_days, days, days);
      }
      if (days < 365) {
        int months = Math.round((float) days / 30);
        return context.getResources().getQuantityString(
            R.plurals.date_months, months, months
        );
      } else {
        // Check if days are about the same as to the never expiring date
        Calendar calendarNever = Calendar.getInstance();
        try {
          Date dateNever = DATE_FORMAT.parse(Constants.DATE.NEVER_OVERDUE);
          if (dateNever != null) {
            calendarNever.setTime(dateNever);
          }
        } catch (ParseException e) {
          Log.i(TAG, "getHumanDuration: " + e);
        }
        long msDiff = calendarNever.getTime().getTime() - getCurrentDate().getTime();
        long daysToNever = TimeUnit.DAYS.convert(msDiff, TimeUnit.MILLISECONDS);
        if (days >= daysToNever - 100) {
          // deviation in server calculation possible
          return context.getString(R.string.date_unlimited);
        } else {
          int years = Math.round((float) days / 365);
          return context.getResources().getQuantityString(
              R.plurals.date_years, years, years
          );
        }
      }
    } else {
      return context.getString(R.string.date_unknown);
    }
  }
}
