package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.zedler.patrick.grocy.R;

public class ActionButton extends LinearLayout {

    private final static String TAG = ActionButton.class.getSimpleName();
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
                setIconTint(
                        typedArray.getColor(R.styleable.ActionButton_tint, -1)
                );
            } finally {
                typedArray.recycle();
            }
        }
    }

    public void setIcon(@DrawableRes int iconRes) {
        imageViewIcon.setImageResource(iconRes);
    }

    public void setIconTint(int color) {
        imageViewIcon.setImageTintList(new ColorStateList(new int[][]{
                new int[] { android.R.attr.state_enabled},
                new int[] {-android.R.attr.state_enabled}
        }, new int[]{
                color, color
        }));
    }

    public void setState(boolean enabled) {
        frameLayoutButton.setEnabled(enabled);
        frameLayoutButton.setAlpha(enabled ? 1 : ICON_ALPHA_DISABLED);
    }

    public void refreshState(boolean enabled) {
        if(enabled) {
            if(!frameLayoutButton.isEnabled()) {
                frameLayoutButton.animate().alpha(1).setDuration(300).start();
            }
        } else {
            if(frameLayoutButton.isEnabled()) {
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
}
