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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.R;

public class AlertDialogUtil {

  public static void showProductDescriptionDialog(Context context, String description) {
    MaterialAlertDialogBuilder alertBuilder = new MaterialAlertDialogBuilder(context, R.style.AlertDialogCustom);
    alertBuilder.setTitle(R.string.property_description);

    RelativeLayout relativeLayout = new RelativeLayout(context);
    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    int margin = UnitUtil.dpToPx(context, 16);
    relativeLayout.setPadding(margin, margin, margin, 0);
    relativeLayout.setLayoutParams(lp);

    WebView webView = new WebView(context);
    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setDomStorageEnabled(false);
    webView.getSettings().setAppCacheEnabled(false);
    webView.getSettings().setAllowFileAccess(false);

    webView.loadData(description, "text/html; charset=utf-8", "UTF-8");
    webView.setBackgroundColor(
        ResourcesCompat.getColor(context.getResources(), R.color.on_background_tertiary, null));
    FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    webView.setLayoutParams(wlp);

    relativeLayout.addView(webView);

    int linearId = View.generateViewId();
    relativeLayout.setId(linearId);
    alertBuilder.setView(relativeLayout);
    alertBuilder.setNegativeButton(R.string.action_close, (dialog, which) -> dialog.dismiss());
    AlertDialog alert = alertBuilder.create();
    alert.show();

    if (alert.getWindow() != null) {
      WindowManager.LayoutParams lp2 = new WindowManager.LayoutParams();
      lp2.copyFrom(alert.getWindow().getAttributes());
      lp2.width = WindowManager.LayoutParams.MATCH_PARENT;
      lp2.height = WindowManager.LayoutParams.MATCH_PARENT;
      alert.getWindow().setAttributes(lp2);
    }
  }

}
