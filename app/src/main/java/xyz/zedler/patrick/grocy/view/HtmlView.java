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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.ViewHtmlBinding;
import xyz.zedler.patrick.grocy.util.ResUtil;

public class HtmlView extends LinearLayout {

  private static final String TAG = HtmlView.class.getSimpleName();

  private ViewHtmlBinding binding;
  private Context context;

  public HtmlView(Context context) {
    super(context);
    init(context);
  }

  public HtmlView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context context) {
    this.context = context;
    binding = ViewHtmlBinding.inflate(
        LayoutInflater.from(context), this, true
    );
    setSaveEnabled(true);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    binding = null;
  }

  @SuppressLint("ClickableViewAccessibility")
  public void setHtml(String html) {
    if (html != null) {
      html = html.replaceAll("</?font[^>]*>", ""); // remove font
      html = html.replaceAll(
          "<p[^>]*> *(<br ?/>)*</p[^>]*>[\\n\\r\\s]*$", "" // trim empty paragraphs at end
      );
      html = html.replaceAll(
          "^[\\n\\r\\s]*<p[^>]*> *(<br ?/>)*</p[^>]*>", "" // trim empty paragraphs at start
      );
      binding.webview.getSettings().setJavaScriptEnabled(false);
      binding.webview.loadDataWithBaseURL(
          "file:///android_asset/",
          getFormattedHtml(html),
          "text/html; charset=utf-8", "utf8", null
      );
      binding.webview.setBackgroundColor(Color.TRANSPARENT);
    } else {
      setVisibility(View.GONE);
    }
  }

  private String getFormattedHtml(String html) {
    int textColor = ResUtil.getColorAttr(context, R.attr.colorOnSurface);
    int linkColor = ResUtil.getColorAttr(context, R.attr.colorPrimary);
    return  "<!DOCTYPE html><html><head><meta charset='UTF-8'><style type='text/css'>"
        + "@font-face{font-family: Jost; src: url('fonts/jost_400_book.otf')}"
        + "body{font-family: Jost;color:#" + String.format("%06X", (0xFFFFFF & textColor)) + ";}"
        + "a{color:#" + String.format("%06X", (0xFFFFFF & linkColor)) + ";}"
        + "</style></head><body>" + html + "</body></html>";
  }
}
