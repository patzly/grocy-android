package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.util.TypedValue;

public class UnitUtil {

    public static int getDp(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
