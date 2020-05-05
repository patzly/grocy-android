package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.SettingsActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class DrawerBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private final static String TAG = "DrawerBottomSheet";

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
                R.layout.fragment_bottomsheet_drawer, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        uiMode = bundle.getString(Constants.ARGUMENT.UI_MODE, Constants.UI.STOCK_DEFAULT);

        view.findViewById(R.id.button_drawer_batch_consume).setOnClickListener(v -> {
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.CONSUME);
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_CONSUME);
            new Handler().postDelayed(this::dismiss, 500);
        });

        view.findViewById(R.id.button_drawer_batch_purchase).setOnClickListener(v -> {
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.PURCHASE);
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_PURCHASE);
            new Handler().postDelayed(this::dismiss, 500);
        });

        setOnClickListeners(
                R.id.linear_drawer_consume,
                R.id.linear_drawer_purchase,
                R.id.linear_drawer_master_data,
                R.id.linear_settings,
                R.id.linear_feedback,
                R.id.linear_help
        );

        if(uiMode.equals(Constants.UI.CONSUME)) {
            select(R.id.linear_drawer_consume, R.id.text_drawer_consume);
        } else if(uiMode.equals(Constants.UI.PURCHASE)) {
            select(R.id.linear_drawer_purchase, R.id.text_drawer_purchase);
        } else if(uiMode.startsWith(Constants.UI.MASTER)) {
            select(R.id.linear_drawer_master_data, R.id.text_drawer_master_data);
        }

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
            case R.id.linear_drawer_consume:
                if(!uiMode.startsWith(Constants.UI.CONSUME)) {
                    replaceFragment(Constants.UI.CONSUME);
                }
                break;
            case R.id.linear_drawer_purchase:
                if(!uiMode.startsWith(Constants.UI.PURCHASE)) {
                    replaceFragment(Constants.UI.PURCHASE);
                }
                break;
            case R.id.linear_drawer_master_data:
                dismiss();
                Bundle bundle = new Bundle();
                // selection for master data sheet
                bundle.putString(Constants.ARGUMENT.UI_MODE, uiMode);
                activity.showBottomSheet(new MasterDataBottomSheetDialogFragment(), bundle);
                break;
            case R.id.linear_settings:
                startAnimatedIcon(R.id.image_settings);
                dismiss();
                startActivity(new Intent(activity, SettingsActivity.class));
                //new Handler().postDelayed(this::dismiss, 500);
                break;
            case R.id.linear_feedback:
                dismiss();
                activity.showBottomSheet(new FeedbackBottomSheetDialogFragment(), null);
                break;
            case R.id.linear_help:
                startAnimatedIcon(R.id.image_help);
                //startActivity(new Intent(activity, HelpActivity.class));
                new Handler().postDelayed(this::dismiss, 500);
                break;
        }
    }

    private void replaceFragment(String fragmentNew) {
        activity.replaceFragment(fragmentNew, null, true);
        dismiss();
    }

    private void select(@IdRes int linearLayoutId, @IdRes int textViewId) {
        view.findViewById(linearLayoutId).setBackgroundResource(R.drawable.bg_drawer_item_selected);
        ((TextView) view.findViewById(textViewId)).setTextColor(
                ContextCompat.getColor(activity, R.color.secondary)
        );
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        try {
            ((Animatable) ((ImageView) view.findViewById(viewId)).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(ImageView) requires AVD!");
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
