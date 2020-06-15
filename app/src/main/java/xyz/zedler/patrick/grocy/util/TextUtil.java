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

public class TextUtil {

    public static CharSequence trimCharSequence(CharSequence source) {
        if(source == null || source.length() == 0) return null;
        int i = 0;
        try {
            while (i < source.length() && Character.isWhitespace(source.charAt(i))) {
                i++;
            }
            int j = source.length()-1;
            while (j >= 0 && j > i && Character.isWhitespace(source.charAt(j))) {
                j--;
            }
            return source.subSequence(i, j + 1);
        } catch (StringIndexOutOfBoundsException e) {
            return null;
        }
    }
}
