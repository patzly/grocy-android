package xyz.zedler.patrick.grocy.util;

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

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibratorUtil {

    private final static long CLICK = 60;
    private final static long HEAVY_CLICK = 120;
    private final static long TICK = 30;

    private Vibrator vibrator;

    public VibratorUtil(Activity activity) {
        if(activity != null) {
            vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    public void click() {
        vibrate(CLICK);
    }

    public void heavyClick() {
        vibrate(HEAVY_CLICK);
    }

    public void tick() {
        vibrate(TICK);
    }

    public void vibrate(long duration) {
        if(vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(
                                duration,
                                VibrationEffect.DEFAULT_AMPLITUDE
                        )
                );
            } else {
                vibrator.vibrate(duration);
            }
        }
    }
}
