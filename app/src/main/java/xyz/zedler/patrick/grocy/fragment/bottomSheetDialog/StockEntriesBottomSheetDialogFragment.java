package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.StockEntryAdapter;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.model.StockEntries;
import xyz.zedler.patrick.grocy.util.Constants;

public class StockEntriesBottomSheetDialogFragment
        extends BottomSheetDialogFragment
        implements StockEntryAdapter.StockEntryAdapterListener {

    private final static boolean DEBUG = false;
    private final static String TAG = "ProductEntriesBottomSheet";

    private MainActivity activity;
    private StockEntries stockEntries;

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

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        stockEntries = bundle.getParcelable(Constants.ARGUMENT.STOCK_ENTRIES);
        String selectedStockId = bundle.getString(Constants.ARGUMENT.SELECTED_ID);

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

        return view;
    }

    @Override
    public void onItemRowClicked(int position) {
        Fragment currentFragment = activity.getCurrentFragment();
        if(currentFragment.getClass() == ConsumeFragment.class) {
            ((ConsumeFragment) currentFragment).selectStockEntry(
                    stockEntries.get(position).getStockId()
            );
        }
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
