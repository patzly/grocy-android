package xyz.zedler.patrick.grocy.web;

import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomJsonObjectRequest extends JsonObjectRequest {

    private final Runnable onRequestFinished;
    private final String apiKey;

    public CustomJsonObjectRequest(
            int method,
            String url,
            String apiKey,
            @Nullable JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            @Nullable Response.ErrorListener errorListener,
            @Nullable Runnable onRequestFinished,
            int timeoutSeconds,
            String tag
    ) {
        super(method, url, jsonRequest, response -> {
            if(onRequestFinished != null) onRequestFinished.run();
            listener.onResponse(response);
        }, error -> {
            if(onRequestFinished != null) onRequestFinished.run();
            if(errorListener != null) errorListener.onErrorResponse(error);
        });
        this.onRequestFinished = onRequestFinished;
        this.apiKey = apiKey;
        if(tag != null) setTag(tag);
        setShouldCache(false);
        RetryPolicy policy = new DefaultRetryPolicy(
                timeoutSeconds * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        setRetryPolicy(policy);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET)
            );
            JSONObject result = null;
            if(jsonString.length() > 0) {
                result = new JSONObject(jsonString);
            }
            return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        if(onRequestFinished != null) onRequestFinished.run();
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> params = new HashMap<>();
        if(apiKey != null && !apiKey.isEmpty()) params.put("GROCY-API-KEY", apiKey);
        return params.isEmpty() ? Collections.emptyMap() : params;
    }
}
