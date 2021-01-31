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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.card.MaterialCardView;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class FilterChip extends LinearLayout {

    private final static String TAG = "FilterChip";

    private Context context;
    private ImageView imageViewIcon, imageViewIconBg;
    private FrameLayout frameLayoutIcon;
    private TextView textView;
    private MaterialCardView cardView;
    private boolean isActive = false;

    public FilterChip(@NonNull Context context) {
        super(context);

        this.context = context;
        init(R.color.on_background_secondary, null, null, null);
    }

    public FilterChip(
            Context context,
            @ColorRes int colorId,
            String text,
            Runnable onClick,
            Runnable onClickAgain
    ) {
        super(context);

        this.context = context;
        init(colorId, text, onClick, onClickAgain);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putBoolean("isActive", isActive);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            setActive(bundle.getBoolean("isActive"));
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(@ColorRes int colorId, String text, Runnable onClick, Runnable onClickAgain) {
        inflate(context, R.layout.view_filter_chip, this);

        cardView = findViewById(R.id.card_filter_chip);
        imageViewIcon = findViewById(R.id.image_filter_chip_icon);
        imageViewIconBg = findViewById(R.id.image_filter_chip_icon_bg);
        frameLayoutIcon = findViewById(R.id.frame_filter_chip_icon);
        textView = findViewById(R.id.text_filter_chip);

        setText(text);
        setIconTint(ContextCompat.getColor(context, R.color.black));
        setBackgroundColor(ContextCompat.getColor(context, colorId));

        setOnClickListener(v -> {
            invertState();
            if(onClick != null && isActive) onClick.run();
            if(onClickAgain != null && !isActive) onClickAgain.run();
        });

        setActive(isActive);
    }

    public void setIcon(@DrawableRes int iconRes) {
        imageViewIcon.setImageResource(iconRes);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void setIconTint(int color) {
        imageViewIcon.setImageTintList(new ColorStateList(new int[][]{
                new int[] { android.R.attr.state_enabled},
                new int[] {-android.R.attr.state_enabled}
        }, new int[]{
                color, color
        }));
    }

    public void setBackgroundColor(int color) {
        cardView.setCardBackgroundColor(color);
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;

        frameLayoutIcon.setLayoutParams(new LayoutParams(dp(isActive ? 24 : 0), dp(24)));
        if(active) {
            imageViewIcon.setImageResource(R.drawable.ic_round_filter_list_out_anim);
            IconUtil.reset(imageViewIcon.getDrawable());
        }
    }

    public void invertState() {
        changeState(!isActive);
    }

    public void changeState(boolean active) {
        isActive = active;

        ValueAnimator valueAnimator = ValueAnimator.ofInt(
                frameLayoutIcon.getWidth(),
                dp(isActive ? 24 : 0)
        );
        valueAnimator.addUpdateListener(
                animation -> {
                    frameLayoutIcon.setLayoutParams(
                            new LayoutParams((int) animation.getAnimatedValue(), dp(24))
                    );
                    frameLayoutIcon.invalidate();
                }
        );
        valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        valueAnimator.setDuration(150).start();

        imageViewIconBg.animate().alpha(active ? 1 : 0).setDuration(active ? 150 : 100).start();

        imageViewIcon.setImageResource(
                active
                        ? R.drawable.ic_round_filter_list_in_anim
                        : R.drawable.ic_round_filter_list_out_anim
        );
        new Handler().postDelayed(() -> {
            try {
                ((Animatable) imageViewIcon.getDrawable()).start();
            } catch (ClassCastException cla) {
                Log.e(TAG, "startIconAnimation() requires AVD!");
            }
        }, active ? 100 : 0);
    }

    private int dp(int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
