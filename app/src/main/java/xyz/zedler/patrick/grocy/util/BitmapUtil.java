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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import xyz.zedler.patrick.grocy.R;

public class BitmapUtil {

    public static Bitmap scale(@Nullable Bitmap bm, float scale) {
        if(scale > 1) return bm;
        if(bm != null) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            int scaleWidth = (int) (width * scale);
            if(scaleWidth <= 0) scaleWidth = 1;
            int scaleHeight = (int) (height * scale);
            if(scaleHeight <= 0) scaleHeight = 1;
            return Bitmap.createScaledBitmap(
                    bm, scaleWidth, scaleHeight, false
            );
        } return null;
    }

    public static Bitmap getFromDrawable(Context context, @DrawableRes int resId) {
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), resId, null);
        if(drawable != null) {
            if(drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
            Bitmap bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } return null;
    }

    public static Bitmap getFromDrawableWithNumber(
            Context context,
            @DrawableRes int resId,
            int number,
            float textSize,
            float textOffsetX,
            float textOffsetY
    ) {
        Bitmap bitmap = getFromDrawable(context, resId);
        if(bitmap == null) return null;
        // make mutable
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(UnitUtil.getDp(context, textSize));
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.material_digits_round));
        paint.setLetterSpacing(0.1f);

        Rect bounds = new Rect();
        paint.getTextBounds(
                String.valueOf(number),
                0,
                String.valueOf(number).length(),
                bounds
        );
        int x = (bitmap.getWidth() - bounds.width()) / 2;
        int y = (bitmap.getHeight() + bounds.height()) / 2;

        canvas.drawText(
                String.valueOf(number),
                x + UnitUtil.getDp(context, textOffsetX),
                y - UnitUtil.getDp(context, textOffsetY),
                paint
        );

        return bitmap;
    }
}
