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

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.adapter.LocationAdapter;
import xyz.zedler.patrick.grocy.fragment.MasterProductEditSimpleFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.util.Constants;

public class LocationsBottomSheetDialogFragment
        extends BottomSheetDialogFragment implements LocationAdapter.LocationAdapterListener {

    private final static boolean DEBUG = false;
    private final static String TAG = "LocationsBottomSheet";

    private Activity activity;
    private ArrayList<Location> locations;

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

        activity = getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        locations = bundle.getParcelableArrayList(Constants.ARGUMENT.LOCATIONS);
        int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, -1);

        TextView textViewTitle = view.findViewById(R.id.text_master_edit_selection_title);
        textViewTitle.setText(activity.getString(R.string.property_locations));

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
                new LocationAdapter(
                        locations, selected, this
                )
        );

        if(activity.getClass() == ScanBatchActivity.class) {
            setCancelable(false);
        }

        return view;
    }

    @Override
    public void onItemRowClicked(int position) {
        if(activity.getClass() == MainActivity.class) {
            Fragment currentFragment = ((MainActivity) activity).getCurrentFragment();
            if(currentFragment.getClass() == MasterProductEditSimpleFragment.class) {
                ((MasterProductEditSimpleFragment) currentFragment).selectLocation(
                        locations.get(position).getId()
                );
            } else if(currentFragment.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) currentFragment).selectLocation(
                        locations.get(position).getId()
                );
            }
        } else if(activity.getClass() == ScanBatchActivity.class) {
            String locationId = String.valueOf(locations.get(position).getId());
            ((ScanBatchActivity) activity).setLocationId(locationId);
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
