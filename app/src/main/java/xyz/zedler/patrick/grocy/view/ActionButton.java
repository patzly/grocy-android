package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import xyz.zedler.patrick.grocy.R;

public class ActionButton extends LinearLayout {

    private final static String TAG = "ActionButton";
    private final static boolean DEBUG = false;

    private final static float ICON_ALPHA_DISABLED = 0.5f;

    Context context;
    ImageView imageViewIcon;
    FrameLayout frameLayoutButton;

    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        inflate(context, R.layout.view_action_button, this);

        imageViewIcon = findViewById(R.id.image_action_button);
        frameLayoutButton = findViewById(R.id.frame_action_button);

        if(attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton);
            try {
                imageViewIcon.setImageResource(
                        typedArray.getResourceId(R.styleable.ActionButton_icon, -1)
                );
            } finally {
                typedArray.recycle();
            }
        }
    }

    public void setIcon(@DrawableRes int iconRes) {
        imageViewIcon.setImageResource(iconRes);
    }

    public void setState(boolean enabled) {
        frameLayoutButton.setEnabled(enabled);
        frameLayoutButton.setAlpha(enabled ? 1 : ICON_ALPHA_DISABLED);
    }

    public void refreshState(boolean enabled) {
        if(enabled) {
            if(!isEnabled()) {
                frameLayoutButton.animate().alpha(1).setDuration(300).start();
            }
        } else {
            if(isEnabled()) {
                frameLayoutButton.animate().alpha(ICON_ALPHA_DISABLED).setDuration(300).start();
            }
        }
        frameLayoutButton.setEnabled(enabled);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        frameLayoutButton.setOnClickListener(l);
    }

    @Override
    public void setTooltipText(@Nullable CharSequence tooltipText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            frameLayoutButton.setTooltipText(tooltipText);
        }
    }

    public void startIconAnimation() {
        try {
            ((Animatable) (imageViewIcon).getDrawable()).start();
        } catch (ClassCastException cla) {
            if(DEBUG) Log.e(TAG, "startIconAnimation() requires AVD!");
        }
    }

    private int dp(float dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
