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

package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

public class CustomAutoCompleteTextView extends MaterialAutoCompleteTextView {

    private Runnable onEnterPressListener;
    private Runnable onTabPressListener;

    public CustomAutoCompleteTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_TAB && onTabPressListener != null) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_TAB && onTabPressListener != null) {
            onTabPressListener.run();
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_ENTER && onEnterPressListener != null) {
            onEnterPressListener.run();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void setOnTabPressListener(Runnable onTabPressListener) {
        this.onTabPressListener = onTabPressListener;
    }

    public void setOnEnterPressListener(Runnable onEnterPressListener) {
        this.onEnterPressListener = onEnterPressListener;
    }
}
