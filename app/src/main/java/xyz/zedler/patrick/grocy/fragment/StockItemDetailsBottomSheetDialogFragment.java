package xyz.zedler.patrick.grocy.fragment;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Type;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.StockItemDetailsItemAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class StockItemDetailsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static boolean DEBUG = false;
	private final static String TAG = "ProductBottomSheet";

	private MainActivity activity;
	private StockItem stockItem;
	private List<QuantityUnit> quantityUnits;
	private List<Location> locations;
	private MaterialCardView cardViewDescription;
	private int descriptionHeight;

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
				R.layout.fragment_bottomsheet_stock_item_details,
				container,
				false
		);

		Context context = getContext();
		activity = (MainActivity) getActivity();
		assert context != null && activity != null;

		((TextView) view.findViewById(R.id.text_stock_item_details_name)).setText(
				stockItem.getProduct().getName()
		);

		Picasso.get().load(
				new GrocyApi(context).getPicture(
						stockItem.getProduct().getPictureFileName(),
						300
				)
		).into((ImageView) view.findViewById(R.id.image_stock_item_details));

		cardViewDescription = view.findViewById(
				R.id.card_stock_item_details_description
		);
		if(stockItem.getProduct().getDescription() != null
				&& !stockItem.getProduct().getDescription().trim().equals("")
		) {
			TextView textViewDescription = view.findViewById(
					R.id.text_stock_item_details_description
			);
			textViewDescription.setText(
					Html.fromHtml(stockItem.getProduct().getDescription()).toString().trim()
			);
			cardViewDescription.setOnClickListener(v -> {
				cardViewDescription.measure(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT
				);
				/*descriptionHeight = cardViewDescription.getMeasuredHeight();
				Log.i(TAG, "animateDescription: " + cardViewDescription.getLayoutParams().height);*/
				textViewDescription.setMaxLines(textViewDescription.getMaxLines() == 3 ? 50 : 3);
				//animateDescriptionCard();
			});
		} else {
			cardViewDescription.setVisibility(View.GONE);
		}

		RecyclerView recyclerView = view.findViewById(R.id.recycler_stock_item_details);
		recyclerView.setLayoutManager(
				new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
		);
		recyclerView.setAdapter(
				new StockItemDetailsItemAdapter(context, stockItem, quantityUnits, locations)
		);
		if(stockItem.getProduct().getDescription() != null
				&& !stockItem.getProduct().getDescription().trim().equals("")
		) {
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			layoutParams.setMargins(0, 0, 0, 0);
			recyclerView.setLayoutParams(layoutParams);
		}

		// LOAD DETAILS

		if(activity.isOnline()) {
			new WebRequest(activity.getRequestQueue()).get(
					activity.getGrocy().getStockProduct(stockItem.getProduct().getId()),
					response -> {
						Type listType = new TypeToken<ProductDetails>(){}.getType();
						ProductDetails productDetails = new Gson().fromJson(response, listType);
						recyclerView.setAdapter(
								new StockItemDetailsItemAdapter(context, productDetails)
						);
					},
					error -> { }
			);
		}

		return view;
	}

	private void animateDescriptionCard() {
		cardViewDescription.measure(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		final int targetHeight = cardViewDescription.getMeasuredHeight();

		Log.i(TAG, "animateDescription: hh " + cardViewDescription.getMeasuredHeight());

		ValueAnimator anim = ValueAnimator.ofInt(descriptionHeight, targetHeight);
		anim.setInterpolator(new AccelerateInterpolator());
		anim.setDuration(1000);
		anim.addUpdateListener(animation -> {
			ViewGroup.LayoutParams layoutParams = cardViewDescription.getLayoutParams();
			layoutParams.height = (int) (animation.getAnimatedValue());
			cardViewDescription.setLayoutParams(layoutParams);
		});
		/*anim.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				ViewGroup.LayoutParams layoutParams = cardViewDescription.getLayoutParams();
				layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			}
		});*/
		anim.start();
	}

	public void setData(
			StockItem stockItem,
			List<QuantityUnit> quantityUnits,
			List<Location> locations
	) {
		this.stockItem = stockItem;
		this.quantityUnits = quantityUnits;
		this.locations = locations;
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
