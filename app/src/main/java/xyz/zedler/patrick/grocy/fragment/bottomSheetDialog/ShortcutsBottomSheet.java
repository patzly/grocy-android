package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetShortcutsBinding;

public class ShortcutsBottomSheet extends BaseBottomSheet {

    private final static String TAG = ShortcutsBottomSheet.class.getSimpleName();

    private final static String SHOPPING_LIST = "shortcut_shopping_list";
    private final static String ADD_TO_SHOPPING_LIST = "shortcut_add_to_shopping_list";
    private final static String SHOPPING_MODE = "shortcut_shopping_mode";
    private final static String PURCHASE = "shortcut_purchase";
    private final static String CONSUME = "shortcut_consume";
    private final static String BATCH_MODE = "shortcut_batch_mode";

    private MainActivity activity;
    private FragmentBottomsheetShortcutsBinding binding;

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
        binding = FragmentBottomsheetShortcutsBinding
                .inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setSkipCollapsedInPortrait();
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) requireActivity();
        binding.setActivity(activity);
        binding.setSheet(this);

        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            binding.text.setText(R.string.msg_shortcuts_not_supported);
            binding.checkboxContainer.setVisibility(View.GONE);
            binding.save.setVisibility(View.GONE);
            return;
        }

        ShortcutManager shortcutManager = activity.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcutInfos = shortcutManager.getDynamicShortcuts();
        for(ShortcutInfo shortcutInfo : shortcutInfos) {
            if(shortcutInfo.getId().equals(SHOPPING_LIST)) {
                setCheckBoxChecked(R.id.shopping_list);
            } else if(shortcutInfo.getId().equals(ADD_TO_SHOPPING_LIST)) {
                setCheckBoxChecked(R.id.add_to_shopping_list);
            } else if(shortcutInfo.getId().equals(SHOPPING_MODE)) {
                setCheckBoxChecked(R.id.shopping_mode);
            } else if(shortcutInfo.getId().equals(PURCHASE)) {
                setCheckBoxChecked(R.id.purchase);
            } else if(shortcutInfo.getId().equals(CONSUME)) {
                setCheckBoxChecked(R.id.consume);
            } else if(shortcutInfo.getId().equals(BATCH_MODE)) {
                setCheckBoxChecked(R.id.batch_mode);
            }
        }

        checkLimitReached();
    }

    public void checkLimitReached() {
        int countEnabled = 0;
        for(int i=0; i<=binding.checkboxContainer.getChildCount(); i++) {
            MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
            if (checkBox == null) continue;
            if (checkBox.isChecked()) countEnabled++;
        }
        for(int i=0; i<=binding.checkboxContainer.getChildCount(); i++) {
            MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
            if (checkBox == null) continue;
            checkBox.setEnabled(countEnabled < 4 || checkBox.isChecked());
        }
    }

    private void setCheckBoxChecked(@IdRes int id) {
        View view = binding.checkboxContainer.findViewById(id);
        if(view != null) ((MaterialCheckBox) view).setChecked(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public void saveShortcuts() {
        ShortcutManager shortcutManager = activity.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcutInfos = new ArrayList<>();
        for(int i=0; i<=binding.checkboxContainer.getChildCount(); i++) {
            MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
            if (checkBox == null || !checkBox.isChecked()) continue;
            if(checkBox.getId() == R.id.shopping_list) {
                shortcutInfos.add(createShortcutShoppingList(checkBox.getText()));
            } else if(checkBox.getId() == R.id.add_to_shopping_list) {
                shortcutInfos.add(createShortcutAddToShoppingList(checkBox.getText()));
            } else if(checkBox.getId() == R.id.shopping_mode) {
                shortcutInfos.add(createShortcutShoppingMode(checkBox.getText()));
            } else if(checkBox.getId() == R.id.purchase) {
                shortcutInfos.add(createShortcutPurchase(checkBox.getText()));
            } else if(checkBox.getId() == R.id.consume) {
                shortcutInfos.add(createShortcutConsume(checkBox.getText()));
            } else if(checkBox.getId() == R.id.batch_mode) {
                shortcutInfos.add(createShortcutBatchMode(checkBox.getText()));
            }
        }

        shortcutManager.removeAllDynamicShortcuts();
        shortcutManager.setDynamicShortcuts(shortcutInfos);
        activity.getCurrentFragment().updateShortcuts();
        dismiss();
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShortcutShoppingList(CharSequence label) {
        Uri uri = Uri.parse(getString(R.string.deep_link_shoppingListFragment));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(requireContext(), MainActivity.class);
        return new ShortcutInfo.Builder(requireContext(), SHOPPING_LIST)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(requireContext(), R.mipmap.ic_shopping_list))
                .setIntent(intent).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShortcutAddToShoppingList(CharSequence label) {
        Uri uri = Uri.parse(getString(R.string.deep_link_shoppingListItemEditFragment));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(requireContext(), MainActivity.class);
        return new ShortcutInfo.Builder(requireContext(), ADD_TO_SHOPPING_LIST)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(requireContext(), R.mipmap.ic_add))
                .setIntent(intent).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShortcutShoppingMode(CharSequence label) {
        Uri uri = Uri.parse(getString(R.string.deep_link_shoppingModeFragment));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(requireContext(), MainActivity.class);
        return new ShortcutInfo.Builder(requireContext(), SHOPPING_MODE)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(requireContext(), R.mipmap.ic_shopping_mode))
                .setIntent(intent).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShortcutPurchase(CharSequence label) {
        Uri uri = Uri.parse(getString(R.string.deep_link_purchaseFragment));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(requireContext(), MainActivity.class);
        return new ShortcutInfo.Builder(requireContext(), PURCHASE)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(requireContext(), R.mipmap.ic_purchase))
                .setIntent(intent).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShortcutConsume(CharSequence label) {
        Uri uri = Uri.parse(getString(R.string.deep_link_consumeFragment));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(requireContext(), MainActivity.class);
        return new ShortcutInfo.Builder(requireContext(), CONSUME)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(requireContext(), R.mipmap.ic_consume))
                .setIntent(intent).build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private ShortcutInfo createShortcutBatchMode(CharSequence label) {
        Uri uri = Uri.parse(getString(R.string.deep_link_batchModeFragment));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(requireContext(), MainActivity.class);
        return new ShortcutInfo.Builder(requireContext(), BATCH_MODE)
                .setShortLabel(label)
                .setIcon(Icon.createWithResource(requireContext(), R.drawable.ic_round_barcode_scan))
                .setIntent(intent).build();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
