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
import android.widget.RelativeLayout;

import xyz.zedler.patrick.grocy.databinding.PartialErrorBinding;
import xyz.zedler.patrick.grocy.model.ErrorFullscreen;

public class ErrorFullscreenHelper {

    private PartialErrorBinding partialErrorBinding;
    private Handler handler;
    private final Runnable displayError;
    private ErrorFullscreen errorFullscreen;

    public ErrorFullscreenHelper(PartialErrorBinding partialErrorBinding) {
        this.partialErrorBinding = partialErrorBinding;
        this.handler = new Handler();
        this.displayError = displayError();
    }

    public void setError(ErrorFullscreen errorFullscreen) {
        if(errorFullscreen == null) {
            clearState();
            return;
        }
        this.errorFullscreen = errorFullscreen;
        handler.postDelayed(displayError, 125);
        startAnimation();
    }

    public void clearState() {
        // hide container
        partialErrorBinding.relativeError.animate().alpha(0).setDuration(125).withEndAction(
                () -> partialErrorBinding.relativeError.setVisibility(View.GONE)
        ).start();
        errorFullscreen = null;
    }

    private void startAnimation() {
        RelativeLayout container = partialErrorBinding.relativeError;
        if(container.getVisibility() == View.VISIBLE) {
            // first hide previous error state if needed
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
            if(errorFullscreen.getErrorPicture() != -1) {
                partialErrorBinding.imageError.setImageResource(errorFullscreen.getErrorPicture());
                partialErrorBinding.imageError.setVisibility(View.VISIBLE);
            } else {
                partialErrorBinding.imageError.setVisibility(View.GONE);
            }
            if(errorFullscreen.getErrorTitle() != -1) {
                partialErrorBinding.textErrorTitle.setText(errorFullscreen.getErrorTitle());
                partialErrorBinding.textErrorTitle.setVisibility(View.VISIBLE);
            } else {
                partialErrorBinding.textErrorTitle.setVisibility(View.GONE);
            }
            if(errorFullscreen.getErrorSubtitle() != -1) {
                partialErrorBinding.textErrorSubtitle.setText(errorFullscreen.getErrorSubtitle());
                partialErrorBinding.textErrorSubtitle.setVisibility(View.VISIBLE);
            } else {
                partialErrorBinding.textErrorSubtitle.setVisibility(View.GONE);
            }
            if(errorFullscreen.getErrorExact() != null) {
                partialErrorBinding.textErrorExact.setText(errorFullscreen.getErrorExact());
                partialErrorBinding.textErrorExact.setVisibility(View.VISIBLE);
            } else {
                partialErrorBinding.textErrorExact.setVisibility(View.GONE);
            }
        };
    }

    public void destroyInstance() {
        handler.removeCallbacks(displayError);
        partialErrorBinding.relativeError.animate().cancel();
        partialErrorBinding = null;
    }
}
