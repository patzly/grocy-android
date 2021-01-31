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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

public class IconUtil {

    private final static String TAG = IconUtil.class.getSimpleName();

    public static void start(Activity activity, @IdRes int viewId) {
        if(activity == null) return;
        start((View) activity.findViewById(viewId));
    }

    public static void start(View view, @IdRes int viewId) {
        if(view == null) return;
        start((View) view.findViewById(viewId));
    }

    public static void start(View view) {
        if(view == null) return;
        try {
            ImageView imageView = (ImageView) view;
            start(imageView);
        } catch (ClassCastException e) {
            Log.e(TAG, "start() requires ImageView");
        }
    }

    public static void start(ImageView imageView) {
        if(imageView == null || imageView.getDrawable() == null) return;
        if(!(imageView.getDrawable() instanceof AnimatedVectorDrawable)) return;
        start(imageView.getDrawable());
    }

    public static void start(MenuItem item) {
        if(item == null) return;
        start(item.getIcon());
    }

    public static void start(Drawable drawable) {
        if(drawable == null) return;
        try {
            ((Animatable) drawable).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "start() requires AnimVectorDrawable");
        }
    }

    public static void reset(Drawable drawable) {
        if(drawable == null) return;
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((AnimatedVectorDrawable) drawable).reset();
            }
        } catch (ClassCastException cla) {
            Log.e(TAG, "start() requires AnimVectorDrawable");
        }
    }
}
