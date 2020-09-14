package xyz.zedler.patrick.grocy.view;

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
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.StringRes;

import xyz.zedler.patrick.grocy.databinding.ViewSettingEntryCardBinding;

public class SettingEntryCard extends LinearLayout {

    public SettingEntryCard(Context context) {
        super(context);
    }

    public SettingEntryCard(Context context, @StringRes int text) {
        super(context);
        ViewSettingEntryCardBinding binding = ViewSettingEntryCardBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.text.setText(text);
    }
}
