package xyz.zedler.patrick.grocy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.net.URI;
import java.util.Objects;

import xyz.zedler.patrick.grocy.fragment.DrawerBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class LoginActivity extends AppCompatActivity {

    final static String TAG = "LoginActivity";
    private final static boolean DEBUG = false;

    private SharedPreferences sharedPrefs;
    private long lastClick = 0;
    private RequestQueue queue;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(this);

        TextInputLayout textInputLayoutServer = findViewById(R.id.text_input_login_server);
        EditText editTextServer = textInputLayoutServer.getEditText();
        assert editTextServer != null;
        editTextServer.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(R.id.image_login_logo);
        });

        TextInputLayout textInputLayoutKey = findViewById(R.id.text_input_login_key);
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
            if(editTextServer.getText().toString().equals("")) {
                textInputLayoutServer.setError(getString(R.string.msg_error_empty));
            } else if(!URLUtil.isValidUrl(editTextServer.getText().toString())) {
                textInputLayoutServer.setError(getString(R.string.msg_error_invalid_url));
            } else {
                textInputLayoutServer.setErrorEnabled(false);
                requestLogin(editTextServer.getText().toString(), editTextKey.getText().toString());
            }
        });

        findViewById(R.id.button_login_demo).setOnClickListener(v -> {
            // TODO: "https://de.demo.grocy.info" should be placed in xml strings
            sharedPrefs.edit()
                    .putString(Constants.PREF.SERVER_URL,"https://de.demo.grocy.info")
                    .putString(Constants.PREF.API_KEY, "")
                    .apply();
            finish();
        });
    }

    private void requestLogin(String server, String key) {
        String url = server + "/api/system/info?GROCY-API-KEY=" + key;
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    showSnackbar("Response is: "+ response.substring(0, 100));
                    sharedPrefs.edit()
                            .putString(Constants.PREF.SERVER_URL, server)
                            .putString(Constants.PREF.API_KEY, key)
                            .apply();
                    finish();
                },
                error -> showSnackbar("That didn't work!" + error)) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                int statusCode = response.statusCode;
                Log.i(TAG, "parseNetworkResponse: " + statusCode);
                return super.parseNetworkResponse(response);
            }
        };

        queue.add(stringRequest);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        queue.getCache().clear();
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
