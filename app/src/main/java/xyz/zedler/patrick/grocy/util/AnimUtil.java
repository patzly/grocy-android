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

package xyz.zedler.patrick.grocy.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

public class AnimUtil {

    private ValueAnimator animator;

    public AnimUtil() {}

    public void replaceViews(View viewIn, View viewOut, boolean animated) {
        if(animator != null) {
            animator.pause();
            animator.cancel();
            animator.removeAllUpdateListeners();
            animator.removeAllListeners();
            animator = null;
        }
        if(viewIn.getVisibility() == View.VISIBLE && viewIn.getAlpha() == 1) return;
        if(animated) {
            animator = ValueAnimator.ofFloat(viewOut.getAlpha(), 0);
            animator.addUpdateListener(
                    animation -> viewOut.setAlpha((float) animation.getAnimatedValue())
            );
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    viewOut.setVisibility(View.GONE);
                    viewIn.setAlpha(0);
                    viewIn.setVisibility(View.VISIBLE);
                    animator = ValueAnimator.ofFloat(0, 1);
                    animator.addUpdateListener(
                            anim -> viewIn.setAlpha((float) anim.getAnimatedValue())
                    );
                    animator.setDuration(150).start();
                }
            });
            animator.setDuration(150).start();
        } else {
            viewOut.setVisibility(View.GONE);
            viewIn.setVisibility(View.VISIBLE);
        }
    }
}
