package xyz.zedler.patrick.grocy.web;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WebRequest {

    private RequestQueue requestQueue;

    public WebRequest(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void get(
            String url,
            String tag,
            OnResponseListener onResponse,
            OnErrorListener onError
    ) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                onResponse::onResponse,
                onError::onError
        );
        if(tag != null) stringRequest.setTag(tag);
        requestQueue.add(stringRequest);
    }

    public void get(String url, OnResponseListener onResponse, OnErrorListener onError) {
        get(url, null, onResponse, onError);
    }

    public void get(
            String url,
            OnResponseListener onResponse,
            OnErrorListener onError,
            String userAgent
    ) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.GET,
                        url,
                        onResponse::onResponse,
                        onError::onError
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> params = new HashMap<>();
                        params.put("User-Agent", userAgent);
                        return params;
                    }
                }
        );
    }

    public void post(
            String url,
            JSONObject json,
            OnJsonResponseListener onResponse,
            OnErrorListener onError
    ) {
        requestQueue.add(
                new CustomJsonObjectRequest(
                        Request.Method.POST,
                        url,
                        json,
                        onResponse::onResponse,
                        onError::onError
                )
        );
    }

    public void post(String url, OnResponseListener onResponse, OnErrorListener onError) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.POST,
                        url,
                        onResponse::onResponse,
                        onError::onError
                )
        );
    }

    public void put(
            String url,
            JSONObject json,
            OnJsonResponseListener onResponse,
            OnErrorListener onError
    ) {
        requestQueue.add(
                new CustomJsonObjectRequest(
                        Request.Method.PUT,
                        url,
                        json,
                        onResponse::onResponse,
                        onError::onError
                )
        );
    }

    public void delete(
            String url,
            String tag,
            OnResponseListener onResponse,
            OnErrorListener onError
    ) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                onResponse::onResponse,
                onError::onError
        );
        if(tag != null) stringRequest.setTag(tag);
        requestQueue.add(stringRequest);
    }

    public void delete(String url, OnResponseListener onResponse, OnErrorListener onError) {
        delete(url, null, onResponse, onError);
    }

    public void cancelAll(String tag) {
        requestQueue.cancelAll(tag);
    }

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public interface OnJsonResponseListener {
        void onResponse(JSONObject response);
    }

    public interface OnErrorListener {
        void onError(VolleyError error);
    }
}