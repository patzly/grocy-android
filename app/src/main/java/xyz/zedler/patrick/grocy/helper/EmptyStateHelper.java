package xyz.zedler.patrick.grocy.helper;

import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.PartialEmptyBinding;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoresFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;

public class EmptyStateHelper {

    private Fragment fragment;
    private PartialEmptyBinding partialEmptyBinding;
    private Handler handler;
    private Runnable displayEmpty;
    private Runnable displayNoSearchResults;
    private Runnable displayNoFilterResults;

    public EmptyStateHelper(Fragment fragment, PartialEmptyBinding partialEmptyBinding) {
        this.partialEmptyBinding = partialEmptyBinding;
        this.fragment = fragment;
        this.handler = new android.os.Handler();
        this.displayEmpty = displayEmpty();
        this.displayNoSearchResults = displayNoSearchResults();
        this.displayNoFilterResults = displayNoFilterResults();
    }

    public void clearState() {
        // hide container
        partialEmptyBinding.linearEmpty.animate().alpha(0).setDuration(125).withEndAction(
                () -> partialEmptyBinding.linearEmpty.setVisibility(View.GONE)
        ).start();
    }

    public void setEmpty() {
        handler.postDelayed(displayEmpty, 125);
        startAnimation();
    }

    public void setNoSearchResults() {
        handler.postDelayed(displayNoSearchResults, 125);
        startAnimation();
    }

    public void setNoFilterResults() {
        handler.postDelayed(displayNoFilterResults, 125);
        startAnimation();
    }

    private void startAnimation() {
        LinearLayout container = partialEmptyBinding.linearEmpty;
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

    private Runnable displayEmpty() {
        return () -> {
            partialEmptyBinding.imageEmpty.setImageResource(R.drawable.illustration_toast);

            TextView title = partialEmptyBinding.textEmptyTitle;
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

            TextView subtitle = partialEmptyBinding.textEmptySubtitle;
            if(fragment.getClass() == StockFragment.class) {
                subtitle.setText(R.string.error_empty_stock_sub);
            } else if(fragment.getClass() == ShoppingListFragment.class) {
                subtitle.setText(R.string.error_empty_shopping_list_sub);
            } else {
                subtitle.setText(R.string.error_empty_master_data_sub);
            }
        };
    }

    private Runnable displayNoSearchResults() {
        return () -> {
            partialEmptyBinding.imageEmpty.setImageResource(R.drawable.illustration_jar);
            partialEmptyBinding.textEmptyTitle.setText(R.string.error_search);
            partialEmptyBinding.textEmptySubtitle.setText(R.string.error_search_sub);
        };
    }

    private Runnable displayNoFilterResults() {
        return () -> {
            partialEmptyBinding.imageEmpty.setImageResource(R.drawable.illustration_jar);
            partialEmptyBinding.textEmptyTitle.setText(R.string.error_search);
            partialEmptyBinding.textEmptySubtitle.setText(R.string.error_search_sub);
        };
    }

    public void destroyInstance() {
        handler.removeCallbacks(displayEmpty);
        handler.removeCallbacks(displayNoSearchResults);
        handler.removeCallbacks(displayNoFilterResults);
        partialEmptyBinding.linearEmpty.animate().cancel();
        displayEmpty = null;
        displayNoSearchResults = null;
        displayNoFilterResults = null;
        partialEmptyBinding = null;
    }
}
