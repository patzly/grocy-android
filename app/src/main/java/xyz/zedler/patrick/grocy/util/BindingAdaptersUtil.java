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
 * along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import androidx.annotation.ColorInt;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputLayout;

public class BindingAdaptersUtil {

    @BindingAdapter({"errorText"})
    public static void setErrorMessage(TextInputLayout view, MutableLiveData<Integer> errorMsg) {
        if(errorMsg.getValue() != null) {
            view.setError(view.getContext().getString(errorMsg.getValue()));
        } else if(view.isErrorEnabled()) {
            view.setErrorEnabled(false);
        }
    }

    @BindingAdapter({"progressBackgroundColor"})
    public static void setProgressBackgroundColor(SwipeRefreshLayout view, @ColorInt int color) {
        view.setProgressBackgroundColorSchemeColor(color);
    }

    @BindingAdapter({"progressForegroundColor"})
    public static void setColorSchemeColors(SwipeRefreshLayout view, @ColorInt int color) {
        view.setColorSchemeColors(color);
    }
}
