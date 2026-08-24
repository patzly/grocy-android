/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import xyz.zedler.patrick.grocy.web.ReverseProxyAuthDetector;

/**
 * Hosts an interactive reverse-proxy login and returns after the provider redirects back to Grocy.
 */
public class ReverseProxyAuthActivity extends AppCompatActivity {

  public static final String EXTRA_TARGET_URL = "target_url";

  private WebView webView;
  private Uri targetUri;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String targetUrl = getIntent().getStringExtra(EXTRA_TARGET_URL);
    targetUri = targetUrl == null ? null : Uri.parse(targetUrl);
    if (targetUri == null || targetUri.getHost() == null || !isHttp(targetUri)) {
      setResult(Activity.RESULT_CANCELED);
      finish();
      return;
    }

    FrameLayout container = new FrameLayout(this);
    webView = new WebView(this);
    ProgressBar progress = new ProgressBar(this);

    container.addView(webView, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    ));
    FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    );
    progressParams.gravity = android.view.Gravity.CENTER;
    container.addView(progress, progressParams);
    setContentView(container);

    CookieManager cookies = CookieManager.getInstance();
    cookies.setAcceptCookie(true);
    cookies.setAcceptThirdPartyCookies(webView, true);

    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      settings.setSafeBrowsingEnabled(true);
    }

    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        progress.setVisibility(android.view.View.GONE);
        Uri current = Uri.parse(url);
        if (ReverseProxyAuthDetector.isSameOrigin(targetUri.toString(), current.toString())
            && !ReverseProxyAuthDetector.isAuthenticationPath(current.getPath())) {
          CookieManager.getInstance().flush();
          setResult(Activity.RESULT_OK);
          finish();
        }
      }
    });
    webView.loadUrl(targetUri.toString());
  }

  @Override
  protected void onDestroy() {
    if (webView != null) {
      webView.stopLoading();
      webView.loadUrl("about:blank");
      webView.clearHistory();
      webView.removeAllViews();
      webView.destroy();
      webView = null;
    }
    super.onDestroy();
  }

  private static boolean isHttp(Uri uri) {
    return "https".equalsIgnoreCase(uri.getScheme())
        || "http".equalsIgnoreCase(uri.getScheme());
  }
}
