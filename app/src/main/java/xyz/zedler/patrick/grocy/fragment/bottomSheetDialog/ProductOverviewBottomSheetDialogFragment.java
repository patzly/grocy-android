package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.ExpandableCard;
import xyz.zedler.patrick.grocy.view.ListItem;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ProductOverviewBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static boolean DEBUG = false;
	private final static String TAG = "ProductBottomSheet";

	private SharedPreferences sharedPrefs;

	private MainActivity activity;
	private StockItem stockItem;
	private ProductDetails productDetails;
	private Product product;
	private QuantityUnit quantityUnit;
	private Location location;
	private ActionButton actionButtonConsume, actionButtonOpen;
	private boolean setUpWithProductDetails = false, showActions = false;
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

		Bundle bundle = getArguments();
		if(bundle != null) {
			setUpWithProductDetails = bundle.getBoolean(
					Constants.ARGUMENT.SET_UP_WITH_PRODUCT_DETAILS,
					false
			);
			showActions = bundle.getBoolean(
					Constants.ARGUMENT.SHOW_ACTIONS,
					false
			);

			// setup in CONSUME is with ProductDetails, in STOCK with StockItem

			if(setUpWithProductDetails) {
				productDetails = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_DETAILS);
				assert productDetails != null;
				product = productDetails.getProduct();
			} else {
				stockItem = bundle.getParcelable(Constants.ARGUMENT.STOCK_ITEM);
				quantityUnit = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);
				location = bundle.getParcelable(Constants.ARGUMENT.LOCATION);
				product = stockItem.getProduct();
			}
		}

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

		refreshItems();

		((TextView) view.findViewById(R.id.text_product_overview_name)).setText(product.getName());

		if(product.getPictureFileName() != null) {
			Picasso.get().load(
					new GrocyApi(activity).getPicture(product.getPictureFileName(), 300)
			).into((ImageView) view.findViewById(R.id.image_product_overview));
		} else {
			view.findViewById(
					R.id.linear_product_overview_picture_container
			).setVisibility(View.GONE);
		}

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_product_overview);
		boolean isInStock = setUpWithProductDetails
				? productDetails.getStockAmount() > 0
				: stockItem.getAmount() > 0;
		toolbar.getMenu().findItem(R.id.action_consume_all).setEnabled(isInStock);
		toolbar.getMenu().findItem(R.id.action_consume_spoiled).setEnabled(
				isInStock && product.getEnableTareWeightHandling() == 0
		);
		toolbar.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
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
				case R.id.action_edit_product:
					Bundle bundle1 = new Bundle();
					bundle1.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.EDIT);
					bundle1.putParcelable(Constants.ARGUMENT.PRODUCT, product);
					activity.replaceFragment(
							Constants.UI.MASTER_PRODUCT_EDIT_SIMPLE,
							bundle1,
							true
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
		String description = TextUtil.getFromHtml(product.getDescription());
		if(description != null) {
			cardDescription.setText(description);
		} else {
			cardDescription.setVisibility(View.GONE);
		}

		// ACTIONS

		if(!showActions) {
			// hide actions when set up from CONSUME with productDetails
			view.findViewById(
					R.id.linear_product_overview_action_container
			).setVisibility(View.GONE);
			toolbar.setVisibility(View.GONE);
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
			// TODO: global queue
			new WebRequest(activity.getRequestQueue()).get(
					activity.getGrocy().getStockProductDetails(product.getId()),
					response -> {
						Type listType = new TypeToken<ProductDetails>(){}.getType();
						productDetails = new Gson().fromJson(response, listType);
						refreshButtonStates(true);
						refreshItems();
					},
					error -> { }
			);
		}

		return view;
	}

	private void refreshItems() {
		DateUtil dateUtil = new DateUtil(activity);

		// quantity unit refresh for an up-to-date value (productDetails has it in it)
		if(hasDetails()) {
			quantityUnit = productDetails.getQuantityUnitStock();
		}
		// aggregated amount
		int isAggregatedAmount = hasDetails()
				? productDetails.getIsAggregatedAmount()
				: stockItem.getIsAggregatedAmount();
		// location refresh for an up-to-date value (productDetails has it in it)
		if(hasDetails()) {
			location = productDetails.getLocation();
		}
		// best before
		String bestBefore = hasDetails()
				? productDetails.getNextBestBeforeDate()
				: stockItem.getBestBeforeDate();
		if(bestBefore == null) bestBefore = ""; // for "never" from dateUtil

		// AMOUNT
		itemAmount.setText(
				activity.getString(R.string.property_amount),
				getAmountText(),
				isAggregatedAmount == 1 ? getAggregatedAmount() : null
		);

		// LOCATION
		itemLocation.setText(
				activity.getString(R.string.property_location_default),
				location != null ? location.getName() : "",
				null
		);

		// BEST BEFORE
		itemBestBefore.setText(
				activity.getString(R.string.property_bbd_next),
				!bestBefore.equals(Constants.DATE.NEVER_EXPIRES)
						? dateUtil.getLocalizedDate(bestBefore)
						: activity.getString(R.string.date_never),
				!bestBefore.equals(Constants.DATE.NEVER_EXPIRES) && !bestBefore.equals("")
						? dateUtil.getHumanForDaysFromNow(bestBefore)
						: null
		);

		if(hasDetails()) {
			// LAST PURCHASED
			if(setUpWithProductDetails) itemLastPurchased.setVisibility(View.VISIBLE);
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
			if(setUpWithProductDetails) itemLastUsed.setVisibility(View.VISIBLE);
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
			if(lastPrice != null) {
				if(setUpWithProductDetails) itemLastPrice.setVisibility(View.VISIBLE);
				itemLastPrice.setText(
						activity.getString(R.string.property_last_price),
						lastPrice + " " + sharedPrefs.getString(
								Constants.PREF.CURRENCY,
								activity.getString(R.string.setting_currency_default)
						), null
				);
			}

			// SHELF LIFE
			int shelfLife = productDetails.getAverageShelfLifeDays();
			if(shelfLife != 0 && shelfLife != -1) {
				if(setUpWithProductDetails) itemShelfLife.setVisibility(View.VISIBLE);
				itemShelfLife.setText(
						activity.getString(R.string.property_average_shelf_life),
						dateUtil.getHumanFromDays(shelfLife),
						null
				);
			}

			// SPOIL RATE
			if(setUpWithProductDetails) itemSpoilRate.setVisibility(View.VISIBLE);
			itemSpoilRate.setText(
					activity.getString(R.string.property_spoil_rate),
					NumUtil.trim(productDetails.getSpoilRatePercent()) + "%",
					null
			);
		}
	}

	private void refreshButtonStates(boolean animated) {
		boolean consume = hasDetails()
				? productDetails.getStockAmount() > 0
					&& productDetails.getProduct().getEnableTareWeightHandling() == 0
				: stockItem.getAmount() > 0
					&& stockItem.getProduct().getEnableTareWeightHandling() == 0;
		boolean open = hasDetails()
				? productDetails.getStockAmount()
					> productDetails.getStockAmountOpened()
					&& productDetails.getProduct().getEnableTareWeightHandling() == 0
				: stockItem.getAmount()
					> stockItem.getAmountOpened()
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

	private boolean hasDetails() {
		return productDetails != null;
	}

	private String getAmountText() {
		double amount = hasDetails() ? productDetails.getStockAmount() : stockItem.getAmount();
		double opened = hasDetails()
				? productDetails.getStockAmountOpened()
				: stockItem.getAmountOpened();
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
		double amountAggregated = hasDetails()
				? productDetails.getStockAmountAggregated()
				: stockItem.getAmountAggregated();
		return "∑ " + activity.getString(
				R.string.subtitle_amount,
				NumUtil.trim(amountAggregated),
				amountAggregated == 1
						? quantityUnit.getName()
						: quantityUnit.getNamePlural()
		);
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
