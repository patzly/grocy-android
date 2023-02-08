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

package xyz.zedler.patrick.grocy.view;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import java.util.List;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.ViewOfflineIndicatorBinding;

public class OfflineIndicatorView extends LinearLayout {

  private final ViewOfflineIndicatorBinding binding;
  private SystemBarBehavior systemBarBehavior;

  public OfflineIndicatorView(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    binding = ViewOfflineIndicatorBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    );
  }

  public void setSystemBarBehavior(SystemBarBehavior systemBarBehavior) {
    this.systemBarBehavior = systemBarBehavior;
  }

  @Override
  public void setVisibility(int visibility) {
    ViewParent view = binding.linearOfflineError.getParent().getParent();
    LayoutTransition layoutTransition;
    if (view instanceof AppBarLayout) {
      layoutTransition = ((AppBarLayout) view).getLayoutTransition();
      // this is layout transition from the app bar linear layout
    } else {
      return;
    }
    if (layoutTransition == null) return;
    layoutTransition.addTransitionListener(new TransitionListener() {
      @Override
      public void startTransition(LayoutTransition transition, ViewGroup container, View view,
          int transitionType) {
      }
      @Override
      public void endTransition(LayoutTransition transition, ViewGroup container, View view,
          int transitionType) {
        if (systemBarBehavior != null) systemBarBehavior.refresh();
        List<TransitionListener> listeners = layoutTransition.getTransitionListeners();
        if (listeners.size() > 0) {
          layoutTransition.removeTransitionListener(listeners.get(listeners.size()-1));
        }
      }
    });
    super.setVisibility(visibility);
  }
}
