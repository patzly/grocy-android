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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class InputChip extends LinearLayout {

    private final static String TAG = "InputChip";

    private Context context;
    private LinearLayout linearLayoutThis;
    private ImageView imageViewIcon;
    private FrameLayout frameLayoutContainer, frameLayoutIcon;
    private TextView textView;
    private Runnable runnableOnClose;
    private int width;

    public InputChip(@NonNull Context context) {
        super(context);

        this.context = context;
        init(null, -1, false, null);
    }

    public InputChip(Context context, String text, @DrawableRes int iconRes, boolean animate) {
        super(context);

        this.context = context;
        init(text, iconRes, animate, null);
    }

    public InputChip(
            Context context,
            String text,
            @DrawableRes int iconRes,
            boolean animate,
            Runnable onClose
    ) {
        super(context);

        this.context = context;
        init(text, iconRes, animate, onClose);
    }

    public InputChip(
            Context context,
            String text,
            boolean animate,
            Runnable onClose
    ) {
        super(context);

        this.context = context;
        init(text, -1, animate, onClose);
    }

    public InputChip(
            Context context,
            String text,
            boolean animate
    ) {
        super(context);

        this.context = context;
        init(text, -1, animate, null);
    }

    private void init(String text, int iconRes, boolean animate, Runnable onClose) {
        inflate(context, R.layout.view_input_chip, this);

        linearLayoutThis = this;
        frameLayoutContainer = findViewById(R.id.frame_input_chip_container);
        frameLayoutIcon = findViewById(R.id.frame_input_chip_icon);
        imageViewIcon = findViewById(R.id.image_input_chip_icon);
        textView = findViewById(R.id.text_input_chip);
        runnableOnClose = onClose;

        if(animate) {
            frameLayoutContainer.setAlpha(0);
            frameLayoutContainer.animate().alpha(1).setDuration(200).setStartDelay(200).start();
        }

        setIcon(iconRes);
        setText(text);

        findViewById(R.id.view_input_chip_close).setOnClickListener(v -> close());
    }

    public void setIcon(@DrawableRes int iconRes) {
        if(iconRes != -1) {
            imageViewIcon.setImageResource(iconRes);
            frameLayoutIcon.setVisibility(VISIBLE);
        } else {
            frameLayoutIcon.setVisibility(GONE);
        }
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public String getText() {
        return textView.getText().toString();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        frameLayoutContainer.setPadding(
                UnitUtil.getDp(context, left),
                UnitUtil.getDp(context, top),
                UnitUtil.getDp(context, right),
                UnitUtil.getDp(context, bottom)
        );
    }

    public void close() {
        // DURATIONS
        int fade = 200, disappear = 300;
        // run action
        if(runnableOnClose != null) {
            new Handler().postDelayed(runnableOnClose, fade + disappear);
        }
        // first fade out
        frameLayoutContainer.animate().alpha(0).setDuration(fade).start();
        // then make shape disappear
        width = frameLayoutContainer.getWidth();
        // animate height
        ValueAnimator animatorHeight = ValueAnimator.ofInt(frameLayoutContainer.getHeight(), 0);
        animatorHeight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeAllViews();
                if(getParent() != null) {
                    ((ViewGroup) getParent()).removeView(linearLayoutThis);
                }
            }
        });
        animatorHeight.addUpdateListener(
                animation -> {
                    frameLayoutContainer.setLayoutParams(
                            new LayoutParams(width, (int) animation.getAnimatedValue())
                    );
                    frameLayoutContainer.invalidate();
                }
        );
        animatorHeight.setInterpolator(new FastOutSlowInInterpolator());
        animatorHeight.setStartDelay(fade);
        animatorHeight.setDuration(disappear).start();
        // animate width
        ValueAnimator animatorWidth = ValueAnimator.ofInt(frameLayoutContainer.getWidth(), 0);
        animatorWidth.addUpdateListener(
                animation -> width = (int) animation.getAnimatedValue()
        );
        animatorWidth.setInterpolator(new FastOutSlowInInterpolator());
        animatorWidth.setStartDelay(fade);
        animatorWidth.setDuration(disappear).start();
    }

    public void changeText(String text) {
        textView.setText(text);
    }
}
