package xyz.zedler.patrick.grocy.web;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class CustomJsonObjectRequest extends JsonObjectRequest {

    public CustomJsonObjectRequest(
            int method,
            String url,
            @Nullable JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            @Nullable Response.ErrorListener errorListener
    ) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public CustomJsonObjectRequest(
            String url,
            @Nullable JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            @Nullable Response.ErrorListener errorListener
    ) {
        super(url, jsonRequest, listener, errorListener);
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
}
