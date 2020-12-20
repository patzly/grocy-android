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

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.view.InfoFullscreenView;

public class InfoFullscreenHelper {

    private ViewGroup viewGroup;
    private InfoFullscreenView infoFullscreenView;

    private static final int ANIMATION_DURATION = 125;
    private static final int INFO_FULLSCREEN_VIEW_ID = 39253513;

    public InfoFullscreenHelper(ViewGroup viewGroup) {
        this.viewGroup = viewGroup;
    }

    public void setInfo(InfoFullscreen infoFullscreen) {
        if(infoFullscreen == null) {
            this.infoFullscreenView = null;
            clearState(null);
            return;
        }
        this.infoFullscreenView = new InfoFullscreenView(
                viewGroup.getContext(),
                infoFullscreen.getType(),
                infoFullscreen.getExact(),
                infoFullscreen.getClickListener()
        );
        this.infoFullscreenView.setId(INFO_FULLSCREEN_VIEW_ID);
        startAnimation();
    }

    private void clearState(@Nullable Runnable runnable) {
        View view = viewGroup.findViewById(INFO_FULLSCREEN_VIEW_ID);
        if(view == null) {
            if(runnable != null) runnable.run();
            return;
        }
        view.animate().alpha(0).setDuration(ANIMATION_DURATION)
                .withEndAction(() -> {
                    if(viewGroup == null) return;
                    viewGroup.removeView(view);
                    if(runnable != null) runnable.run();
                }).start();
    }

    private void startAnimation() {
        clearState(() -> {
            infoFullscreenView.setAlpha(0);
            if(infoFullscreenView.isInForeground()) {
                viewGroup.addView(infoFullscreenView);
            } else {
                viewGroup.addView(infoFullscreenView, 0);
            }
            infoFullscreenView.animate().alpha(1).setDuration(ANIMATION_DURATION).start();
        });
    }

    public void destroyInstance() {
        if(infoFullscreenView != null) {
            infoFullscreenView.animate().cancel();
            infoFullscreenView = null;
        }
        viewGroup = null;
    }
}
