package xyz.zedler.patrick.grocy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.card.MaterialCardView;

import xyz.zedler.patrick.grocy.R;

public class FilterChip extends LinearLayout {

    private final static String TAG = "FilterChip";
    private final static boolean DEBUG = false;

    private Context context;
    private ImageView imageViewIcon, imageViewIconBg;
    private FrameLayout frameLayoutIcon;
    private TextView textView;
    private MaterialCardView cardView;
    private boolean isActive = false;

    public FilterChip(@NonNull Context context) {
        super(context);

        this.context = context;
        init(R.color.on_background_secondary, null, null);
    }

    public FilterChip(Context context, @ColorRes int colorId, String text, Runnable onClick) {
        super(context);

        this.context = context;
        init(colorId, text, onClick);
    }

    private void init(@ColorRes int colorId, String text, Runnable onClick) {
        inflate(context, R.layout.view_filter_chip, this);

        cardView = findViewById(R.id.card_filter_chip);
        imageViewIcon = findViewById(R.id.image_filter_chip_icon);
        imageViewIconBg = findViewById(R.id.image_filter_chip_icon_bg);
        frameLayoutIcon = findViewById(R.id.frame_filter_chip_icon);
        textView = findViewById(R.id.text_filter_chip);

        setText(text);

        setBackgroundColor(ContextCompat.getColor(context, colorId));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(dp(4), 0, dp(4), 0);
        setLayoutParams(layoutParams);

        setOnClickListener(v -> {
            invertState();
            if(onClick != null) onClick.run();
        });

        setActive(isActive);
    }

    public void setIcon(@DrawableRes int iconRes) {
        imageViewIcon.setImageResource(iconRes);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void setIconTint(int color) {
        imageViewIcon.setImageTintList(new ColorStateList(new int[][]{
                new int[] { android.R.attr.state_enabled},
                new int[] {-android.R.attr.state_enabled}
        }, new int[]{
                color, color
        }));
    }

    public void setBackgroundColor(int color) {
        cardView.setCardBackgroundColor(color);
    }

    public void setActive(boolean active) {
        isActive = active;
        frameLayoutIcon.setLayoutParams(new LayoutParams(dp(isActive ? 24 : 0), dp(24)));
    }

    public void invertState() {
        changeState(!isActive);
    }

    public void changeState(boolean active) {
        isActive = active;

        ValueAnimator valueAnimator = ValueAnimator.ofInt(
                frameLayoutIcon.getWidth(),
                dp(isActive ? 24 : 0)
        );
        valueAnimator.addUpdateListener(
                animation -> {
                    frameLayoutIcon.setLayoutParams(
                            new LayoutParams((int) animation.getAnimatedValue(), dp(24))
                    );
                    frameLayoutIcon.invalidate();
                }
        );
        valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        valueAnimator.setDuration(150).start();

        imageViewIconBg.animate().alpha(active ? 1 : 0).setDuration(active ? 150 : 100).start();

        imageViewIcon.setImageResource(
                active
                        ? R.drawable.ic_round_filter_list_in_anim
                        : R.drawable.ic_round_filter_list_out_anim
                );
        new Handler().postDelayed(this::startIconAnimation, active ? 100 : 0);
    }

    @Override
    public boolean callOnClick() {
        setActive(!isActive);
        return super.callOnClick();
    }

    private void startIconAnimation() {
        try {
            ((Animatable) (imageViewIcon).getDrawable()).start();
        } catch (ClassCastException cla) {
            if(DEBUG) Log.e(TAG, "startIconAnimation() requires AVD!");
        }
    }

    private int dp(int dp){
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
