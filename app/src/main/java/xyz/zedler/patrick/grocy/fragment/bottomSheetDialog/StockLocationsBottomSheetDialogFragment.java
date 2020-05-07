package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.adapter.StockLocationAdapter;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.util.Constants;

public class StockLocationsBottomSheetDialogFragment
        extends BottomSheetDialogFragment
        implements StockLocationAdapter.StockLocationAdapterListener {

    private final static String TAG = "StockLocationsBottomSheet";

    private Activity activity;
    private ArrayList<StockLocation> stockLocations;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_stock_locations, container, false
        );

        activity = getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        stockLocations = bundle.getParcelableArrayList(Constants.ARGUMENT.STOCK_LOCATIONS);
        ProductDetails productDetails = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_DETAILS);
        int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, 0);

        TextView textViewSubtitle = view.findViewById(R.id.text_stock_locations_subtitle);
        if(productDetails != null) {
            textViewSubtitle.setText(
                    activity.getString(
                            R.string.subtitle_stock_locations,
                            productDetails.getProduct().getName()
                    )
            );
        } else {
            textViewSubtitle.setVisibility(View.GONE);
        }

        MaterialButton button = view.findViewById(R.id.button_stock_locations_discard);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_stock_locations);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(
                new StockLocationAdapter(
                        stockLocations, productDetails, selected, this
                )
        );

        if(activity.getClass() == ScanBatchActivity.class) {
            setCancelable(false);
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(
                    v -> {
                        ((ScanBatchActivity) activity).discardCurrentProduct();
                        dismiss();
                    }
            );
        }

        return view;
    }

    @Override
    public void onItemRowClicked(int position) {
        if(activity.getClass() == MainActivity.class) {
            Fragment currentFragment = ((MainActivity) activity).getCurrentFragment();
            if(currentFragment.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) currentFragment).selectLocation(
                        stockLocations.get(position).getLocationId()
                );
            }
        } else if(activity.getClass() == ScanBatchActivity.class) {
            ((ScanBatchActivity) activity).setStockLocationId(
                    String.valueOf(stockLocations.get(position).getLocationId())
            );
            ((ScanBatchActivity) activity).askNecessaryDetails();
        }

        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
