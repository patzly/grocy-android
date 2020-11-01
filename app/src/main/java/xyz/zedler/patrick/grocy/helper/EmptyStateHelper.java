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
import android.widget.LinearLayout;

import androidx.annotation.StringRes;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.PartialEmptyBinding;

public class EmptyStateHelper {

    private PartialEmptyBinding partialEmptyBinding;
    private final Handler handler;
    private Runnable displayEmpty;
    private Runnable displayNoSearchResults;
    private Runnable displayNoFilterResults;
    @StringRes private final int emptyTitle;
    @StringRes private final int emptySubtitle;

    public EmptyStateHelper(
            PartialEmptyBinding partialEmptyBinding,
            @StringRes int emptyTitle,
            @StringRes int emptySubtitle
    ) {
        this.partialEmptyBinding = partialEmptyBinding;
        this.handler = new android.os.Handler();
        this.displayEmpty = displayEmpty();
        this.displayNoSearchResults = displayNoSearchResults();
        this.displayNoFilterResults = displayNoFilterResults();
        this.emptyTitle = emptyTitle;
        this.emptySubtitle = emptySubtitle;
    }

    public void clearState() {
        if(partialEmptyBinding == null) return;
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
            partialEmptyBinding.textEmptyTitle.setText(emptyTitle);
            partialEmptyBinding.textEmptySubtitle.setText(emptySubtitle);
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
            partialEmptyBinding.imageEmpty.setImageResource(R.drawable.illustration_coffee);
            partialEmptyBinding.textEmptyTitle.setText(R.string.error_filter);
            partialEmptyBinding.textEmptySubtitle.setText(R.string.error_filter_sub);
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
