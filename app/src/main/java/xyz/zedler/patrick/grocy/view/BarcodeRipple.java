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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;

import xyz.zedler.patrick.grocy.R;

public class BarcodeRipple extends LinearLayout {

    private MaterialCardView cardView;
    private ValueAnimator animator;
    private int width, height, strokeWidth;
    private boolean continueAnim = false;
    private int rippleDuration, rippleOffset;

    public BarcodeRipple(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.view_barcode_ripple, this);

        rippleDuration = 1500;
        rippleOffset = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40,
                getResources().getDisplayMetrics()
        );

        cardView = findViewById(R.id.card_barcode_ripple);
        cardView.setAlpha(0);
        cardView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        width = cardView.getWidth();
                        height = cardView.getHeight();
                        strokeWidth = cardView.getStrokeWidth();

                        //resumeAnimation();

                        if (getViewTreeObserver().isAlive()) {
                            getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
    }

    private void animation() {
        cardView.getLayoutParams().width = width;
        cardView.getLayoutParams().height = height;
        cardView.setStrokeWidth(strokeWidth);
        cardView.requestLayout();
        cardView.setAlpha(0);

        if(animator != null) {
            if(animator.isRunning()) animator.pause();
        }
        cardView.animate().alpha(1).setDuration(500).setInterpolator(
                new AccelerateInterpolator()
        ).withEndAction(() -> {
            animator = ValueAnimator.ofFloat(0, 1);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(continueAnim) startAnimation();
                }
            });
            animator.addUpdateListener(animation -> {
                cardView.getLayoutParams().height =
                        (int) (height + ((float) animation.getAnimatedValue()) * rippleOffset);
                cardView.getLayoutParams().width =
                        (int) (width + ((float) animation.getAnimatedValue()) * rippleOffset);
                cardView.setStrokeWidth(
                        (int) ((((float) animation.getAnimatedValue()) * -1 + 1) * strokeWidth)
                );
                cardView.setAlpha((float) animation.getAnimatedValue() * -1 + 1);
                cardView.requestLayout();
            });
            animator.setDuration(rippleDuration).setInterpolator(new DecelerateInterpolator());
            animator.start();
        }).start();
    }

    private void startAnimation() {
        continueAnim = true;
        //animation();
    }

    public void resumeAnimation() {
        continueAnim = true;
        new Handler().postDelayed(this::animation, 1500);
    }

    public void pauseAnimation() {
        continueAnim = false;
    }
}
