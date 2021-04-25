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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextUtil {

    private final static String TAG = TextUtil.class.getSimpleName();

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

    @NonNull
    public static String readFromFile(Context context, String file) {
        // TODO: use this in all asset usages and improve fetching of debug setting
        boolean debug = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.PREF.DEBUG, false
        );
        StringBuilder text = new StringBuilder();
        try {
            InputStream inputStream = context.getAssets().open(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            for (String line; (line = bufferedReader.readLine()) != null;) {
                text.append(line).append('\n');
            }
            text.deleteCharAt(text.length() - 1);
            inputStream.close();
        } catch (FileNotFoundException e) {
            if (debug) Log.e(TAG, "readFromFile: \"" + file + "\" not found!");
            return "";
        } catch (Exception e) {
            if (debug) Log.e(TAG, "readFromFile: " + e.toString());
            return "";
        }
        return text.toString();
    }
}
