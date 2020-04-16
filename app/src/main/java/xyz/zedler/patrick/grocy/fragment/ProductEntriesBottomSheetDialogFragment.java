package xyz.zedler.patrick.grocy.fragment;

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
import xyz.zedler.patrick.grocy.adapter.ProductEntryAdapter;
import xyz.zedler.patrick.grocy.model.ProductEntries;
import xyz.zedler.patrick.grocy.util.Constants;

public class ProductEntriesBottomSheetDialogFragment
        extends BottomSheetDialogFragment
        implements ProductEntryAdapter.ProductEntryAdapterListener {

    private final static boolean DEBUG = false;
    private final static String TAG = "ProductEntriesBottomSheet";

    private MainActivity activity;
    private ProductEntries productEntries;

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
                R.layout.fragment_bottomsheet_product_entries, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        productEntries = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_ENTRIES);
        int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, 0);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_product_entries);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(
                new ProductEntryAdapter(
                        productEntries, selected, this
                )
        );

        return view;
    }

    @Override
    public void onItemRowClicked(int position) {
        Fragment currentFragment = activity.getCurrentFragment();
        if(currentFragment.getClass() == ConsumeFragment.class) {
            ((ConsumeFragment) currentFragment).selectProductEntry(
                    productEntries.get(position).getId() // TODO: what id
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
