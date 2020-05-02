package xyz.zedler.patrick.grocy.util;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class NumUtil {

    public static String trim(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("###.##");
        return decimalFormat.format(value);
    }

    public static String trimPrice(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return decimalFormat.format(value);
    }

    public static double stringToDouble(String input) {
        double num;
        if(!input.equals("")) {
            if(input.contains(",")) {
                List<String> stringWithComma = Arrays.asList(input.split(","));
                if(stringWithComma.size() > 1) { // with comma
                    num = Double.parseDouble(stringWithComma.get(0) + "." + stringWithComma.get(1));
                } else {
                    num = Double.parseDouble(stringWithComma.get(0));
                }
            } else {
                num = Double.parseDouble(input);
            }
        } else {
            num = 0;
        }
        return num;
    }
}
