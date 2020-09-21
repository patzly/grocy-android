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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
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
            String preference,
            boolean preferenceDefault,
            String title,
            String description,
            Drawable drawable,
            OnCheckedChanged onCheckedChanged
    ) {
        super(context);
        setTag(preference);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        binding = ViewSettingEntrySwitchBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        if(description == null) binding.description.setVisibility(GONE);
        if(drawable != null) binding.image.setImageDrawable(drawable);
        binding.switchMaterial.setChecked(sharedPrefs.getBoolean(preference, preferenceDefault));
        binding.getRoot().setOnClickListener(v -> binding.switchMaterial.toggle());
        binding.switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IconUtil.start(binding.image);
            sharedPrefs.edit().putBoolean(preference, isChecked).apply();
            if(onCheckedChanged != null) onCheckedChanged.execute(preference, isChecked);
        });
    }

    public SettingEntrySwitch(
            Context context,
            String preference,
            boolean preferenceDefault,
            String title,
            String description,
            Drawable drawable
    ) {
        this(context, preference, preferenceDefault, title, description, drawable, null);
    }

    public SettingEntrySwitch(
            Context context,
            String preference,
            boolean preferenceDefault,
            String title
    ) {
        this(
                context,
                preference,
                preferenceDefault,
                title,
                null,
                null,
                null
        );
    }

    public SettingEntrySwitch(
            Context context,
            String preference,
            boolean preferenceDefault,
            String title,
            String description,
            @DrawableRes int drawableOffAnim,
            @DrawableRes int drawableOnAnim,
            OnFinishedAnim onFinishedAnim
    ) {
        super(context);
        setTag(preference);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        binding = ViewSettingEntrySwitchBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
        );
        binding.title.setText(title);
        binding.description.setText(description);
        boolean enabled = sharedPrefs.getBoolean(preference, preferenceDefault);
        binding.image.setImageDrawable(ContextCompat.getDrawable(
                context, enabled ? drawableOffAnim : drawableOnAnim
        ));
        binding.switchMaterial.setChecked(enabled);
        binding.getRoot().setOnClickListener(v -> binding.switchMaterial.toggle());
        binding.switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            IconUtil.start(binding.image);
            new Handler().postDelayed(() -> {
                binding.image.setImageResource(isChecked ? drawableOffAnim : drawableOnAnim);
                if(onFinishedAnim != null) onFinishedAnim.execute(isChecked);
            }, 300);
            sharedPrefs.edit().putBoolean(preference, isChecked).apply();
        });
    }

    public interface OnFinishedAnim {
        void execute(boolean isChecked);
    }

    public interface OnCheckedChanged {
        void execute(String preference, boolean isChecked);
    }
}
