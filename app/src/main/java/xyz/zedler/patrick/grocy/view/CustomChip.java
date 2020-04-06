package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;

import xyz.zedler.patrick.grocy.R;

public class CustomChip extends Chip {

    private Context context;

    public CustomChip(Context context, @ColorRes int colorId, String text, int marginStart, int marginEnd) {
        super(context);

        this.context = context;

        setCheckable(true);
        setCheckedIconVisible(true);
        setCloseIconVisible(false);

        setTextAppearance(context, R.style.TextAppearance_Grocy_Chip);
        setTextColor(ContextCompat.getColor(context, R.color.on_retro));
        setChipIconVisible(false);
        setRippleColor(null);
        setStateListAnimator(null);

        setChipBackgroundColorResource(colorId);
        setText(text);
        setMargins(marginStart, marginEnd);
    }

    public void setMargins(int start, int end) {
        setLayoutParams(getLayoutParams(start, end));
    }

    private LinearLayout.LayoutParams getLayoutParams(int left, int right) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(dp(left), 0, dp(right), 0);
        return layoutParams;
    }

    private int dp(float dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
