package xyz.zedler.patrick.grocy.util;

import java.text.DecimalFormat;

public class NumUtil {

    public static String trim(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("###.##");
        return decimalFormat.format(value);
    };
}
