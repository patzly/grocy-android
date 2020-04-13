package xyz.zedler.patrick.grocy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class LoginActivity extends AppCompatActivity {

    final static String TAG = "LoginActivity";
    private final static boolean DEBUG = false;

    private SharedPreferences sharedPrefs;
    private long lastClick = 0;
    private RequestQueue requestQueue;
    private WebRequest request;

    private TextInputLayout textInputLayoutKey;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_login);

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        // INITIALIZE VIEWS

        TextInputLayout textInputLayoutServer = findViewById(R.id.text_input_login_server);
        EditText editTextServer = textInputLayoutServer.getEditText();
        assert editTextServer != null;
        editTextServer.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(R.id.image_login_logo);
        });

        textInputLayoutKey = findViewById(R.id.text_input_login_key);
        EditText editTextKey = textInputLayoutKey.getEditText();
        assert editTextKey != null;
        editTextKey.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(R.id.image_login_logo);
        });

        findViewById(R.id.button_login_key).setOnClickListener(v -> {
            if(editTextServer.getText().toString().equals("")) {
                textInputLayoutServer.setError(getString(R.string.msg_error_empty));
            } else if(!URLUtil.isValidUrl(editTextServer.getText().toString())) {
                textInputLayoutServer.setError(getString(R.string.msg_error_invalid_url));
            } else {
                textInputLayoutServer.setErrorEnabled(false);
                Intent browserManageKeys = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(editTextServer.getText().toString() + "/manageapikeys");
                browserManageKeys.setData(uri);
                startActivity(browserManageKeys);
            }
        });

        findViewById(R.id.button_login_login).setOnClickListener(v -> {
            // remove old errors
            textInputLayoutServer.setErrorEnabled(false);
            textInputLayoutKey.setErrorEnabled(false);

            String server = editTextServer.getText().toString();
            String key = editTextKey.getText().toString();
            if(server.equals("")) {
                textInputLayoutServer.setError(getString(R.string.msg_error_empty));
            } else if(!Patterns.WEB_URL.matcher(server).matches()) {
                textInputLayoutServer.setError(getString(R.string.msg_error_invalid_url));
            } else if(key.length() > 0 && key.length() != 50) {
                textInputLayoutKey.setError("API key too short"); // TODO: XML String
            } else {
                requestLogin(server, key);
            }
        });

        findViewById(R.id.button_login_demo).setOnClickListener(v -> {
            sharedPrefs.edit()
                    .putString(Constants.PREF.SERVER_URL, getString(R.string.url_grocy_demo))
                    .putString(Constants.PREF.API_KEY, "")
                    .apply();
            setResult(Constants.RESULT.SUCCESS);
            finish();
        });
    }

    private void requestLogin(String server, String key) {
        request.get(
                server + "/api/system/info?GROCY-API-KEY=" + key,
                response -> {
                    sharedPrefs.edit()
                            .putString(Constants.PREF.SERVER_URL, server)
                            .putString(Constants.PREF.API_KEY, key)
                            .apply();
                    setResult(Constants.RESULT.SUCCESS);
                    finish();
                },
                error -> {
                    if(error instanceof AuthFailureError) {
                        textInputLayoutKey.setError("API key not working"); // TODO: XML String
                    } else {
                        showSnackbar("That didn't work!" + error); // TODO: XML String
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void showSnackbar(String text) {
        Snackbar.make(
                findViewById(R.id.linear_login_container),
                text,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        try {
            ((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }
}
