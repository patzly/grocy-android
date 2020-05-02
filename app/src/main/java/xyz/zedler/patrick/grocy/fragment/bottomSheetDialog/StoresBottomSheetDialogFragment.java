package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.StoreAdapter;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;

public class StoresBottomSheetDialogFragment
        extends BottomSheetDialogFragment implements StoreAdapter.StoreAdapterListener {

    private final static boolean DEBUG = false;
    private final static String TAG = "StoresBottomSheet";

    private MainActivity activity;
    private ArrayList<Store> stores;

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
                R.layout.fragment_bottomsheet_master_edit_selection, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        stores = bundle.getParcelableArrayList(Constants.ARGUMENT.STORES);
        int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, -1);

        TextView textViewTitle = view.findViewById(R.id.text_master_edit_selection_title);
        textViewTitle.setText(activity.getString(R.string.property_stores));

        RecyclerView recyclerView = view.findViewById(R.id.recycler_master_edit_selection);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(
                new StoreAdapter(
                        stores, selected, this
                )
        );

        return view;
    }

    @Override
    public void onItemRowClicked(int position) {
        Fragment currentFragment = activity.getCurrentFragment();
        if(currentFragment.getClass() == PurchaseFragment.class) {
            ((PurchaseFragment) currentFragment).selectStore(
                    stores.get(position).getId()
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
