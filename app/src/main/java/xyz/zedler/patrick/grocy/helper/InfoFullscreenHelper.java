/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.helper;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.Nullable;
import java.util.Objects;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.view.InfoFullscreenView;

public class InfoFullscreenHelper {

  private ViewGroup viewGroup;
  private InfoFullscreenView infoFullscreenView;

  private static final int ANIMATION_DURATION = 300;
  private static final int INFO_FULLSCREEN_VIEW_ID = 39253513;

  public InfoFullscreenHelper(ViewGroup viewGroup) {
    this.viewGroup = viewGroup;
  }

  public void setInfo(InfoFullscreen infoFullscreen) {
    setInfo(infoFullscreen, false);
  }

  public void setInfo(InfoFullscreen infoFullscreen, boolean slide) {
    View oldView = viewGroup.findViewById(INFO_FULLSCREEN_VIEW_ID);
    if (oldView != null && infoFullscreen != null) {
      // if info texts and type are the same, skip update
      InfoFullscreenView oldFullscreenView = (InfoFullscreenView) oldView;
      if (oldFullscreenView.getType() == infoFullscreen.getType()
          && Objects.equals(oldFullscreenView.getExact(), infoFullscreen.getExact())
      ) {
        return;
      }
    }

    Animation animation;
    if (infoFullscreen == null) {
      animation = AnimationUtils.loadAnimation(viewGroup.getContext(), R.anim.slide_out_up);
      animation.setDuration(ANIMATION_DURATION);
      this.infoFullscreenView = null;
      clearState(null, slide ? animation : null);
      return;
    } else {
      animation = AnimationUtils.loadAnimation(viewGroup.getContext(), R.anim.slide_in_down);
      animation.setDuration(ANIMATION_DURATION);
    }
    this.infoFullscreenView = new InfoFullscreenView(
        viewGroup.getContext(),
        infoFullscreen.getType(),
        infoFullscreen.getExact(),
        infoFullscreen.getClickListener()
    );
    this.infoFullscreenView.setId(INFO_FULLSCREEN_VIEW_ID);
    startAnimation(slide ? animation : null);
  }

  private void clearState(
      @Nullable OnClearedListener onCleared,
      @Nullable Animation specificAnim
  ) {
    View view = viewGroup.findViewById(INFO_FULLSCREEN_VIEW_ID);
    if (view == null) {
      if (onCleared != null) {
        onCleared.cleared(true);
      }
      return;
    }
    if (specificAnim != null) {
      specificAnim.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
          if (viewGroup == null) {
            return;
          }
          viewGroup.removeView(view);
          if (onCleared != null) {
            onCleared.cleared(false);
          }
        }
      });
      view.startAnimation(specificAnim);
    } else {
      view.animate().alpha(0).setDuration(ANIMATION_DURATION / 2)
          .withEndAction(() -> {
            if (viewGroup == null) {
              return;
            }
            viewGroup.removeView(view);
            if (onCleared != null) {
              onCleared.cleared(false);
            }
          }).start();
    }
  }

  private void startAnimation(@Nullable Animation additionalAnim) {
    clearState(skipped -> {
      infoFullscreenView.setAlpha(0);
      if (infoFullscreenView.isInForeground()) {
        viewGroup.addView(infoFullscreenView);
      } else {
        viewGroup.addView(infoFullscreenView, 0);
      }
      int duration;
      if (additionalAnim != null && skipped) {
        infoFullscreenView.startAnimation(additionalAnim);
        duration = ANIMATION_DURATION / 2;
      } else {
        duration = ANIMATION_DURATION / 2;
      }
      infoFullscreenView.animate().alpha(1).setDuration(duration).start();
    }, null);
  }

  public void destroyInstance() {
    if (infoFullscreenView != null) {
      infoFullscreenView.animate().cancel();
      infoFullscreenView = null;
    }
    viewGroup = null;
  }

  private interface OnClearedListener {

    void cleared(boolean skipped);
  }
}
