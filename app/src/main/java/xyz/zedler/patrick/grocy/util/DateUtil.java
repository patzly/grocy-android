package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import xyz.zedler.patrick.grocy.R;

public class DateUtil {

    private static final String TAG = "DateUtil";
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

    public String getLocalizedDate(String dateString) {
        if(dateString == null) return "";
        Date date = null;
        try {
            date = DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "getLocalizedDate: ");
        }
        if(date == null) return "";
        java.text.DateFormat localized = android.text.format.DateFormat.getLongDateFormat(context);
        return localized.format(date);
    }

    public String getHumanForDaysFromNow(String dateString) {
        if(dateString == null) {
            return context.getString(R.string.date_unknown);
        } else if(dateString.equals("2999-12-31")) {
            return context.getString(R.string.date_never);
        } else {
            return getHumanFromDays(getDaysFromNow(dateString));
        }
    }

    public String getHumanFromDays(int days) {
        if(days == 0) {
            return context.getString(R.string.date_today);
        } else if(days > 0) {
            if(days == 1) {
                return context.getString(R.string.date_tomorrow);
            } else {
                if(days < 30) {
                    return context.getString(
                            R.string.date_from_now,
                            context.getString(R.string.date_days, days)
                    );
                } else {
                    if(days < 60) {
                        return context.getString(
                                R.string.date_from_now,
                                context.getString(R.string.date_month)
                        );
                    } else {
                        if(days < 365) {
                            return context.getString(
                                    R.string.date_from_now,
                                    context.getString(
                                            R.string.date_months,
                                            days / 30
                                    )
                            );
                        } else {
                            if(days < 700) { // how many days do you understand as two years?
                                return context.getString(
                                        R.string.date_from_now,
                                        context.getString(R.string.date_year)
                                );
                            } else {
                                return context.getString(
                                        R.string.date_from_now,
                                        context.getString(
                                                R.string.date_years,
                                                days / 365
                                        )
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
                    return context.getString(
                            R.string.date_ago,
                            context.getString(R.string.date_days, -1 * days)
                    );
                } else {
                    if(days > -60) {
                        return context.getString(
                                R.string.date_ago,
                                context.getString(R.string.date_month)
                        );
                    } else {
                        if(days > -365) {
                            return context.getString(
                                    R.string.date_ago,
                                    context.getString(
                                            R.string.date_months,
                                            days / 30 * -1
                                    )
                            );
                        } else {
                            if(days > -700) {
                                return context.getString(
                                        R.string.date_ago,
                                        context.getString(R.string.date_year)
                                );
                            } else {
                                return context.getString(
                                        R.string.date_ago,
                                        context.getString(
                                                R.string.date_years,
                                                days / 365 * -1
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }
    }
}
