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
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.grocy.databinding.ViewSettingEntrySwitchBinding;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class SettingEntrySwitch extends LinearLayout {

    private ViewSettingEntrySwitchBinding binding;

    public SettingEntrySwitch(Context context) {
        super(context);
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            String title,
            String description,
            String pref
    ) {
        super(context);
        setTag(tag);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        binding = ViewSettingEntrySwitchBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        if(description == null) binding.description.setVisibility(GONE);
        binding.switchMaterial.setChecked(sharedPrefs.getBoolean(pref, false));
        binding.getRoot().setOnClickListener(v -> binding.switchMaterial.toggle());
        binding.switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IconUtil.start(binding.image);
            sharedPrefs.edit().putBoolean(pref, isChecked).apply();
        });
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            String title,
            String description,
            @DrawableRes int drawable,
            String pref
    ) {
        this(context, tag, title, description, pref);
        binding.image.setImageDrawable(ContextCompat.getDrawable(context, drawable));
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            String title,
            String description,
            @DrawableRes int drawable,
            boolean setChecked,
            OnCheckedChanged onCheckedChanged
    ) {
        super(context);
        setTag(tag);
        binding = ViewSettingEntrySwitchBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        if(description == null) binding.description.setVisibility(GONE);
        binding.image.setImageDrawable(ContextCompat.getDrawable(context, drawable));
        binding.switchMaterial.setChecked(setChecked);
        binding.getRoot().setOnClickListener(v -> binding.switchMaterial.toggle());
        binding.switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IconUtil.start(binding.image);
            if(onCheckedChanged != null) onCheckedChanged.execute(isChecked);
        });
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            @StringRes int title,
            @StringRes int description,
            @DrawableRes int drawable,
            boolean setChecked,
            OnCheckedChanged onCheckedChanged
    ) {
        this(
                context,
                tag,
                context.getString(title),
                context.getString(description),
                drawable,
                setChecked,
                onCheckedChanged
        );
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            String title,
            String description,
            @DrawableRes int drawableOffAnim,
            @DrawableRes int drawableOnAnim,
            String pref,
            OnFinishedAnim onFinishedAnim
    ) {
        super(context);
        setTag(tag);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        binding = ViewSettingEntrySwitchBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        boolean enabled = sharedPrefs.getBoolean(pref, false);
        binding.image.setImageDrawable(ContextCompat.getDrawable(
                context, enabled ? drawableOffAnim : drawableOnAnim
        ));
        binding.switchMaterial.setChecked(sharedPrefs.getBoolean(pref, false));
        binding.getRoot().setOnClickListener(v -> binding.switchMaterial.toggle());
        binding.switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IconUtil.start(binding.image);
            new Handler().postDelayed(() -> {
                binding.image.setImageResource(isChecked ? drawableOffAnim : drawableOnAnim);
                if(onFinishedAnim != null) onFinishedAnim.execute(isChecked);
            }, 300);
            sharedPrefs.edit().putBoolean(pref, isChecked).apply();
        });
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            @StringRes int title,
            @StringRes int description,
            @DrawableRes int drawable,
            String pref
    ) {
        this(
                context,
                tag,
                context.getString(title),
                context.getString(description),
                drawable,
                pref
        );
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            @StringRes int title,
            @DrawableRes int drawable,
            String pref
    ) {
        this(context, tag, context.getString(title), null, drawable, pref);
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            @StringRes int title,
            String pref
    ) {
        this(context, tag, context.getString(title), null, pref);
    }

    public SettingEntrySwitch(
            Context context,
            String tag,
            @StringRes int title,
            @StringRes int description,
            @DrawableRes int drawableOffAnim,
            @DrawableRes int drawableOnAnim,
            String pref,
            OnFinishedAnim onFinishedAnim
    ) {
        this(
                context,
                tag,
                context.getString(title),
                context.getString(description),
                drawableOffAnim,
                drawableOnAnim,
                pref,
                onFinishedAnim
        );
    }

    public interface OnFinishedAnim {
        void execute(boolean isChecked);
    }

    public interface OnCheckedChanged {
        void execute(boolean isChecked);
    }
}
