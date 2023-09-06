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

package xyz.zedler.patrick.grocy.activity;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import xyz.zedler.patrick.grocy.R;

public class WebDialogActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_dialog);

    FrameLayout container = findViewById(R.id.container);
    WebView webView = findViewById(R.id.webview);
    CardView cardView = findViewById(R.id.card_view);
    TextView textView = findViewById(R.id.description);

    container.setOnClickListener(v -> finish());

    int backgroundColor = getIntent().getIntExtra("colorBg", -1);
    if (backgroundColor != -1) {
      cardView.setCardBackgroundColor(backgroundColor);
      webView.setBackgroundColor(backgroundColor);
    }
    int textColor = getIntent().getIntExtra("colorText", -1);
    if (textColor != -1) {
      textView.setTextColor(textColor);
    }

    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setDomStorageEnabled(false);
    webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    webView.getSettings().setAllowFileAccess(false);

    String html = getIntent().getStringExtra("html");
    webView.loadDataWithBaseURL(
        "file:///android_asset/", html,
        "text/html; charset=utf-8", "utf8", null
    );
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
  }
}
