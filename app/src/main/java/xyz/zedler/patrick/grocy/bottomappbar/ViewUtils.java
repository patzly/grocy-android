/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zedler.patrick.grocy.bottomappbar;

import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.R;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Utils class for custom views.
 */
@RestrictTo(LIBRARY_GROUP)
public class ViewUtils {

  private ViewUtils() {}

  public static boolean isLayoutRtl(View view) {
    return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
  }

  /**
   * Wrapper around {@link androidx.core.view.OnApplyWindowInsetsListener} which also passes
   * the initial padding set on the view. Used with {@link #doOnApplyWindowInsets(View,
   * OnApplyWindowInsetsListener)}.
   */
  public interface OnApplyWindowInsetsListener {

    /**
     * When {@link View#setOnApplyWindowInsetsListener(View.OnApplyWindowInsetsListener) set} on a
     * View, this listener method will be called instead of the view's own {@link
     * View#onApplyWindowInsets(WindowInsets)} method. The {@code initialPadding} is the view's
     * original padding which can be updated and will be applied to the view automatically. This
     * method should return a new {@link WindowInsetsCompat} with any insets consumed.
     */
    WindowInsetsCompat onApplyWindowInsets(
        View view, WindowInsetsCompat insets, RelativePadding initialPadding);
  }

  /** Simple data object to store the initial padding for a view. */
  public static class RelativePadding {
    public int start;
    public int top;
    public int end;
    public int bottom;

    public RelativePadding(int start, int top, int end, int bottom) {
      this.start = start;
      this.top = top;
      this.end = end;
      this.bottom = bottom;
    }

    public RelativePadding(@NonNull RelativePadding other) {
      this.start = other.start;
      this.top = other.top;
      this.end = other.end;
      this.bottom = other.bottom;
    }

    /** Applies this relative padding to the view. */
    public void applyToView(View view) {
      ViewCompat.setPaddingRelative(view, start, top, end, bottom);
    }
  }

  /**
   * Wrapper around {@link androidx.core.view.OnApplyWindowInsetsListener} that can
   * automatically apply inset padding based on view attributes.
   */
  public static void doOnApplyWindowInsets(
      @NonNull View view,
      @Nullable AttributeSet attrs,
      int defStyleAttr,
      int defStyleRes,
      @Nullable final OnApplyWindowInsetsListener listener) {
    TypedArray a =
        view.getContext()
            .obtainStyledAttributes(attrs, R.styleable.Insets, defStyleAttr, defStyleRes);

    final boolean paddingBottomSystemWindowInsets =
        a.getBoolean(R.styleable.Insets_paddingBottomSystemWindowInsets, false);
    final boolean paddingLeftSystemWindowInsets =
        a.getBoolean(R.styleable.Insets_paddingLeftSystemWindowInsets, false);
    final boolean paddingRightSystemWindowInsets =
        a.getBoolean(R.styleable.Insets_paddingRightSystemWindowInsets, false);

    a.recycle();

    doOnApplyWindowInsets(
        view,
            (view1, insets, initialPadding) -> {
              if (paddingBottomSystemWindowInsets) {
                initialPadding.bottom += insets.getSystemWindowInsetBottom();
              }
              boolean isRtl = isLayoutRtl(view1);
              if (paddingLeftSystemWindowInsets) {
                if (isRtl) {
                  initialPadding.end += insets.getSystemWindowInsetLeft();
                } else {
                  initialPadding.start += insets.getSystemWindowInsetLeft();
                }
              }
              if (paddingRightSystemWindowInsets) {
                if (isRtl) {
                  initialPadding.start += insets.getSystemWindowInsetRight();
                } else {
                  initialPadding.end += insets.getSystemWindowInsetRight();
                }
              }
              initialPadding.applyToView(view1);
              return listener != null
                  ? listener.onApplyWindowInsets(view1, insets, initialPadding)
                  : insets;
            });
  }

  /**
   * Wrapper around {@link androidx.core.view.OnApplyWindowInsetsListener} that records the
   * initial padding of the view and requests that insets are applied when attached.
   */
  public static void doOnApplyWindowInsets(
      @NonNull View view, @NonNull final OnApplyWindowInsetsListener listener) {
    // Create a snapshot of the view's padding state.
    final RelativePadding initialPadding =
        new RelativePadding(
            ViewCompat.getPaddingStart(view),
            view.getPaddingTop(),
            ViewCompat.getPaddingEnd(view),
            view.getPaddingBottom());
    // Set an actual OnApplyWindowInsetsListener which proxies to the given callback, also passing
    // in the original padding state.
    ViewCompat.setOnApplyWindowInsetsListener(
            view,
            (view1, insets) -> listener.onApplyWindowInsets(
                    view1, insets, new RelativePadding(initialPadding)
            )
    );
    // Request some insets.
    requestApplyInsetsWhenAttached(view);
  }

  /** Requests that insets should be applied to this view once it is attached. */
  public static void requestApplyInsetsWhenAttached(@NonNull View view) {
    if (ViewCompat.isAttachedToWindow(view)) {
      // We're already attached, just request as normal.
      ViewCompat.requestApplyInsets(view);
    } else {
      // We're not attached to the hierarchy, add a listener to request when we are.
      view.addOnAttachStateChangeListener(
          new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
              v.removeOnAttachStateChangeListener(this);
              ViewCompat.requestApplyInsets(v);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {}
          });
    }
  }

  /** Returns the content view that is the parent of the provided view. */
  @Nullable
  public static ViewGroup getContentView(@Nullable View view) {
    if (view == null) {
      return null;
    }

    View rootView = view.getRootView();
    ViewGroup contentView = rootView.findViewById(android.R.id.content);
    if (contentView != null) {
      return contentView;
    }

    // Account for edge cases: Parent's parent can be null without ever having found
    // android.R.id.content (e.g. if view is in an overlay during a transition).
    // Additionally, sometimes parent's parent is neither a ViewGroup nor a View (e.g. if view
    // is in a PopupWindow).
    if (rootView != view && rootView instanceof ViewGroup) {
      return (ViewGroup) rootView;
    }

    return null;
  }
}
