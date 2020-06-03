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
