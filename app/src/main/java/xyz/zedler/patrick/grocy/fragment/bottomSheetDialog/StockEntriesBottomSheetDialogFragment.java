package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import xyz.zedler.patrick.grocy.adapter.StockEntryAdapter;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.util.Constants;

public class StockEntriesBottomSheetDialogFragment
        extends BottomSheetDialogFragment
        implements StockEntryAdapter.StockEntryAdapterListener {

    private final static String TAG = "ProductEntriesBottomSheet";

    private Activity activity;
    private ArrayList<StockEntry> stockEntries;

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
                R.layout.fragment_bottomsheet_stock_entries, container, false
        );

        activity = getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        stockEntries = bundle.getParcelableArrayList(Constants.ARGUMENT.STOCK_ENTRIES);
        String selectedStockId = bundle.getString(Constants.ARGUMENT.SELECTED_ID);

        MaterialButton button = view.findViewById(R.id.button_stock_entries_discard);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_stock_entries);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(
                new StockEntryAdapter(
                        activity, stockEntries, selectedStockId, this
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
                ((ConsumeFragment) currentFragment).selectStockEntry(
                        stockEntries.get(position).getStockId()
                );
            }
        } else if(activity.getClass() == ScanBatchActivity.class) {
            ((ScanBatchActivity) activity).setEntryId(
                    stockEntries.get(position).getStockId()
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
