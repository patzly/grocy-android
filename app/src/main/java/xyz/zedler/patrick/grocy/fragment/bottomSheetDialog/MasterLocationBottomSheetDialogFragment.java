package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.ListItem;

public class MasterLocationBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static boolean DEBUG = false;
	private final static String TAG = "MasterLocationBottomSheet";

	private MainActivity activity;
	private Location location;
	private ListItem itemName, itemDescription;

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
				R.layout.fragment_bottomsheet_master_location,
				container,
				false
		);

		activity = (MainActivity) getActivity();
		assert activity != null;

		Bundle bundle = getArguments();
		if(bundle != null) location = bundle.getParcelable(Constants.ARGUMENT.LOCATION);

		// VIEWS

		itemName = view.findViewById(R.id.item_master_location_name);
		itemDescription = view.findViewById(R.id.item_master_location_description);

		// TOOLBAR

		MaterialToolbar toolbar = view.findViewById(R.id.toolbar_master_location);
		toolbar.setOnMenuItemClickListener(item -> {
			Fragment fragmentCurrent = activity.getCurrentFragment();
			if(fragmentCurrent.getClass() != MasterLocationsFragment.class) return false;
			switch (item.getItemId()) {
				case R.id.action_edit:
					((MasterLocationsFragment) fragmentCurrent).editLocation(location);
					dismiss();
					return true;
				case R.id.action_delete:
					//((MasterProductsFragment) fragmentCurrent).checkForStock(product);
					dismiss();
					return true;
			}
			return false;
		});

		setData();

		return view;
	}

	private void setData() {
		// NAME
		itemName.setText(activity.getString(R.string.property_name), location.getName());

		// DESCRIPTION
		String description = TextUtil.getFromHtml(location.getDescription());
		if(description != null) {
			itemDescription.setSingleLine(false);
			itemDescription.setText(activity.getString(R.string.property_description), description);
		} else {
			itemDescription.setVisibility(View.GONE);
		}
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
