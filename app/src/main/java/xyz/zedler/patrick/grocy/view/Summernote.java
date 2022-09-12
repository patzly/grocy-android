package xyz.zedler.patrick.grocy.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by Avinash on 01-04-2016.
 */
public class Summernote extends WebView  {
    String text="";
    private int REQUEST_FILE_PICKER = 1;
    private ValueCallback<Uri> mFilePathCallback4;
    private ValueCallback<Uri[]> mFilePathCallback5;
    Context context;
    public Summernote(Context context) {
        super(context);
        this.context =context;
        enable_summernote();
    }
    public Summernote(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context =context;
        enable_summernote();
    }
    public Summernote(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context =context;
        enable_summernote();
    }
    public void enable_summernote(){
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //this.getSettings().setBuiltInZoomControls(true);
        this.addJavascriptInterface(new MyJavaScriptInterface(), "android");
        this. getSettings().setLoadWithOverviewMode(true);
        this. getSettings().setUseWideViewPort(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.loadUrl("file:///android_asset/summernote.html");
        setWebChromeClient(new WebChromeClient() {
            public void openFileChooser(ValueCallback<Uri> filePathCallback) {
                mFilePathCallback4 = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                ((Activity)context).startActivityForResult(Intent.createChooser(intent, "File Chooser"), REQUEST_FILE_PICKER);
            }

            public void openFileChooser(ValueCallback filePathCallback, String acceptType) {
                mFilePathCallback4 = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                ((Activity)context).startActivityForResult(Intent.createChooser(intent, "File Chooser"), REQUEST_FILE_PICKER);
            }

            public void openFileChooser(ValueCallback<Uri> filePathCallback, String acceptType, String capture) {
                mFilePathCallback4 = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                ((Activity)context).startActivityForResult(Intent.createChooser(intent, "File Chooser"), REQUEST_FILE_PICKER);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                mFilePathCallback5 = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                ((Activity)context).startActivityForResult(Intent.createChooser(intent, "File Chooser"), REQUEST_FILE_PICKER);
                return true;
            }
        });
    }
    class MyJavaScriptInterface {
        @JavascriptInterface
        public void getText(String html) {
            text=html;
        }
    }
    public void setText(final String html) {
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                setText(html);
            }
        });
        this.loadUrl("javascript:$('#summernote').summernote('reset');");
        this.loadUrl("javascript:$('#summernote').summernote('code', '" + html.replace("'","\\'") + "');");
    }
    public String getText() {
        text = "P/%TE5XpkAijBc%LjA;_-pZcbiU25E6feX5y/n6qxCTmhprLrqC3H%^hU!%q2,k'm`SHheoW^'mQ~zW93,C?~GtYk!wi/&'3KxW8";
        this.loadUrl("javascript:window.android.getText" + "(document.getElementsByClassName('note-editable')[0].innerHTML);");
        int i=0;
        try{
            while(text.equals("P/%TE5XpkAijBc%LjA;_-pZcbiU25E6feX5y/n6qxCTmhprLrqC3H%^hU!%q2,k'm`SHheoW^'mQ~zW93,C?~GtYk!wi/&'3KxW8")&&i<100){
                Thread.sleep(200);
                i++;
            }
        }catch (Exception e ){text = "Unable get the Text";}
        return text;
    }
    public void setRequestCodeforFilepicker(int i){
        REQUEST_FILE_PICKER=i;
    }
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode==REQUEST_FILE_PICKER)
        {
            if(mFilePathCallback4!=null)
            {
                Uri result = intent==null || resultCode!=Activity.RESULT_OK ? null : intent.getData();
                if(result!=null)
                {
                    Uri path = intent.getData();
                    mFilePathCallback4.onReceiveValue(path);
                }
                else
                {
                    mFilePathCallback4.onReceiveValue(null);
                }
            }
            if(mFilePathCallback5!=null)
            {
                Uri result = intent==null || resultCode!=Activity.RESULT_OK ? null : intent.getData();
                if(result!=null)
                {
                    Uri path = intent.getData();
                    mFilePathCallback5.onReceiveValue(new Uri[]{path});
                }
                else
                {
                    mFilePathCallback5.onReceiveValue(null);
                }
            }

            mFilePathCallback4 = null;
            mFilePathCallback5 = null;
        }
    }
}
