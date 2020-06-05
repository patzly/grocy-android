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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.zedler.patrick.grocy.R;

public class ActionButton extends LinearLayout {

    private final static String TAG = ActionButton.class.getSimpleName();
    private final static boolean DEBUG = false;

    private final static float ICON_ALPHA_DISABLED = 0.5f;

    Context context;
    ImageView imageViewIcon;
    FrameLayout frameLayoutButton;

    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        int iconResId = -1;
        int colorIconTint = -1;
        boolean isDense = false;

        if(attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton);
            try {
                iconResId = typedArray.getResourceId(R.styleable.ActionButton_icon, -1);
                colorIconTint = typedArray.getColor(R.styleable.ActionButton_tint, -1);
                isDense = typedArray.getBoolean(R.styleable.ActionButton_dense, false);
            } finally {
                typedArray.recycle();
            }
        }

        inflate(
                context,
                isDense
                        ? R.layout.view_action_button_dense
                        : R.layout.view_action_button,
                this
        );

        imageViewIcon = findViewById(R.id.image_action_button);
        frameLayoutButton = findViewById(R.id.frame_action_button);

        imageViewIcon.setImageResource(iconResId);
        setIconTint(colorIconTint);
    }

    public void setIcon(@DrawableRes int iconRes) {
        imageViewIcon.setImageResource(iconRes);
    }

    public void setIconTint(int color) {
        imageViewIcon.setImageTintList(new ColorStateList(new int[][]{
                new int[] { android.R.attr.state_enabled},
                new int[] {-android.R.attr.state_enabled}
        }, new int[]{
                color, color
        }));
    }

    public void setState(boolean enabled) {
        frameLayoutButton.setEnabled(enabled);
        frameLayoutButton.setAlpha(enabled ? 1 : ICON_ALPHA_DISABLED);
    }

    public void refreshState(boolean enabled) {
        if(enabled) {
            if(!frameLayoutButton.isEnabled()) {
                frameLayoutButton.animate().alpha(1).setDuration(300).start();
            }
        } else {
            if(frameLayoutButton.isEnabled()) {
                frameLayoutButton.animate().alpha(ICON_ALPHA_DISABLED).setDuration(300).start();
            }
        }
        frameLayoutButton.setEnabled(enabled);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        frameLayoutButton.setOnClickListener(l);
    }

    @Override
    public void setTooltipText(@Nullable CharSequence tooltipText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            frameLayoutButton.setTooltipText(tooltipText);
        }
    }

    public void startIconAnimation() {
        try {
            ((Animatable) imageViewIcon.getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startIconAnimation() requires AVD!");
        }
    }
}
