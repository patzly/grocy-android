package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
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
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.BezierCurveChart;
import xyz.zedler.patrick.grocy.view.ExpandableCard;
import xyz.zedler.patrick.grocy.view.ListItem;

public class ProductOverviewBottomSheet extends CustomBottomSheet {

	private final static String TAG = ProductOverviewBottomSheet.class.getSimpleName();

	private SharedPreferences sharedPrefs;

	private MainActivity activity;
	private StockItem stockItem;
	private ProductDetails productDetails;
	private Product product;
	private QuantityUnit quantityUnit;
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

		activity = (MainActivity) getActivity();
		assert activity != null;

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

		assert getArguments() != null;
		ProductOverviewBottomSheetArgs args =
				ProductOverviewBottomSheetArgs.fromBundle(getArguments());

		boolean showActions = args.getShowActions();

		// setup in CONSUME/PURCHASE with ProductDetails, in STOCK with StockItem

		if(args.getProductDetails() != null) {
			productDetails = args.getProductDetails();
			product = productDetails.getProduct();
			stockItem = new StockItem(productDetails);
		} else if(args.getStockItem() != null) {
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
		boolean isInStock = stockItem.getAmount() > 0;
		MenuCompat.setGroupDividerEnabled(toolbar.getMenu(), true);
		// disable consume actions if necessary
		toolbar.getMenu().findItem(R.id.action_consume_all).setEnabled(isInStock);
		toolbar.getMenu().findItem(R.id.action_consume_spoiled).setEnabled(
				isInStock && product.getEnableTareWeightHandling() == 0
		);
		toolbar.getMenu().findItem(R.id.action_consume).setEnabled(isInStock);
		toolbar.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
				case R.id.action_add_to_shopping_list:
					NavHostFragment.findNavController(this).navigate(
							ProductOverviewBottomSheetDirections
									.actionProductOverviewBottomSheetDialogFragmentToShoppingListItemEditFragment(
											Constants.ACTION.CREATE
									).setProductName(product.getName()));
					dismiss();
					return true;
				case R.id.action_consume_all:
					((StockFragment) activity.getCurrentFragment()).performAction(
							Constants.ACTION.CONSUME_ALL,
							product.getId()
					);
					dismiss();
					return true;
				case R.id.action_consume_spoiled:
					((StockFragment) activity.getCurrentFragment()).performAction(
							Constants.ACTION.CONSUME_SPOILED,
							product.getId()
					);
					dismiss();
					return true;
				case R.id.action_purchase:
					NavHostFragment.findNavController(this).navigate(
							ProductOverviewBottomSheetDirections
									.actionProductOverviewBottomSheetDialogFragmentToPurchaseFragment()
									.setCloseWhenFinished(true)
									.setProductName(product.getName()) // TODO: Doesn't work yet
					);
					dismiss();
					return true;
				case R.id.action_consume:
					NavHostFragment.findNavController(this).navigate(
							ProductOverviewBottomSheetDirections
									.actionProductOverviewBottomSheetDialogFragmentToConsumeFragment()
									.setCloseWhenFinished(true)
									.setProductName(product.getName())
					);
					dismiss();
					return true;
				case R.id.action_edit_product:
					NavHostFragment.findNavController(this).navigate(
							ProductOverviewBottomSheetDirections
									.actionProductOverviewBottomSheetDialogFragmentToMasterProductSimpleFragment(
											Constants.ACTION.EDIT
									).setProduct(product)
					);
					dismiss();
					return true;
			}
			return false;
		});

		// DESCRIPTION

		ExpandableCard cardDescription = view.findViewById(
				R.id.card_product_overview_description
		);
		Spanned description = product.getDescription() != null
				? Html.fromHtml(product.getDescription())
				: null;
		description = (Spanned) TextUtil.trimCharSequence(description);
		if(description != null && !description.toString().isEmpty()) {
			cardDescription.setText(description.toString());
		} else {
			cardDescription.setVisibility(View.GONE);
		}

		// ACTIONS

		if(!showActions) {
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
			((StockFragment) activity.getCurrentFragment()).performAction(
					Constants.ACTION.CONSUME,
					product.getId()
			);
			dismiss();
		});
		actionButtonOpen.setOnClickListener(v -> {
			disableActions();
			((StockFragment) activity.getCurrentFragment()).performAction(
					Constants.ACTION.OPEN,
					product.getId()
			);
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
		if(description != null) {
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			layoutParams.setMargins(0, 0, 0, 0);
			view.findViewById(R.id.linear_product_overview_amount).setLayoutParams(layoutParams);
		}

		// LOAD DETAILS

		if(activity.isOnline() && !hasDetails()) {
			dlHelper.get(
					activity.getGrocy().getStockProductDetails(product.getId()),
					response -> {
						Type listType = new TypeToken<ProductDetails>(){}.getType();
						productDetails = new Gson().fromJson(response, listType);
						stockItem = new StockItem(productDetails);
						refreshButtonStates(true);
						refreshItems();
						loadPriceHistory(view);
					},
					error -> { }
			);
		} else if(activity.isOnline() && hasDetails()) {
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
		if(hasDetails()) quantityUnit = productDetails.getQuantityUnitStock();
		// aggregated amount
		int isAggregatedAmount = stockItem.getIsAggregatedAmount();
		// best before
		String bestBefore = stockItem.getBestBeforeDate();
		if(bestBefore == null) bestBefore = ""; // for "never" from dateUtil

		// AMOUNT
		itemAmount.setText(
				activity.getString(R.string.property_amount),
				getAmountText(),
				isAggregatedAmount == 1 ? getAggregatedAmount() : null
		);
		itemAmount.setSingleLine(false);

		// LOCATION
		if(hasDetails()) location = productDetails.getLocation(); // refresh
		if(location != null && isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
			itemLocation.setText(
					activity.getString(R.string.property_location_default),
					location.getName(),
					null
			);
		} else {
			itemLocation.setVisibility(View.GONE);
		}

		// BEST BEFORE
		if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
			itemBestBefore.setText(
					activity.getString(R.string.property_bbd_next),
					!bestBefore.equals(Constants.DATE.NEVER_EXPIRES)
							? dateUtil.getLocalizedDate(bestBefore)
							: activity.getString(R.string.date_never),
					!bestBefore.equals(Constants.DATE.NEVER_EXPIRES) && !bestBefore.isEmpty()
							? dateUtil.getHumanForDaysFromNow(bestBefore)
							: null
			);
		}

		if(hasDetails()) {
			// LAST PURCHASED
			String lastPurchased = productDetails.getLastPurchased();
			itemLastPurchased.setText(
					activity.getString(R.string.property_last_purchased),
					lastPurchased != null
							? dateUtil.getLocalizedDate(lastPurchased)
							: activity.getString(R.string.date_never),
					lastPurchased != null
							? dateUtil.getHumanFromToday(
									DateUtil.getDaysFromNow(productDetails.getLastPurchased()))
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
			if(lastPrice != null && isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
				itemLastPrice.setText(
						activity.getString(R.string.property_last_price),
						lastPrice + " " + sharedPrefs.getString(
								Constants.PREF.CURRENCY,
								""
						), null
				);
			}

			// SHELF LIFE
			int shelfLife = productDetails.getAverageShelfLifeDays();
			if(shelfLife != 0 && shelfLife != -1 && isFeatureEnabled(
					Constants.PREF.FEATURE_STOCK_BBD_TRACKING
			)) {
				itemShelfLife.setText(
						activity.getString(R.string.property_average_shelf_life),
						dateUtil.getHumanFromDays(shelfLife),
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
		if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
			return;
		}
		dlHelper.get(
				activity.getGrocy().getPriceHistory(product.getId()),
				response -> {
					Type listType = new TypeToken<ArrayList<PriceHistoryEntry>>(){}.getType();
					ArrayList<PriceHistoryEntry> priceHistoryEntries;
					priceHistoryEntries = new Gson().fromJson(response, listType);
					if(priceHistoryEntries.isEmpty()) return;

					ArrayList<String> dates = new ArrayList<>();
					Collections.reverse(priceHistoryEntries);

					HashMap<String, ArrayList<BezierCurveChart.Point>> curveLists = new HashMap<>();

					for(PriceHistoryEntry priceHistoryEntry : priceHistoryEntries) {
						Store store = priceHistoryEntry.getStore();
						String storeName;
						if(store == null || store.getName().trim().isEmpty()) {
							storeName = activity.getString(R.string.property_store_unknown);
						} else {
							storeName = store.getName().trim();
						}
						if(!curveLists.containsKey(storeName)) {
							curveLists.put(storeName, new ArrayList<>());
						}
						ArrayList<BezierCurveChart.Point> curveList = curveLists.get(storeName);

						String date = new DateUtil(activity).getLocalizedDate(
								priceHistoryEntry.getDate(),
								DateUtil.FORMAT_SHORT
						);
						if(!dates.contains(date)) dates.add(date);
						assert curveList != null;
						curveList.add(new BezierCurveChart.Point(
								dates.indexOf(date),
								(float) priceHistoryEntry.getPrice()
						));
					}
					priceHistory.init(curveLists, dates);
					animateLinearPriceHistory(view);
				},
				error -> { }
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
		boolean consume = stockItem.getAmount() > 0
					&& stockItem.getProduct().getEnableTareWeightHandling() == 0;
		boolean open = stockItem.getAmount() > stockItem.getAmountOpened()
					&& stockItem.getProduct().getEnableTareWeightHandling() == 0;
		if(animated) {
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
		if(!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
			MaterialToolbar toolbar = view.findViewById(R.id.toolbar_product_overview);
			toolbar.getMenu().findItem(R.id.action_add_to_shopping_list).setVisible(false);
		}
		if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
			itemBestBefore.setVisibility(View.GONE);
		}
		if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)) {
			actionButtonOpen.setVisibility(View.GONE);
		}
	}

	private boolean hasDetails() {
		return productDetails != null;
	}

	private String getAmountText() {
		double amount = stockItem.getAmount();
		double opened = stockItem.getAmountOpened();
		StringBuilder stringBuilderAmount = new StringBuilder(
				activity.getString(
						R.string.subtitle_amount,
						NumUtil.trim(amount),
						amount == 1 ? quantityUnit.getName() : quantityUnit.getNamePlural()
				)
		);
		if(opened > 0) {
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
		double amountAggregated = stockItem.getAmountAggregated();
		return "∑ " + activity.getString(
				R.string.subtitle_amount,
				NumUtil.trim(amountAggregated),
				amountAggregated == 1
						? quantityUnit.getName()
						: quantityUnit.getNamePlural()
		);
	}

	private boolean isFeatureEnabled(String pref) {
		if(pref == null) return true;
		return sharedPrefs.getBoolean(pref, true);
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
