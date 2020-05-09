package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterDataBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private final static boolean DEBUG = false;
    private final static String TAG = "MasterDataBottomSheet";

    private MainActivity activity;
    private View view;
    private String uiMode;
    private ClickUtil clickUtil = new ClickUtil();

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
        view = inflater.inflate(
                R.layout.fragment_bottomsheet_master_data, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        uiMode = bundle.getString(Constants.ARGUMENT.UI_MODE, Constants.UI.STOCK_DEFAULT);

        setOnClickListeners(
                R.id.linear_master_data_products,
                R.id.linear_master_data_locations,
                R.id.linear_master_data_stores,
                R.id.linear_master_data_quantity_units,
                R.id.linear_master_data_product_groups
        );

        if(uiMode.startsWith(Constants.UI.MASTER_PRODUCTS)) {
            select(R.id.linear_master_data_products, R.id.text_master_data_products);
        } else if(uiMode.startsWith(Constants.UI.MASTER_LOCATIONS)) {
            select(R.id.linear_master_data_locations, R.id.text_master_data_locations);
        } else if(uiMode.startsWith(Constants.UI.MASTER_STORES)) {
            select(R.id.linear_master_data_stores, R.id.text_master_data_stores);
        } else if(uiMode.startsWith(Constants.UI.MASTER_QUANTITY_UNITS)) {
            select(R.id.linear_master_data_quantity_units, R.id.text_master_data_quantity_units);
        } else if(uiMode.startsWith(Constants.UI.MASTER_PRODUCT_GROUPS)) {
            select(R.id.linear_master_data_product_groups, R.id.text_master_data_product_groups);
        }

        return view;
    }

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            view.findViewById(viewId).setOnClickListener(this);
        }
    }

    public void onClick(View v) {
        if(clickUtil.isDisabled()) return;

        switch(v.getId()) {
            case R.id.linear_master_data_products:
                if(!uiMode.startsWith(Constants.UI.MASTER_PRODUCTS)) {
                    replaceFragment(Constants.UI.MASTER_PRODUCTS);
                }
                break;
            case R.id.linear_master_data_locations:
                if(!uiMode.startsWith(Constants.UI.MASTER_LOCATIONS)) {
                    replaceFragment(Constants.UI.MASTER_LOCATIONS);
                }
                break;
            case R.id.linear_master_data_stores:
                if(!uiMode.startsWith(Constants.UI.MASTER_STORES)) {
                    replaceFragment(Constants.UI.MASTER_STORES);
                }
                break;
            case R.id.linear_master_data_quantity_units:
                if(!uiMode.startsWith(Constants.UI.MASTER_QUANTITY_UNITS)) {
                    replaceFragment(Constants.UI.MASTER_QUANTITY_UNITS);
                }
                break;
            case R.id.linear_master_data_product_groups:
                if(!uiMode.startsWith(Constants.UI.MASTER_PRODUCT_GROUPS)) {
                    replaceFragment(Constants.UI.MASTER_PRODUCT_GROUPS);
                }
                break;
        }
    }

    private void select(@IdRes int linearLayoutId, @IdRes int textViewId) {
        view.findViewById(linearLayoutId).setBackgroundResource(R.drawable.bg_drawer_item_selected);
        ((TextView) view.findViewById(textViewId)).setTextColor(
                ContextCompat.getColor(activity, R.color.secondary)
        );
    }

    private void replaceFragment(String fragmentNew) {
        activity.replaceFragment(fragmentNew, null, true);
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
