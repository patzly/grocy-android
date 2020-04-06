package xyz.zedler.patrick.grocy.task;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class JsonDownloadTask extends AsyncTask<Void, Void, String> {

	private static final String TAG = "JsonDownloadTask";

	private String url;
	private OnFinishedListener onFinishedListener;
	private Runnable onCancelledAction;
	private StringBuilder content = new StringBuilder();

	public JsonDownloadTask(
			String url,
			OnFinishedListener onFinishedListener,
			Runnable onCancelledAction
	) {
		this.url = url;
		this.onFinishedListener = onFinishedListener;
		this.onCancelledAction = onCancelledAction;
	}

	@Override
	protected String doInBackground(Void... voids) {
		if(url != null) {
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
						new URL(url).openStream()
				));
				String line;
				while ((line = bufferedReader.readLine()) != null) content.append(line);
				bufferedReader.close();
			} catch (Exception e) {
				Log.e(TAG, "doInBackground: " + e);
				cancel(true);
				return null;
			}
		}
		return content.toString();
	}

	@Override
	protected void onPostExecute(String json) {
		if(onFinishedListener != null && json != null) onFinishedListener.onFinished(json);
	}

	@Override
	protected void onCancelled() {
		if(onCancelledAction != null) onCancelledAction.run();
	}

	public interface OnFinishedListener {
		void onFinished(String json);
	}
}
