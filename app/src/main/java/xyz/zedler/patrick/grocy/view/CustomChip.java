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

    public CustomChip(Context context) {
        super(context);

        this.context = context;
        init(R.color.on_background_secondary, null);
    }

    public CustomChip(Context context, @ColorRes int colorId, String text) {
        super(context);

        this.context = context;
        init(colorId, text);
    }

    private void init(int colorId, String text) {
        setCheckable(true);
        setCheckedIconVisible(true);
        setCloseIconVisible(false);

        setTextAppearance(context, R.style.TextAppearance_Grocy_Chip);
        setChipIconVisible(false);
        setRippleColor(null);
        setStateListAnimator(null);

        setChipBackgroundColorResource(colorId);
        setText(text);
        setTextColor(ContextCompat.getColor(context, R.color.on_retro));
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(getMargin(), 0, getMargin(), 0);
        setLayoutParams(layoutParams);
    }

    private int getMargin(){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4,
                context.getResources().getDisplayMetrics()
        );
    }
}
