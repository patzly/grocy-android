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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import xyz.zedler.patrick.grocy.databinding.ViewSettingEntryClickBinding;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class SettingEntryClick extends LinearLayout {

    private ViewSettingEntryClickBinding binding;

    public SettingEntryClick(Context context) {
        super(context);
    }

    public SettingEntryClick(
            Context context,
            String tag,
            String title,
            String description,
            @Nullable OnClickListener onClick
    ) {
        super(context);
        setTag(tag);
        binding = ViewSettingEntryClickBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        if(description == null) binding.description.setVisibility(GONE);
        if(onClick != null) {
            binding.getRoot().setOnClickListener(v -> {
                IconUtil.start(binding.image);
                onClick.execute(this);
            });
        }
    }

    public SettingEntryClick(
            Context context,
            String tag,
            String title,
            String description,
            @DrawableRes int drawable,
            @Nullable OnClickListener onClick
    ) {
        this(context, tag, title, description, onClick);
        binding.image.setImageDrawable(ContextCompat.getDrawable(context, drawable));
    }

    public SettingEntryClick(
            Context context,
            String tag,
            @StringRes int title,
            @StringRes int description,
            @DrawableRes int drawable,
            @Nullable OnClickListener onClick
    ) {
        this(
                context,
                tag,
                context.getString(title),
                context.getString(description),
                drawable,
                onClick
        );
    }

    public SettingEntryClick(
            Context context,
            String tag,
            @StringRes int title,
            @StringRes int description,
            @Nullable OnClickListener onClick
    ) {
        this(context, tag, context.getString(title), context.getString(description), onClick);
    }

    public SettingEntryClick(
            Context context,
            String tag,
            @StringRes int overLine,
            String title,
            String description,
            @DrawableRes int drawable,
            @Nullable OnClickListener onClick
    ) {
        this(context, tag, title, description, drawable, onClick);
        binding.overline.setVisibility(VISIBLE);
        binding.overline.setText(overLine);
    }

    public SettingEntryClick(
            Context context,
            String tag,
            @StringRes int overLine,
            String title,
            @StringRes int description,
            @ColorRes int descriptionColor,
            boolean descriptionBold,
            @DrawableRes int drawable,
            @Nullable OnClickListener onClick
    ) {
        this(context, tag, title, context.getString(description), drawable, onClick);
        binding.description.setTextColor(getResources().getColor(descriptionColor));
        if(descriptionBold) {
            binding.description.setTypeface(binding.description.getTypeface(), Typeface.BOLD);
        }
        binding.overline.setVisibility(VISIBLE);
        binding.overline.setText(overLine);
    }

    public SettingEntryClick(
            Context context,
            String tag,
            @StringRes int title,
            String description,
            @DrawableRes int drawable,
            @Nullable OnClickListener onClick
    ) {
        this(context, tag, context.getString(title), description, drawable, onClick);
    }

    public void setTitle(String title) {
        binding.title.setText(title);
    }

    public void setTitle(@StringRes int title) {
        binding.title.setText(title);
    }

    public String getTitle() {
        return binding.title.getText().toString();
    }

    public interface OnClickListener {
        void execute(SettingEntryClick view);
    }
}
