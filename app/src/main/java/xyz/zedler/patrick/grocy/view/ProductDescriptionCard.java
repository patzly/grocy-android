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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.card.MaterialCardView;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.AlertDialogUtil;

public class ProductDescriptionCard extends LinearLayout {

  private WebView webView;
  private MaterialCardView card;

  public ProductDescriptionCard(Context context) {
    super(context);

    init();
  }

  public ProductDescriptionCard(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    init();
  }

  private void init() {
    inflate(getContext(), R.layout.view_product_description_card, this);

    webView = findViewById(R.id.webview);
    card = findViewById(R.id.card);
  }

  public void setDescriptionHtml(String description) {
    if (description != null) {
      description = "<font color='" + String.format(
          "%06x",
          ContextCompat.getColor(getContext(), R.color.on_background) & 0xffffff
      ) + "'>" + description + "</font>";
      webView.getSettings().setJavaScriptEnabled(false);
      webView.loadData(description, "text/html; charset=utf-8", "UTF-8");
      webView.setBackgroundColor(
          ResourcesCompat.getColor(getResources(), R.color.on_background_tertiary, null));
      String finalDescription = description;
      card.setOnClickListener(
          v -> AlertDialogUtil.showWebViewDialog(
              getContext(),
              getContext().getString(R.string.property_description),
              finalDescription
          )
      );

    } else {
      card.setVisibility(View.GONE);
    }
  }
}
