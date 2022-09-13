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
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;

public class AlertDialogUtil {

  public static void showWebViewDialog(Context context, String title, String html) {
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context, R.style.AlertDialogCustomWithNegativeOnly);
    alertBuilder.setTitle(title);

    WebView webView = new WebView(context);
    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setDomStorageEnabled(false);
    webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    webView.getSettings().setAllowFileAccess(false);
    webView.loadData(html, "text/html; charset=utf-8", "UTF-8");
    webView.setBackgroundColor(
        ResourcesCompat.getColor(context.getResources(), R.color.surface, null));
    webView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    FrameLayout linearLayout = (FrameLayout) inflater.inflate(R.layout.view_alert_dialog_scroll_content, null);
    LinearLayout webViewContainer = linearLayout.findViewById(R.id.webview_container);
    webViewContainer.addView(webView);
    MaterialButton buttonClose = linearLayout.findViewById(R.id.button_close);

    alertBuilder.setView(linearLayout);
    AlertDialog alert = alertBuilder.create();
    buttonClose.setOnClickListener(v -> alert.dismiss());
    alert.show();

    if (alert.getWindow() != null) {
      WindowManager.LayoutParams lp2 = new WindowManager.LayoutParams();
      lp2.copyFrom(alert.getWindow().getAttributes());
      lp2.width = WindowManager.LayoutParams.MATCH_PARENT;
      lp2.height = WindowManager.LayoutParams.WRAP_CONTENT;
      alert.getWindow().setAttributes(lp2);
    }
  }

  public static void showConfirmationDialog(
      Context context,
      String question,
      @Nullable HashMap<String, Boolean> multiChoiceItems,
      ConfirmationListener confirmationListener
  ) {
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);

    TextView title = new TextView(context);
    title.setTextAppearance(context, R.style.Widget_Grocy_TextView);
    int paddingTitle = UnitUtil.dpToPx(context, 8);
    title.setPadding(paddingTitle*3, paddingTitle*2, paddingTitle*3, 0);
    title.setText(question);
    title.setTextSize(UnitUtil.spToPx(context, 6));
    alertBuilder.setCustomTitle(title);

    if (multiChoiceItems != null) {
      CharSequence[] names = multiChoiceItems.keySet().toArray(new String[0]);
      Boolean[] namesChecked = multiChoiceItems.values().toArray(new Boolean[0]);
      alertBuilder.setMultiChoiceItems(
          names,
          toPrimitiveArray(namesChecked),
          (dialog, which, isChecked) -> multiChoiceItems.put((String) names[which], isChecked)
      );
    }

    alertBuilder.setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss());
    alertBuilder.setPositiveButton(R.string.action_proceed, (dialog, which) -> {
      confirmationListener.onConfirm(multiChoiceItems);
      dialog.dismiss();
    });
    alertBuilder.show();
  }

  public static void showConfirmationDialog(
      Context context,
      String question,
      Runnable confirmationListener
  ) {
    showConfirmationDialog(
        context, question,
        null,
        multiChoiceItems -> confirmationListener.run()
    );
  }

  public interface ConfirmationListener {
    void onConfirm(HashMap<String, Boolean> multiChoiceItems);
  }

  private static boolean[] toPrimitiveArray(Boolean... booleanList) {
    final boolean[] primitives = new boolean[booleanList.length];
    int index = 0;
    for (Boolean object : booleanList) {
      primitives[index++] = object;
    }
    return primitives;
  }

}
