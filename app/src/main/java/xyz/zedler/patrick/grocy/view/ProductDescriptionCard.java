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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDivider;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class ProductDescriptionCard extends LinearLayout {

  private WebView webView;
  private MaterialDivider divider;
  private TextView textViewHelp;
  private MaterialCardView card;
  private String finalDescription;

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
    divider = findViewById(R.id.divider);
    textViewHelp = findViewById(R.id.text_help);
    card = findViewById(R.id.card);
  }

  @SuppressLint("ClickableViewAccessibility")
  public void setDescriptionHtml(String description) {
    if (description != null) {
      description = description.replaceAll("<[/]?font[^>]*>", ""); // remove font
      description = description.replaceAll(
          "<p[^>]*> *(<br ?/>)*</p[^>]*>[\\n\\r\\s]*$", "" // trim empty paragraphs at end
      );
      description = description.replaceAll(
          "^[\\n\\r\\s]*<p[^>]*> *(<br ?/>)*</p[^>]*>", "" // trim empty paragraphs at start
      );
      finalDescription = description;
      webView.getSettings().setJavaScriptEnabled(false);
      webView.loadDataWithBaseURL(
          "file:///android_asset/",
          getColoredHtml(description, true),
          "text/html; charset=utf-8", "utf8", null
      );

      webView.setBackgroundColor(Color.TRANSPARENT);
      webView.setOnTouchListener((v, event) -> card.onTouchEvent(event));
      webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ViewTreeObserver observer = webView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                webView.measure(
                    MeasureSpec.makeMeasureSpec(card.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                );
                int height = webView.getMeasuredHeight();
                int maxHeight = UiUtil.dpToPx(getContext(), 80);
                int padding = UiUtil.dpToPx(getContext(), 8);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Math.min(height, maxHeight)
                );
                params.setMargins(padding, padding, padding, padding);
                webView.setLayoutParams(params);
                if (height > maxHeight) {
                  divider.setVisibility(VISIBLE);
                  textViewHelp.setVisibility(VISIBLE);
                }
                if (webView.getViewTreeObserver().isAlive()) {
                  webView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
              }
            });
          }, 0);
        }
      });
      card.setOnClickListener(v -> showDescriptionDialog());
    } else {
      setVisibility(View.GONE);
    }
  }

  public void showDescriptionDialog() {
    FrameLayout frameLayout = new FrameLayout(getContext());
    frameLayout.setLayoutParams(
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    );
    WebView webView = new WebView(getContext());
    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setDomStorageEnabled(false);
    webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    webView.getSettings().setAllowFileAccess(false);
    webView.loadDataWithBaseURL(
        "file:///android_asset/", getColoredHtml(finalDescription, false),
        "text/html; charset=utf-8", "utf8", null
    );
    webView.setBackgroundColor(Color.TRANSPARENT);
    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
    );
    layoutParams.setMargins(
        UiUtil.dpToPx(getContext(), 17),
        UiUtil.dpToPx(getContext(), 10),
        UiUtil.dpToPx(getContext(), 17),
        0
    );
    webView.setLayoutParams(layoutParams);
    frameLayout.addView(webView);

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
    builder.setTitle(R.string.property_description)
        .setView(frameLayout)
        .setPositiveButton(R.string.action_close, (dialog, which) -> {})
        .create();
    builder.show();
  }

  private String getColoredHtml(String html, boolean useOnSurfaceVariant) {
    int textColor = ResUtil.getColorAttr(
        getContext(), useOnSurfaceVariant ? R.attr.colorOnSurfaceVariant : R.attr.colorOnSurface
    );
    int linkColor = ResUtil.getColorAttr(getContext(), R.attr.colorPrimary);
    return  "<!DOCTYPE html><html><head><title>Description</title><meta charset=\"UTF-8\">"
        + "<style type='text/css'>"
        + "@font-face {\n"
        + "    font-family: Jost;\n"
        + "    src: url(\"file:///android_asset/fonts/jost_400_book.otf\")\n"
        + "}"
        + "body{font-family: Jost;color:#" + String.format("%06X", (0xFFFFFF & textColor)) + ";}"
        + "a{color:#" + String.format("%06X", (0xFFFFFF & linkColor)) + ";}"
        + "</style></head>"
        + "<body>" + html + "</body></html>";
  }
}
