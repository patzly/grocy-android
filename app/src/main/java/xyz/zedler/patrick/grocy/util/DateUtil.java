package xyz.zedler.patrick.grocy.util;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import xyz.zedler.patrick.grocy.R;

public class DateUtil {

    private static final String TAG = DateUtil.class.getSimpleName();

    public static final int FORMAT_LONG = 2;
    public static final int FORMAT_MEDIUM = 1;
    public static final int FORMAT_SHORT = 0;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd", Locale.ENGLISH
    );
    private Context context;

    public DateUtil(Context context) {
        this.context = context;
    }

    public static Date getDate(String dateString) {
        if(dateString == null) return null;
        Date date = null;
        try {
            date = DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "getDate: ");
        }
        return date;
    }

    public static int getDaysFromNow(String dateString) {
        if(dateString == null) return 0;
        Date current = Calendar.getInstance().getTime();
        Date date = null;
        try {
            date = DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "getDaysAway: ");
        }
        if(date == null) return 0;
        return ((int)(date.getTime() / 86400000) - (int)(current.getTime() / 86400000)) + 1;
    }

    public static String getTodayWithDaysAdded(int daysToAdd) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);
        return DATE_FORMAT.format(calendar.getTime());
    }

    public String getLocalizedDate(String dateString, int format) {
        if(dateString == null || dateString.isEmpty()) {
            return context.getString(R.string.date_unknown);
        }
        Date date = null;
        try {
            date = DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "getLocalizedDate: ");
        }
        if(date == null) return "";
        String localized;
        if(format == FORMAT_LONG) {
            localized = android.text.format.DateFormat.getLongDateFormat(context).format(date);
        } else if(format == FORMAT_MEDIUM) {
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
        if(dateString == null || dateString.isEmpty()) {
            return context.getString(R.string.date_unknown);
        } else if(dateString.equals(Constants.DATE.NEVER_EXPIRES)) {
            return context.getString(R.string.date_never);
        } else {
            return getHumanFromToday(getDaysFromNow(dateString));
        }
    }

    public String getHumanFromToday(int days) {
        if(days == 0) {
            return context.getString(R.string.date_today);
        } else if(days > 0) {
            if(days == 1) {
                return context.getString(R.string.date_tomorrow);
            } else {
                if(days < 30) {
                    return context.getString(R.string.date_days_from_now, days);
                } else {
                    if(days < 60) {
                        return context.getString(R.string.date_month_from_now);
                    } else {
                        if(days < 365) {
                            return context.getString(
                                    R.string.date_months_from_now,
                                    days / 30
                            );
                        } else {
                            if(days < 700) { // how many days do you understand as two years?
                                return context.getString(R.string.date_year_from_now);
                            } else {
                                return context.getString(
                                        R.string.date_years_from_now,
                                        days / 365
                                );
                            }
                        }
                    }
                }
            }
        } else {
            if(days == -1) {
                return context.getString(R.string.date_yesterday);
            } else {
                if(days > -30) {
                    return context.getString(R.string.date_days_ago, -1 * days);
                } else {
                    if(days > -60) {
                        return context.getString(R.string.date_month_ago);
                    } else {
                        if(days > -365) {
                            return context.getString(
                                    R.string.date_months_ago,
                                    days / 30 * -1
                            );
                        } else {
                            if(days > -700) {
                                return context.getString(R.string.date_year_ago);
                            } else {
                                return context.getString(
                                        R.string.date_years_ago,
                                        days / 365 * -1
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    public String getHumanFromDays(int days) {
        if(days == 0) {
            return context.getString(R.string.date_never);
        } else if(days > 0) {
            if(days == 1) {
                return context.getString(R.string.date_day_one);
            } else {
                if(days < 30) {
                    return context.getString(R.string.date_days, days);
                } else {
                    if(days < 60) {
                        return context.getString(R.string.date_month_one);
                    } else {
                        if(days < 365) {
                            return context.getString(R.string.date_months, days / 30);
                        } else {
                            if(days < 700) { // how many days do you understand as two years?
                                return context.getString(R.string.date_year_one);
                            } else {
                                // Check if days are about the same as to the never expiring date
                                Calendar calendarNever = Calendar.getInstance();
                                try {
                                    Date dateNever = DATE_FORMAT.parse(
                                            Constants.DATE.NEVER_EXPIRES
                                    );
                                    if(dateNever != null) calendarNever.setTime(dateNever);
                                } catch (ParseException e) {
                                    Log.i(TAG, "getHumanFromDays: " + e);
                                }
                                long msDiff = calendarNever.getTimeInMillis()
                                        - Calendar.getInstance().getTimeInMillis();
                                long daysToNever = TimeUnit.MILLISECONDS.toDays(msDiff);
                                if(days <= daysToNever + 100 || days >= daysToNever - 100) {
                                    // deviation in server calculation possible
                                    return context.getString(R.string.date_unlimited);
                                } else {
                                    return context.getString(
                                            R.string.date_years, days / 365
                                    );
                                }
                            }
                        }
                    }
                }
            }
        } else return context.getString(R.string.date_unknown);
    }
}
