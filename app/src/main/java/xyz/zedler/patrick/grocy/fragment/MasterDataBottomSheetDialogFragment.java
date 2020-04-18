package xyz.zedler.patrick.grocy.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterDataBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private final static boolean DEBUG = false;
    private final static String TAG = "MasterDataBottomSheet";

    private MainActivity activity;
    private View view;
    private String uiMode;
    private long lastClick = 0;

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
                R.id.linear_master_data_products
        );

        return view;
    }

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            view.findViewById(viewId).setOnClickListener(this);
        }
    }

    public void onClick(View v) {
        if(SystemClock.elapsedRealtime() - lastClick < 2000) return;
        lastClick = SystemClock.elapsedRealtime();

        switch(v.getId()) {
            case R.id.linear_master_data_products:
                if(!uiMode.startsWith(Constants.UI.MASTER_PRODUCTS)) {
                    replaceFragment(
                            Constants.FRAGMENT.MASTER_PRODUCTS,
                            Constants.UI.MASTER_PRODUCTS_DEFAULT
                    );
                }
                break;
        }
    }

    private void replaceFragment(String fragmentNew, String uiModeNew) {
        activity.replaceFragment(fragmentNew, null, true);
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
