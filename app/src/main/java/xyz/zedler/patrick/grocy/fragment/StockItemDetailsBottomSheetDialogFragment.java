package xyz.zedler.patrick.grocy.fragment;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
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
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.ExpandableCard;
import xyz.zedler.patrick.grocy.view.StockItemDetailsItem;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class StockItemDetailsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static boolean DEBUG = false;
	private final static String TAG = "ProductBottomSheet";

	private SharedPreferences sharedPrefs;

	private BottomSheetDialog bottomSheet;
	private MainActivity activity;
	private StockItem stockItem;
	private ProductDetails productDetails;
	private QuantityUnit quantityUnit;
	private Location location;
	private ActionButton actionButtonConsume, actionButtonOpen;
	private StockItemDetailsItem
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
		bottomSheet = new BottomSheetDialog(
				requireContext(),
				R.style.Theme_Grocy_BottomSheetDialog
		);
		return bottomSheet;
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState
	) {
		View view = inflater.inflate(
				R.layout.fragment_bottomsheet_stock_item_details,
				container,
				false
		);

		activity = (MainActivity) getActivity();
		assert activity != null;

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

		Bundle bundle = getArguments();
		if(bundle != null) {
			stockItem = bundle.getParcelable(Constants.ARGUMENT.STOCK_ITEM);
			quantityUnit = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);
			location = bundle.getParcelable(Constants.ARGUMENT.LOCATION);
		}

		// VIEWS

		actionButtonConsume = view.findViewById(R.id.button_stock_item_details_consume);
		actionButtonOpen = view.findViewById(R.id.button_stock_item_details_open);
		itemAmount = view.findViewById(R.id.stock_item_details_item_amount);
		itemLocation = view.findViewById(R.id.stock_item_details_item_location);
		itemBestBefore = view.findViewById(R.id.stock_item_details_item_bbd);
		itemLastPurchased = view.findViewById(R.id.stock_item_details_item_last_purchased);
		itemLastUsed = view.findViewById(R.id.stock_item_details_item_last_used);
		itemLastPrice = view.findViewById(R.id.stock_item_details_item_last_price);
		itemShelfLife = view.findViewById(R.id.stock_item_details_item_shelf_life);
		itemSpoilRate = view.findViewById(R.id.stock_item_details_item_spoil_rate);

		refreshItems();

		((TextView) view.findViewById(R.id.text_stock_item_details_name)).setText(
				stockItem.getProduct().getName()
		);

		Picasso.get().load(
				new GrocyApi(activity).getPicture(
						stockItem.getProduct().getPictureFileName(),
						300
				)
		).into((ImageView) view.findViewById(R.id.image_stock_item_details));

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_stock_item_details);
		toolbar.getMenu().findItem(R.id.action_consume_all).setEnabled(
				stockItem.getAmount() > 0
		);
		toolbar.getMenu().findItem(R.id.action_consume_spoiled).setEnabled(
				stockItem.getAmount() > 0
		);
		toolbar.setOnMenuItemClickListener(item -> {
			switch (item.getItemId()) {
				case R.id.action_consume_all:
					((StockFragment) activity.getCurrentFragment()).performAction(
							Constants.ACTION.CONSUME_ALL,
							stockItem.getProduct().getId()
					);
					bottomSheet.dismiss();
					return true;
				case R.id.action_consume_spoiled:
					((StockFragment) activity.getCurrentFragment()).performAction(
							Constants.ACTION.CONSUME_SPOILED,
							stockItem.getProduct().getId()
					);
					bottomSheet.dismiss();
					return true;
			}
			return false;
		});

		// DESCRIPTION

		ExpandableCard cardDescription = view.findViewById(
				R.id.card_stock_item_details_description
		);
		if(stockItem.getProduct().getDescription() != null
				&& !stockItem.getProduct().getDescription().trim().equals("")
		) {
			cardDescription.setText(
					Html.fromHtml(stockItem.getProduct().getDescription()).toString().trim()
			);
		} else {
			cardDescription.setVisibility(View.GONE);
		}

		// ACTIONS

		refreshButtonStates(false);
		actionButtonConsume.setOnClickListener(v -> {
			disableActions();
			((StockFragment) activity.getCurrentFragment()).performAction(
					Constants.ACTION.CONSUME,
					stockItem.getProduct().getId()
			);
			bottomSheet.dismiss();
		});
		actionButtonOpen.setOnClickListener(v -> {
			disableActions();
			((StockFragment) activity.getCurrentFragment()).performAction(
					Constants.ACTION.OPEN,
					stockItem.getProduct().getId()
			);
			bottomSheet.dismiss();
		});
		// tooltips
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			actionButtonConsume.setTooltipText(
					activity.getString(
							R.string.action_consume_one,
							quantityUnit.getName(),
							stockItem.getProduct().getName()
					)
			);
			actionButtonOpen.setTooltipText(
					activity.getString(
							R.string.action_open_one,
							quantityUnit.getName(),
							stockItem.getProduct().getName()
					)
			);
			// TODO: tooltip colors
		}
		// no margin if description is != null
		if(stockItem.getProduct().getDescription() != null
				&& !stockItem.getProduct().getDescription().trim().equals("")
		) {
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			layoutParams.setMargins(0, 0, 0, 0);
			view.findViewById(R.id.linear_stock_item_details_amount).setLayoutParams(layoutParams);
		}

		// LOAD DETAILS

		if(activity.isOnline()) {
			new WebRequest(activity.getRequestQueue()).get(
					activity.getGrocy().getStockProduct(stockItem.getProduct().getId()),
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
				activity.getString(R.string.property_default_location),
				location != null ? location.getName() : "",
				null
		);

		// BEST BEFORE
		itemBestBefore.setText(
				activity.getString(R.string.property_bbd_next),
				!bestBefore.equals("2999-12-31")
						? dateUtil.getLocalizedDate(bestBefore)
						: activity.getString(R.string.date_never),
				!bestBefore.equals("2999-12-31") && !bestBefore.equals("")
						? dateUtil.getHumanForDaysFromNow(bestBefore)
						: null
		);

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
			if(lastPrice != null) {
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
