package xyz.zedler.patrick.grocy.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebSettingsCompat;
import xyz.zedler.patrick.grocy.util.UnitUtil;

/**
 * Created by Avinash on 01-04-2016.
 */
public class Summernote extends WebView  {
    String text = "";
    private final int REQUEST_FILE_PICKER = 9;
    private ValueCallback<Uri> mFilePathCallback4;
    private ValueCallback<Uri[]> mFilePathCallback5;
    Context context;

    public Summernote(Context context) {
        super(context);
        this.context = context;
        enable_summernote();
    }

    public Summernote(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context = context;
        enable_summernote();
    }

    public Summernote(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        enable_summernote();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void enable_summernote(){
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        this.addJavascriptInterface(new MyJavaScriptInterface(), "android");
        this.getSettings().setLoadWithOverviewMode(true);
        this.getSettings().setUseWideViewPort(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.loadUrl("file:///android_asset/summernote/summernote.html");

        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                mFilePathCallback5 = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                ((Activity) context).startActivityForResult(Intent.createChooser(intent, "File Chooser"), REQUEST_FILE_PICKER);
                return true;
            }
        });
    }

    class MyJavaScriptInterface {
        @JavascriptInterface
        public void getText(String html) {
            text = html;
        }
    }

    public void setText(final String html) {
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                setText(html);
            }
        });
        this.loadUrl("javascript:(function f() {"
            + "document.getElementsByClassName('note-toolbar')[0].style.padding = '11px 16px 16px 16px';"
            + "})()");
        this.loadUrl("javascript:(function f() {"
            + "document.getElementsByClassName('note-editable')[0].style.padding = '16px 16px 120px 16px';"
            + "})()");

        int nightModeFlags = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            this.loadUrl("javascript:(function f() {"
                + "changeCSS('bootstrap-slate.min.css');"
                + "})()");
            this.loadUrl("javascript:(function f() {"
                + "document.getElementsByClassName('note-editable')[0].style.backgroundColor = 'rgb(40, 40, 40)';"
                + "document.getElementsByClassName('note-editable')[0].style.color = 'white';"
                + "})()");
        }

        this.loadUrl("javascript:$('#summernote').summernote('reset');");
        this.loadUrl("javascript:$('#summernote').summernote('code', '" + html.replace("'","\\'") + "');");

    }

    public String getText() {
        text = "P/%TE5XpkAijBc%LjA;_-pZcbiU25E6feX5y/n6qxCTmhprLrqC3H%^hU!%q2,k'm`SHheoW^'mQ~zW93,C?~GtYk!wi/&'3KxW8";
        this.loadUrl("javascript:window.android.getText" + "(document.getElementsByClassName('note-editable')[0].innerHTML);");
        try{
            for (int i=0; i<100; i++) {
                if (!text.equals("P/%TE5XpkAijBc%LjA;_-pZcbiU25E6feX5y/n6qxCTmhprLrqC3H%^hU!%q2,k'm`SHheoW^'mQ~zW93,C?~GtYk!wi/&'3KxW8")) {
                    break;
                }
                Thread.sleep(200);
            }
        } catch (Exception e) {
            text = "Unable to get Text";
        }
        return text;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == REQUEST_FILE_PICKER) {
            if(mFilePathCallback4 != null) {
                Uri result = intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();
                if (result != null) {
                    Uri path = intent.getData();
                    mFilePathCallback4.onReceiveValue(path);
                } else {
                    mFilePathCallback4.onReceiveValue(null);
                }
            }
            if(mFilePathCallback5 != null) {
                Uri result = intent == null || resultCode != Activity.RESULT_OK ? null : intent.getData();
                if(result != null) {
                    Uri path = intent.getData();
                    mFilePathCallback5.onReceiveValue(new Uri[]{path});
                } else {
                    mFilePathCallback5.onReceiveValue(null);
                }
            }

            mFilePathCallback4 = null;
            mFilePathCallback5 = null;
        }
    }
}
