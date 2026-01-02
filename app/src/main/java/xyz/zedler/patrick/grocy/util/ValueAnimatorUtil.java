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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.os.Handler;
import android.os.Looper;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

public class ValueAnimatorUtil {

    private long duration = 300; // Default duration
    private long startTime;
    private boolean running = false;
    private boolean isIntAnimation = false;

    private float startFloatValue;
    private float endFloatValue;
    private float currentFloatValue;

    private final List<AnimatorUpdateListener> updateListeners = new ArrayList<>();
    private final List<AnimatorListener> listeners = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Interpolator interpolator = new LinearInterpolator(); // Default interpolator

    @SuppressWarnings("unused")
    public static ValueAnimatorUtil ofInt(int start, int end) {
        ValueAnimatorUtil animator = new ValueAnimatorUtil();
        animator.startFloatValue = start;
        animator.endFloatValue = end;
        animator.isIntAnimation = true;
        return animator;
    }

    @SuppressWarnings("unused")
    public static ValueAnimatorUtil ofFloat(float start, float end) {
        ValueAnimatorUtil animator = new ValueAnimatorUtil();
        animator.startFloatValue = start;
        animator.endFloatValue = end;
        animator.isIntAnimation = false;
        return animator;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void addUpdateListener(AnimatorUpdateListener listener) {
        updateListeners.add(listener);
    }

    public void addListener(AnimatorListener listener) {
        listeners.add(listener);
    }

    public void removeAllListeners() {
        updateListeners.clear();
        listeners.clear();
    }

    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    public void start() {
        if (running) return;

        running = true;
        startTime = System.currentTimeMillis();

        for (AnimatorListener listener : listeners) {
            listener.onAnimationStart(this);
        }

        handler.post(animationRunnable);
    }

    public void cancel() {
        if (!running) return;

        running = false;
        handler.removeCallbacks(animationRunnable);

        for (AnimatorListener listener : listeners) {
            listener.onAnimationCancel(this);
        }
    }

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = System.currentTimeMillis() - startTime;
            float fraction = Math.min(1f, (float) elapsed / duration);
            fraction = interpolator.getInterpolation(fraction);

            currentFloatValue = startFloatValue + fraction * (endFloatValue - startFloatValue);

            for (AnimatorUpdateListener listener : updateListeners) {
                listener.onAnimationUpdate(ValueAnimatorUtil.this);
            }

            if (fraction < 1f) {
                handler.postDelayed(this, 16);
            } else {
                running = false;
                for (AnimatorListener listener : listeners) {
                    listener.onAnimationEnd(ValueAnimatorUtil.this);
                }
            }
        }
    };

    public Object getAnimatedValue() {
        if (isIntAnimation) {
            return Math.round(currentFloatValue);
        }
        return currentFloatValue;
    }

    public interface AnimatorUpdateListener {
        void onAnimationUpdate(ValueAnimatorUtil animation);
    }

    public interface AnimatorListener {
        void onAnimationStart(ValueAnimatorUtil animation);
        void onAnimationEnd(ValueAnimatorUtil animation);
        void onAnimationCancel(ValueAnimatorUtil animation);
    }

    public abstract static class AnimatorListenerAdapter implements AnimatorListener {
        @Override
        public void onAnimationStart(ValueAnimatorUtil animation) {}

        @Override
        public void onAnimationEnd(ValueAnimatorUtil animation) {}

        @Override
        public void onAnimationCancel(ValueAnimatorUtil animation) {}
    }
}