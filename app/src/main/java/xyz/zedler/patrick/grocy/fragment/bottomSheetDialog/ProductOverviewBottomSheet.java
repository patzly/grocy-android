/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.view.MenuCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.PriceHistoryEntry;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.BezierCurveChart;
import xyz.zedler.patrick.grocy.view.ExpandableCard;
import xyz.zedler.patrick.grocy.view.ListItem;

public class ProductOverviewBottomSheet extends BaseBottomSheet {

  private final static String TAG = ProductOverviewBottomSheet.class.getSimpleName();

  private SharedPreferences sharedPrefs;

  private MainActivity activity;
  private StockItem stockItem;
  private ProductDetails productDetails;
  private Product product;
  private QuantityUnit quantityUnit;
  private PluralUtil pluralUtil;
  private Location location;
  private ActionButton actionButtonConsume, actionButtonOpen;
  private BezierCurveChart priceHistory;
  private DownloadHelper dlHelper;
  private ListItem
      itemAmount,
      itemLocation,
      itemBestBefore,
      itemLastPurchased,
      itemLastUsed,
      itemLastPrice,
      itemShelfLife,
      itemSpoilRate;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    View view = inflater.inflate(
        R.layout.fragment_bottomsheet_product_overview,
        container,
        false
    );

    activity = (MainActivity) requireActivity();
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    pluralUtil = new PluralUtil(getResources().getConfiguration().locale);

    ProductOverviewBottomSheetArgs args =
        ProductOverviewBottomSheetArgs.fromBundle(requireArguments());

    boolean showActions = args.getShowActions();

    // setup in CONSUME/PURCHASE with ProductDetails, in STOCK with StockItem

    if (args.getProductDetails() != null) {
      productDetails = args.getProductDetails();
      product = productDetails.getProduct();
      stockItem = new StockItem(productDetails);
    } else if (args.getStockItem() != null) {
      stockItem = args.getStockItem();
      quantityUnit = args.getQuantityUnit();
      location = args.getLocation();
      product = stockItem.getProduct();
    }

    // WEB REQUESTS

    dlHelper = new DownloadHelper(activity, TAG);

    // VIEWS

    actionButtonConsume = view.findViewById(R.id.button_product_overview_consume);
    actionButtonOpen = view.findViewById(R.id.button_product_overview_open);
    itemAmount = view.findViewById(R.id.item_product_overview_amount);
    itemLocation = view.findViewById(R.id.item_product_overview_location);
    itemBestBefore = view.findViewById(R.id.item_product_overview_bbd);
    itemLastPurchased = view.findViewById(R.id.item_product_overview_last_purchased);
    itemLastUsed = view.findViewById(R.id.item_product_overview_last_used);
    itemLastPrice = view.findViewById(R.id.item_product_overview_last_price);
    itemShelfLife = view.findViewById(R.id.item_product_overview_shelf_life);
    itemSpoilRate = view.findViewById(R.id.item_product_overview_spoil_rate);
    priceHistory = view.findViewById(R.id.item_product_overview_price_history);

    refreshItems();

    ((TextView) view.findViewById(R.id.text_product_overview_name)).setText(product.getName());

    // TOOLBAR

    MaterialToolbar toolbar = view.findViewById(R.id.toolbar_product_overview);
    boolean isInStock = stockItem.getAmountDouble() > 0;
    MenuCompat.setGroupDividerEnabled(toolbar.getMenu(), true);
    // disable actions if necessary
    toolbar.getMenu().findItem(R.id.action_consume_all).setEnabled(isInStock);
    toolbar.getMenu().findItem(R.id.action_consume_spoiled).setEnabled(
        isInStock && product.getEnableTareWeightHandling() == 0
    );
    toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_add_to_shopping_list) {
        activity.showMessage(R.string.msg_not_implemented_yet);
				/*navigate(R.id.shoppingListItemEditFragment,
						new ShoppingListItemEditFragmentArgs.Builder(Constants.ACTION.CREATE)
								.setProductName(product.getName()).build().toBundle());*/
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_consume_all) {
        activity.getCurrentFragment().performAction(
            Constants.ACTION.CONSUME_ALL,
            stockItem
        );
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_consume_spoiled) {
        activity.getCurrentFragment().performAction(
            Constants.ACTION.CONSUME_SPOILED,
            stockItem
        );
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_edit_product) {
        String productId = String.valueOf(product.getId());
        navigateDeepLink(getString(R.string.deep_link_masterProductFragment),
            new MasterProductFragmentArgs.Builder(Constants.ACTION.EDIT)
                .setProductId(productId).build().toBundle());
        dismiss();
        return true;
      }
      return false;
    });

    Chip chipConsume = view.findViewById(R.id.chip_consume);
    chipConsume.setVisibility(isInStock ? View.VISIBLE : View.GONE);
    chipConsume.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToConsumeFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    Chip chipPurchase = view.findViewById(R.id.chip_purchase);
    chipPurchase.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToPurchaseFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    Chip chipTransfer = view.findViewById(R.id.chip_transfer);
    chipTransfer.setVisibility(isInStock && product.getEnableTareWeightHandling() == 0
        ? View.VISIBLE : View.GONE);
    chipTransfer.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToTransferFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    Chip chipInventory = view.findViewById(R.id.chip_inventory);
    chipInventory.setOnClickListener(v -> {
      NavHostFragment.findNavController(this).navigate(
          ProductOverviewBottomSheetDirections
              .actionProductOverviewBottomSheetDialogFragmentToInventoryFragment()
              .setCloseWhenFinished(true)
              .setProductId(String.valueOf(product.getId()))
      );
      dismiss();
    });

    if (!showActions) {
      view.findViewById(R.id.container_chips).setVisibility(View.GONE);
    }

    // DESCRIPTION

    ExpandableCard cardDescription = view.findViewById(
        R.id.card_product_overview_description
    );
    Spanned description = product.getDescription() != null
        ? Html.fromHtml(product.getDescription())
        : null;
    description = (Spanned) TextUtil.trimCharSequence(description);
    if (description != null && !description.toString().isEmpty()) {
      cardDescription.setText(description.toString());
    } else {
      cardDescription.setVisibility(View.GONE);
    }

    // ACTIONS

    if (!showActions) {
      // hide actions when set up with productDetails
      view.findViewById(R.id.linear_product_overview_action_container).setVisibility(
          View.GONE
      );
      // set info menu
      toolbar.getMenu().clear();
      toolbar.inflateMenu(R.menu.menu_actions_product_overview_info);
    }

    refreshButtonStates(false);
    actionButtonConsume.setOnClickListener(v -> {
      disableActions();
      activity.getCurrentFragment().performAction(Constants.ACTION.CONSUME, stockItem);
      dismiss();
    });
    actionButtonOpen.setOnClickListener(v -> {
      disableActions();
      activity.getCurrentFragment().performAction(Constants.ACTION.OPEN, stockItem);
      dismiss();
    });
    // tooltips
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      actionButtonConsume.setTooltipText(
          activity.getString(
              R.string.action_consume_one,
              quantityUnit.getName(),
              product.getName()
          )
      );
      actionButtonOpen.setTooltipText(
          activity.getString(
              R.string.action_open_one,
              quantityUnit.getName(),
              product.getName()
          )
      );
      // TODO: tooltip colors
    }
    // no margin if description is != null
    if (description != null) {
      LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
      );
      layoutParams.setMargins(0, 0, 0, 0);
      view.findViewById(R.id.linear_product_overview_amount).setLayoutParams(layoutParams);
    }

    // LOAD DETAILS

    if (activity.isOnline() && !hasDetails()) {
      dlHelper.get(
          activity.getGrocyApi().getStockProductDetails(product.getId()),
          response -> {
            Type listType = new TypeToken<ProductDetails>() {
            }.getType();
            productDetails = new Gson().fromJson(response, listType);
            stockItem = new StockItem(productDetails);
            refreshButtonStates(true);
            refreshItems();
            loadPriceHistory(view);
          },
          error -> {
          }
      );
    } else if (activity.isOnline() && hasDetails()) {
      loadPriceHistory(view);
    }

    hideDisabledFeatures(view);

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    dlHelper.destroy();
  }

  private void refreshItems() {
    DateUtil dateUtil = new DateUtil(activity);

    // quantity unit refresh for an up-to-date value (productDetails has it in it)
    if (hasDetails()) {
      quantityUnit = productDetails.getQuantityUnitStock();
    }
    // aggregated amount
    int isAggregatedAmount = stockItem.getIsAggregatedAmount();
    // best before
    String bestBefore = stockItem.getBestBeforeDate();
    if (bestBefore == null) {
      bestBefore = ""; // for "never" from dateUtil
    }

    // AMOUNT
    itemAmount.setText(
        activity.getString(R.string.property_amount),
        getAmountText(),
        isAggregatedAmount == 1 ? getAggregatedAmount() : null
    );
    itemAmount.setSingleLine(false);

    // LOCATION
    if (hasDetails()) {
      location = productDetails.getLocation(); // refresh
    }
    if (location != null && isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      itemLocation.setText(
          activity.getString(R.string.property_location_default),
          location.getName(),
          null
      );
    } else {
      itemLocation.setVisibility(View.GONE);
    }

    // BEST BEFORE
    if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
      itemBestBefore.setText(
          activity.getString(R.string.property_due_date_next),
          !bestBefore.equals(Constants.DATE.NEVER_OVERDUE)
              ? dateUtil.getLocalizedDate(bestBefore)
              : activity.getString(R.string.date_never),
          !bestBefore.equals(Constants.DATE.NEVER_OVERDUE) && !bestBefore.isEmpty()
              ? dateUtil.getHumanForDaysFromNow(bestBefore)
              : null
      );
    }

    if (hasDetails()) {
      // LAST PURCHASED
      String lastPurchased = productDetails.getLastPurchased();
      itemLastPurchased.setText(
          activity.getString(R.string.property_last_purchased),
          lastPurchased != null
              ? dateUtil.getLocalizedDate(lastPurchased)
              : activity.getString(R.string.date_never),
          lastPurchased != null
              ? dateUtil.getHumanForDaysFromNow(lastPurchased)
              : null
      );

      // LAST USED
      String lastUsed = productDetails.getLastUsed();
      itemLastUsed.setText(
          activity.getString(R.string.property_last_used),
          lastUsed != null
              ? dateUtil.getLocalizedDate(lastUsed)
              : activity.getString(R.string.date_never),
          lastUsed != null
              ? dateUtil.getHumanForDaysFromNow(lastUsed)
              : null
      );

      // LAST PRICE
      String lastPrice = productDetails.getLastPrice();
      if (NumUtil.isStringDouble(lastPrice) && isFeatureEnabled(
          Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
        itemLastPrice.setText(
            activity.getString(R.string.property_last_price),
            NumUtil.trimPrice(Double.parseDouble(lastPrice))
                + " " + sharedPrefs.getString(Constants.PREF.CURRENCY, ""),
            null
        );
      }

      // SHELF LIFE
      int shelfLife = productDetails.getAverageShelfLifeDays();
      if (shelfLife != 0 && shelfLife != -1 && isFeatureEnabled(
          Constants.PREF.FEATURE_STOCK_BBD_TRACKING
      )) {
        itemShelfLife.setText(
            activity.getString(R.string.property_average_shelf_life),
            dateUtil.getHumanDuration(shelfLife),
            null
        );
      }

      // SPOIL RATE
      itemSpoilRate.setText(
          activity.getString(R.string.property_spoil_rate),
          NumUtil.trim(productDetails.getSpoilRatePercent()) + "%",
          null
      );
    }
  }

  private void loadPriceHistory(View view) {
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
      return;
    }
    dlHelper.get(
        activity.getGrocyApi().getPriceHistory(product.getId()),
        response -> {
          Type listType = new TypeToken<ArrayList<PriceHistoryEntry>>() {
          }.getType();
          ArrayList<PriceHistoryEntry> priceHistoryEntries;
          priceHistoryEntries = new Gson().fromJson(response, listType);
          if (priceHistoryEntries.isEmpty()) {
            return;
          }

          ArrayList<String> dates = new ArrayList<>();
          Collections.reverse(priceHistoryEntries);

          HashMap<String, ArrayList<BezierCurveChart.Point>> curveLists = new HashMap<>();

          for (PriceHistoryEntry priceHistoryEntry : priceHistoryEntries) {
            Store store = priceHistoryEntry.getStore();
            String storeName;
            if (store == null || store.getName().trim().isEmpty()) {
              storeName = activity.getString(R.string.property_store_unknown);
            } else {
              storeName = store.getName().trim();
            }
            if (!curveLists.containsKey(storeName)) {
              curveLists.put(storeName, new ArrayList<>());
            }
            ArrayList<BezierCurveChart.Point> curveList = curveLists.get(storeName);

            String date = new DateUtil(activity).getLocalizedDate(
                priceHistoryEntry.getDate(),
                DateUtil.FORMAT_SHORT
            );
            if (!dates.contains(date)) {
              dates.add(date);
            }
            assert curveList != null;
            curveList.add(new BezierCurveChart.Point(
                dates.indexOf(date),
                (float) priceHistoryEntry.getPrice()
            ));
          }
          priceHistory.init(curveLists, dates);
          animateLinearPriceHistory(view);
        },
        error -> {
        }
    );
  }

  private void animateLinearPriceHistory(View view) {
    LinearLayout linearPriceHistory = view.findViewById(
        R.id.linear_product_overview_price_history
    );
    linearPriceHistory.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    int height = linearPriceHistory.getMeasuredHeight();
    linearPriceHistory.getLayoutParams().height = 0;
    linearPriceHistory.requestLayout();
    linearPriceHistory.setAlpha(0);
    linearPriceHistory.setVisibility(View.VISIBLE);
    linearPriceHistory.animate().alpha(1).setDuration(600).setStartDelay(100).start();
    linearPriceHistory.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            ValueAnimator heightAnimator = ValueAnimator.ofInt(0, height);
            heightAnimator.addUpdateListener(animation -> {
              linearPriceHistory.getLayoutParams().height = (int) animation
                  .getAnimatedValue();
              linearPriceHistory.requestLayout();
            });
            heightAnimator.setDuration(800).setInterpolator(
                new DecelerateInterpolator()
            );
            heightAnimator.start();
            if (linearPriceHistory.getViewTreeObserver().isAlive()) {
              linearPriceHistory.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        }
    );
  }

  private void refreshButtonStates(boolean animated) {
    boolean consume = stockItem.getAmountDouble() > 0
        && stockItem.getProduct().getEnableTareWeightHandling() == 0;
    boolean open = stockItem.getAmountDouble() > stockItem.getAmountOpenedDouble()
        && stockItem.getProduct().getEnableTareWeightHandling() == 0;
    if (animated) {
      actionButtonConsume.refreshState(consume);
      actionButtonOpen.refreshState(open);
    } else {
      actionButtonConsume.setState(consume);
      actionButtonOpen.setState(open);
    }
  }

  private void disableActions() {
    actionButtonConsume.refreshState(false);
    actionButtonOpen.refreshState(false);
  }

  private void hideDisabledFeatures(View view) {
    if (!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
      MaterialToolbar toolbar = view.findViewById(R.id.toolbar_product_overview);
      toolbar.getMenu().findItem(R.id.action_add_to_shopping_list).setVisible(false);
    }
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
      itemBestBefore.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)) {
      actionButtonOpen.setVisibility(View.GONE);
    }
  }

  private boolean hasDetails() {
    return productDetails != null;
  }

  private String getAmountText() {
    double amount = stockItem.getAmountDouble();
    double opened = stockItem.getAmountOpenedDouble();
    StringBuilder stringBuilderAmount = new StringBuilder(
        activity.getString(
            R.string.subtitle_amount,
            NumUtil.trim(amount),
            pluralUtil.getQuantityUnitPlural(quantityUnit, amount)
        )
    );
    if (opened > 0) {
      stringBuilderAmount.append(" ");
      stringBuilderAmount.append(
          activity.getString(
              R.string.subtitle_amount_opened,
              NumUtil.trim(opened)
          )
      );
    }
    return stringBuilderAmount.toString();
  }

  private String getAggregatedAmount() {
    double amountAggregated = stockItem.getAmountAggregatedDouble();
    return "∑ " + activity.getString(
        R.string.subtitle_amount,
        NumUtil.trim(amountAggregated),
        pluralUtil.getQuantityUnitPlural(quantityUnit, amountAggregated)
    );
  }

  private boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
