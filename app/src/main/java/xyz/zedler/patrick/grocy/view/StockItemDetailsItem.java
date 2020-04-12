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

public class StockItemDetailsItem extends LinearLayout {

    private Context context;
    private TextView textViewProperty, textViewValue, textViewExtra;
    private LinearLayout linearLayoutContainer, linearLayoutExtra;
    private int height = 0;

    public StockItemDetailsItem(Context context) {
        super(context);

        this.context = context;
        init();
    }

    /**
     * In layout XML set visibility to GONE if the container should expand when text is set.
     */
    public StockItemDetailsItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        init();
    }

    private void init() {
        inflate(context, R.layout.view_stock_item_details_item, this);

        linearLayoutContainer = findViewById(R.id.linear_stock_item_details_item_container);
        textViewProperty = findViewById(R.id.text_stock_item_details_item_property);
        textViewValue = findViewById(R.id.text_stock_item_details_item_value);
        textViewExtra = findViewById(R.id.text_stock_item_details_item_extra);
        linearLayoutExtra = findViewById(R.id.linear_stock_item_details_item_extra);
    }

    public void setText(String property, String value, String extra) {
        textViewProperty.setText(property);
        textViewValue.setText(value);
        if(extra != null) {
            textViewExtra.setText(extra);
        } else {
            linearLayoutExtra.setVisibility(GONE);
        }
        if(getVisibility() == GONE) {
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
}
