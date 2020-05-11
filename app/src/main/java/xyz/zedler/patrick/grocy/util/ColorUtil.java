package xyz.zedler.patrick.grocy.util;

import android.animation.ArgbEvaluator;

public class ColorUtil {

    public static int blend(int color1, int color2, float fraction) {
        if(fraction > 1) fraction = 1;
        else if(fraction < 0) fraction = 0;
        return (int) new ArgbEvaluator().evaluate(fraction, color1, color2);
    }
}
