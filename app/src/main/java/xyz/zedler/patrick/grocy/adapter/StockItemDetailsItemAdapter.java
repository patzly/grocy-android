package xyz.zedler.patrick.grocy.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class StockItemDetailsItemAdapter extends RecyclerView.Adapter<StockItemDetailsItemAdapter.ViewHolder> {

    private final static String TAG = "ItemDetailsItemAdapter";
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private BottomSheetDialog bottomSheet;
    private StockItem stockItem;
    private ProductDetails productDetails;
    private QuantityUnit quantityUnit;
    private List<QuantityUnit> quantityUnits;
    private List<Location> locations;
    private DateUtil dateUtil;
    private int positionClick;

    public StockItemDetailsItemAdapter(
            MainActivity activity,
            BottomSheetDialog bottomSheet,
            StockItem stockItem,
            List<QuantityUnit> quantityUnits,
            List<Location> locations,
            int positionClick
    ) {
        this.activity = activity;
        this.bottomSheet = bottomSheet;
        this.stockItem = stockItem;
        this.quantityUnits = quantityUnits;
        this.locations = locations;
        this.positionClick = positionClick;
    }

    public StockItemDetailsItemAdapter(
            MainActivity activity,
            BottomSheetDialog bottomSheet,
            ProductDetails productDetails,
            int positionClick
    ) {
        this.activity = activity;
        this.bottomSheet = bottomSheet;
        this.productDetails = productDetails;
        this.positionClick = positionClick;
        dateUtil = new DateUtil(activity);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewProperty, textViewValue, textViewExtra;
        LinearLayout linearLayoutContainer, linearLayoutExtra;
        ActionButton actionButtonConsume, actionButtonOpen;

        ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(R.id.linear_stock_item_details_item_container);
            textViewProperty = view.findViewById(R.id.text_stock_item_details_item_property);
            textViewValue = view.findViewById(R.id.text_stock_item_details_item_value);
            textViewExtra = view.findViewById(R.id.text_stock_item_details_item_extra);
            linearLayoutExtra = view.findViewById(R.id.linear_stock_item_details_item_extra);
            actionButtonConsume = view.findViewById(R.id.button_stock_item_details_consume);
            actionButtonOpen = view.findViewById(R.id.button_stock_item_details_open);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.view_stock_item_details_item, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        switch (position) {
            case 0: // AMOUNT
                if(hasDetails()) {
                    quantityUnit = productDetails.getQuantityUnitStock();
                } else {
                    for(int i = 0; i < quantityUnits.size(); i++) {
                        if(quantityUnits.get(i).getId() == stockItem.getProduct().getQuIdStock()) {
                            quantityUnit = quantityUnits.get(i);
                            break;
                        }
                    }
                }
                // text
                holder.textViewProperty.setText(activity.getString(R.string.property_amount));
                holder.textViewValue.setText(getAmountText());
                // aggregated amount
                int isAggregatedAmount = hasDetails()
                        ? productDetails.getIsAggregatedAmount()
                        : stockItem.getIsAggregatedAmount();
                if(isAggregatedAmount == 1) {
                    holder.textViewExtra.setText(getAggregatedAmount());
                    holder.linearLayoutExtra.setVisibility(View.VISIBLE);
                }
                // actions
                holder.actionButtonConsume.setVisibility(View.VISIBLE);
                holder.actionButtonConsume.setState(
                        hasDetails()
                                ? productDetails.getStockAmount() > 0
                                : stockItem.getAmount() > 0
                );
                holder.actionButtonConsume.setOnClickListener(v -> {
                    disableActions(holder.actionButtonConsume, holder.actionButtonOpen);
                    ((StockFragment) activity.getCurrentFragment()).performAction(
                            Constants.ACTION.CONSUME,
                            positionClick,
                            hasDetails()
                                    ? productDetails.getProduct().getId()
                                    : stockItem.getProduct().getId()
                    );
                    bottomSheet.dismiss();
                });
                holder.actionButtonOpen.setVisibility(View.VISIBLE);
                holder.actionButtonOpen.setState(
                        hasDetails()
                                ? productDetails.getStockAmount()
                                > productDetails.getStockAmountOpened()
                                : stockItem.getAmount() > stockItem.getAmountOpened()
                );
                holder.actionButtonOpen.setOnClickListener(v -> {
                    disableActions(holder.actionButtonConsume, holder.actionButtonOpen);
                    ((StockFragment) activity.getCurrentFragment()).performAction(
                            Constants.ACTION.OPEN,
                            positionClick,
                            hasDetails()
                                    ? productDetails.getProduct().getId()
                                    : stockItem.getProduct().getId()
                    );
                    bottomSheet.dismiss();
                });
                // tooltips
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.actionButtonConsume.setTooltipText(
                            activity.getString(
                                    R.string.action_consume_one,
                                    quantityUnit.getName(),
                                    hasDetails()
                                            ? productDetails.getProduct().getName()
                                            : stockItem.getProduct().getName()
                            )
                    );
                    holder.actionButtonOpen.setTooltipText(
                            activity.getString(
                                    R.string.action_open_one,
                                    quantityUnit.getName(),
                                    hasDetails()
                                            ? productDetails.getProduct().getName()
                                            : stockItem.getProduct().getName()
                            )
                    );
                    // TODO: tooltip colors
                }
                break;
            case 1: // LOCATION
                holder.textViewProperty.setText(
                        activity.getString(R.string.property_default_location)
                );
                Location location = null;
                if(hasDetails()) {
                    location = productDetails.getLocation();
                } else {
                    for(int i = 0; i < locations.size(); i++) {
                        if(locations.get(i).getId() == stockItem.getProduct().getLocationId()) {
                            location = locations.get(i);
                            break;
                        }
                    }
                }
                holder.textViewValue.setText(location != null ? location.getName() : "");
                break;
            case 2: // LAST PURCHASED
                if(hasDetails() && productDetails.getLastPurchased() != null) {
                    holder.textViewProperty.setText(
                            activity.getString(R.string.property_last_purchased)
                    );
                    holder.textViewValue.setText(
                            dateUtil.getLocalizedDate(productDetails.getLastPurchased())
                    );
                    holder.textViewExtra.setText(
                            dateUtil.getHumanFromToday(
                                    DateUtil.getDaysFromNow(productDetails.getLastPurchased())
                            )
                    );
                    holder.linearLayoutExtra.setVisibility(View.VISIBLE);
                    //expandContainer(holder.linearLayoutContainer);
                } else {
                    holder.linearLayoutContainer.setVisibility(View.GONE);
                }
                break;
            case 3: // LAST USED
                if(hasDetails()) {
                    holder.textViewProperty.setText(
                            activity.getString(R.string.property_last_used)
                    );
                    String lastUsed = productDetails.getLastUsed();
                    holder.textViewValue.setText(
                            lastUsed != null
                                    ? dateUtil.getLocalizedDate(lastUsed)
                                    : activity.getString(R.string.date_never)
                    );
                    if(lastUsed != null) {
                        holder.textViewExtra.setText(dateUtil.getHumanForDaysFromNow(lastUsed));
                        holder.linearLayoutExtra.setVisibility(View.VISIBLE);
                    }
                    //expandContainer(holder.linearLayoutContainer);
                } else {
                    holder.linearLayoutContainer.setVisibility(View.GONE);
                }
                break;
            case 4: // LAST PRICE
                if(hasDetails() && productDetails.getLastPrice() != null) {
                    holder.textViewProperty.setText(
                            activity.getString(R.string.property_last_price)
                    );
                    holder.textViewValue.setText(productDetails.getLastPrice());
                    //expandContainer(holder.linearLayoutContainer);
                } else {
                    holder.linearLayoutContainer.setVisibility(View.GONE);
                }
                break;
            case 5: // AVERAGE SHELF LIFE
                if(hasDetails() && productDetails.getAverageShelfLifeDays() != 0) {
                    holder.textViewProperty.setText(
                            activity.getString(R.string.property_average_shelf_life)
                    );
                    holder.textViewValue.setText(
                            dateUtil.getHumanFromDays(productDetails.getAverageShelfLifeDays())
                    );
                    //expandContainer(holder.linearLayoutContainer);
                } else {
                    holder.linearLayoutContainer.setVisibility(View.GONE);
                }
                break;
            case 6: // SPOIL RATE
                if(hasDetails()) {
                    holder.textViewProperty.setText(
                            activity.getString(R.string.property_spoil_rate)
                    );
                    holder.textViewValue.setText(
                            productDetails.getSpoilRatePercent() + "%"
                    );
                    //expandContainer(holder.linearLayoutContainer);
                } else {
                    holder.linearLayoutContainer.setVisibility(View.GONE);
                }
                break;
        }
    }

    private String getAmountText() {
        int amount = hasDetails() ? productDetails.getStockAmount() : stockItem.getAmount();
        int opened = hasDetails()
                ? productDetails.getStockAmountOpened()
                : stockItem.getAmountOpened();
        StringBuilder stringBuilderAmount = new StringBuilder(
                activity.getString(
                        R.string.subtitle_amount,
                        amount,
                        amount == 1 ? quantityUnit.getName() : quantityUnit.getNamePlural()
                )
        );
        if(opened > 0) {
            stringBuilderAmount.append(" ");
            stringBuilderAmount.append(activity.getString(R.string.subtitle_amount_opened, opened));
        }
        return stringBuilderAmount.toString();
    }

    private String getAggregatedAmount() {
        int amountAggregated = hasDetails()
                ? productDetails.getStockAmountAggregated()
                : stockItem.getAmountAggregated();
        return "∑ " + activity.getString(
                R.string.subtitle_amount,
                amountAggregated,
                amountAggregated == 1
                        ? quantityUnit.getName()
                        : quantityUnit.getNamePlural()
        );
    }

    private void disableActions(ActionButton... actionButtons) {
        for(ActionButton actionButton : actionButtons) {
            actionButton.refreshState(false);
        }
    }

    @Override
    public int getItemCount() {
        return hasDetails() ? productDetails.getPropertyCount() : stockItem.getPropertyCount();
    }

    private boolean hasDetails() {
        if(stockItem == null && productDetails != null) {
            Log.i(TAG, "hasDetails: " + productDetails.getProduct().getName());
            return true;
        } else {
            assert stockItem != null;
            return false;
        }
    }

    private void expandContainer(LinearLayout container) {
        container.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = container.getMeasuredHeight();

        // Set initial height to 0 and show the view
        container.getLayoutParams().height = 0;
        container.setVisibility(View.VISIBLE);

        ValueAnimator anim = ValueAnimator.ofInt(container.getMeasuredHeight(), targetHeight);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(1000);
        anim.addUpdateListener(animation -> {
            ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
            layoutParams.height = (int) (targetHeight * animation.getAnimatedFraction());
            container.setLayoutParams(layoutParams);
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // At the end of animation, set the height to wrap content
                // This fix is for long views that are not shown on screen
                ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
        });
        anim.start();
    }
}
