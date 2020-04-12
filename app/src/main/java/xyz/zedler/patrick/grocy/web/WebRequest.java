package xyz.zedler.patrick.grocy.web;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

public class WebRequest {

    private RequestQueue requestQueue;
    private int queueSize = 0;

    public WebRequest(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void get(
            String url,
            String tag,
            OnResponseListener onResponse,
            OnErrorListener onError,
            OnEmptyQueueListener onEmptyQueue
    ) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.GET,
                        url,
                        response -> {
                            onResponse.onResponse(response);
                            queueSize--;
                            if(queueSize == 0) {
                                onEmptyQueue.onEmptyQueue();
                            }
                        },
                        error -> {
                            onError.onError(error);
                            queueSize--;
                            if(queueSize == 0) {
                                onEmptyQueue.onEmptyQueue();
                            }
                        }
                ).setTag(tag)
        );
        queueSize++;
    }

    public void get(String url, OnResponseListener onResponse, OnErrorListener onError) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.GET,
                        url,
                        onResponse::onResponse,
                        onError::onError
                )
        );
    }

    public void get(String url, String tag, OnResponseListener onResponse, OnErrorListener onError) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.GET,
                        url,
                        onResponse::onResponse,
                        onError::onError
                ).setTag(tag)
        );
    }

    public void cancelAll(String tag) {
        this.queueSize = 0; // TODO: Unclear with tag
        requestQueue.cancelAll(tag);
    }

    public void post(
            String url,
            JSONObject json,
            OnJsonResponseListener onResponse,
            OnErrorListener onError
    ) {
        requestQueue.add(
                new JsonObjectRequest(
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

    public void put(String url, String json) {

    }
    public void delete(String url, Runnable onQueued) {

    }

    public int getQueueSize() {
        return this.queueSize;
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

    public interface OnEmptyQueueListener {
        void onEmptyQueue();
    }
}