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
        if(input.isEmpty()) return 0;
        return Double.parseDouble(input.replace(",", "."));
    }

    public static boolean isStringInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
