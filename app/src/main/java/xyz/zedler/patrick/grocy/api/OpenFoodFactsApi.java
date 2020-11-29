package xyz.zedler.patrick.grocy.api;

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

import java.util.Locale;

import xyz.zedler.patrick.grocy.R;

public class OpenFoodFactsApi {

    private final static String TAG = OpenFoodFactsApi.class.getSimpleName();

    public static String getUserAgent(Context context) {
        return "Grocy Android - v"
                + context.getString(R.string.versionName) + " - "
                + context.getString(R.string.url_github);
    }

    private static String getUrl(String command) {
        return "https://world.openfoodfacts.org/api/v0/" + command;
    }

    // PRODUCT

    /**
     * Returns a product json
     */
    public static String getProduct(String barcode) {
        return getUrl("product/" + barcode + ".json");
    }
}
