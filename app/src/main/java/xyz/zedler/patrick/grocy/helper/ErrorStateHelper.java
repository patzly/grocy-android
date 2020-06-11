package xyz.zedler.patrick.grocy.helper;

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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.PartialErrorBinding;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoresFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;

public class ErrorStateHelper {

    private Fragment fragment;
    private PartialErrorBinding partialErrorBinding;
    private Handler handler;
    private Animation animation;
    private Runnable displayError;
    private Runnable displayOffline;

    public ErrorStateHelper(Fragment fragment, PartialErrorBinding partialErrorBinding) {
        this.partialErrorBinding = partialErrorBinding;
        this.fragment = fragment;
        this.handler = new Handler();
        this.displayError = displayError();
        this.displayOffline = displayOffline();
    }

    public void clearState() {
        // hide container
        partialErrorBinding.linearError.animate().alpha(0).setDuration(125).withEndAction(
                () -> partialErrorBinding.linearError.setVisibility(View.GONE)
        ).start();
    }

    public void setError() {
        handler.postDelayed(displayError, 125);
        startAnimation();
    }

    public void setOffline() {
        handler.postDelayed(displayOffline, 125);
        startAnimation();
    }

    private void startAnimation() {
        LinearLayout container = partialErrorBinding.linearError;
        if(container.getVisibility() == View.VISIBLE) {
            // first hide previous empty state if needed
            container.animate().alpha(0).setDuration(125).start();
        }
        handler.postDelayed(() -> {
            container.setAlpha(0);
            container.setVisibility(View.VISIBLE);
            container.animate().alpha(1).setDuration(125).start();
        }, 150);
    }

    private Runnable displayError() {
        return () -> {
            partialErrorBinding.imageError.setImageResource(R.drawable.illustration_toast);

            TextView title = partialErrorBinding.textErrorTitle;
            if(fragment.getClass() == StockFragment.class) {
                title.setText(R.string.error_empty_stock);
            } else if(fragment.getClass() == MasterLocationsFragment.class) {
                title.setText(R.string.error_empty_locations);
            } else if(fragment.getClass() == MasterProductGroupsFragment.class) {
                title.setText(R.string.error_empty_product_groups);
            } else if(fragment.getClass() == MasterProductsFragment.class) {
                title.setText(R.string.error_empty_products);
            } else if(fragment.getClass() == MasterQuantityUnitsFragment.class) {
                title.setText(R.string.error_empty_qu);
            } else if(fragment.getClass() == MasterStoresFragment.class) {
                title.setText(R.string.error_empty_stores);
            } else if(fragment.getClass() == ShoppingListFragment.class) {
                title.setText(R.string.error_empty_shopping_list);
            }

            TextView subtitle = partialErrorBinding.textErrorSubtitle;
            if(fragment.getClass() == StockFragment.class) {
                subtitle.setText(R.string.error_empty_stock_sub);
            } else if(fragment.getClass() == ShoppingListFragment.class) {
                subtitle.setText(R.string.error_empty_shopping_list_sub);
            } else {
                subtitle.setText(R.string.error_empty_master_data_sub);
            }
        };
    }

    private Runnable displayOffline() {
        return () -> {
            partialErrorBinding.imageError.setImageResource(R.drawable.illustration_jar);
            partialErrorBinding.textErrorTitle.setText(R.string.error_search);
            partialErrorBinding.textErrorSubtitle.setText(R.string.error_search_sub);
        };
    }

    public void destroyInstance() {
        handler.removeCallbacks(displayError);
        handler.removeCallbacks(displayOffline);
        partialErrorBinding.linearError.animate().cancel();
        displayError = null;
        displayOffline = null;
        partialErrorBinding = null;
    }
}
