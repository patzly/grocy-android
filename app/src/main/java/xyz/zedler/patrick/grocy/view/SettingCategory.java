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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import xyz.zedler.patrick.grocy.databinding.ViewSettingCategoryBinding;
import xyz.zedler.patrick.grocy.databinding.ViewSettingCategoryOnelineBinding;

public class SettingCategory extends LinearLayout {

    public SettingCategory(Context context) {
        super(context);
    }

    public SettingCategory(
            Context context,
            @StringRes int title,
            @DrawableRes int drawable,
            @Nullable Runnable onClick
    ) {
        super(context);
        ViewSettingCategoryOnelineBinding binding = ViewSettingCategoryOnelineBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.image.setImageDrawable(ContextCompat.getDrawable(context, drawable));
        if(onClick != null) {
            binding.getRoot().setOnClickListener(v -> onClick.run());
        }
    }

    public SettingCategory(
            Context context,
            String title,
            String description,
            @DrawableRes int drawable,
            @Nullable Runnable onClick
    ) {
        super(context);
        ViewSettingCategoryBinding binding = ViewSettingCategoryBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        binding.image.setImageDrawable(ContextCompat.getDrawable(context, drawable));
        if(onClick != null) {
            binding.getRoot().setOnClickListener(v -> onClick.run());
        }
    }

    public SettingCategory(
            Context context,
            @StringRes int title,
            @StringRes int description,
            @DrawableRes int drawable,
            @Nullable Runnable onClick
    ) {
        this(context, context.getString(title), context.getString(description), drawable, onClick);
    }

    public SettingCategory(
            Context context,
            @StringRes int title,
            String description,
            @DrawableRes int drawable,
            @Nullable Runnable onClick
    ) {
        this(context, context.getString(title), description, drawable, onClick);
    }
}
