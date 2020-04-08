package xyz.zedler.patrick.grocy.web;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

public class WebRequest {

    private RequestQueue requestQueue;

    public WebRequest(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void get(
            String url,
            Runnable onQueued,
            OnResponseListener onSuccess,
            OnErrorListener onError,
            Runnable onLeave
    ) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.GET,
                        url,
                        response -> {
                            onSuccess.onResponse(response);
                            onLeave.run();
                        }, error -> {
                            onError.onError(error.getMessage());
                        })
        );
        onQueued.run();
    }

    public void get(String url, OnResponseListener onSuccess, OnErrorListener onError) {
        requestQueue.add(
                new StringRequest(
                        Request.Method.GET,
                        url,
                        response -> {
                            onSuccess.onResponse(response);
                        }, error -> {
                            onError.onError(error.getMessage());
                        })
        );
    }

    public void post(String url, String json) {

    }

    public void put(String url, String json) {

    }
    public void delete(String url, Runnable onQueued) {

    }

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public interface OnErrorListener {
        void onError(String msg);
        // json format
    }
}
