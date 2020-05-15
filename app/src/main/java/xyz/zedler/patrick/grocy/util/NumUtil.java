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

    public static String formatPrice(String value) {
        return trimPrice(stringToDouble(value));
    }

    public static double stringToDouble(String input) {
        double num;
        if(!input.equals("")) {
            if(input.contains(",")) {
                List<String> stringWithComma = Arrays.asList(input.split(","));
                if (stringWithComma.size() > 1) { // with comma
                    input = stringWithComma.get(0) + "." + stringWithComma.get(1);
                } else {
                    input = stringWithComma.get(0);
                }
            }
            if(input.equals("")) return 0;
            num = Double.parseDouble(input);
        } else {
            num = 0;
        }
        return num;
    }
}
