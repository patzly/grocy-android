package xyz.zedler.patrick.grocy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import xyz.zedler.patrick.grocy.R;

public class ListItem extends LinearLayout {

    private Context context;
    private TextView textViewProperty, textViewValue, textViewExtra;
    private LinearLayout linearLayoutContainer, linearLayoutExtra;
    private int height = 0;

    public ListItem(Context context) {
        super(context);

        this.context = context;
        init();
    }

    /**
     * In layout XML set visibility to GONE if the container should expand when setText() is called.
     */
    public ListItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        init();
    }

    private void init() {
        inflate(context, R.layout.view_list_item, this);

        linearLayoutContainer = findViewById(R.id.linear_list_item_container);
        textViewProperty = findViewById(R.id.text_list_item_property);
        textViewValue = findViewById(R.id.text_list_item_value);
        textViewExtra = findViewById(R.id.text_list_item_extra);
        linearLayoutExtra = findViewById(R.id.linear_list_item_extra);
    }

    public void setText(String property, String value) {
        setText(property, value, null);
    }

    public void setText(String property, String value, String extra) {
        // property
        if(property != null) {
            textViewProperty.setText(property);
        } else {
            textViewProperty.setVisibility(GONE);
        }
        // value
        textViewValue.setText(value);
        // extra
        if(extra != null) {
            textViewExtra.setText(extra);
        } else {
            linearLayoutExtra.setVisibility(GONE);
        }
        if(getVisibility() == GONE) {
            // expand
            measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            height = getMeasuredHeight();
            getLayoutParams().height = 0;
            requestLayout();

            setAlpha(0);
            setVisibility(VISIBLE);
            animate().alpha(1).setDuration(300).setStartDelay(100).start();
            getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            ValueAnimator heightAnimator = ValueAnimator.ofInt(0, height);
                            heightAnimator.addUpdateListener(animation -> {
                                getLayoutParams().height = (int) animation.getAnimatedValue();
                                requestLayout();
                            });
                            heightAnimator.setDuration(400).setInterpolator(
                                    new DecelerateInterpolator()
                            );
                            heightAnimator.start();
                            if (getViewTreeObserver().isAlive()) {
                                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        }
                    });
        }
    }

    public void setSingleLine(boolean singleLine) {
        textViewValue.setSingleLine(singleLine);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        linearLayoutContainer.setOnClickListener(l);
    }
}
