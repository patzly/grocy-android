/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.web;

import com.android.volley.RequestQueue;
import java.util.ArrayList;
import java.util.UUID;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnLoadingListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;

public class NetworkQueue {

  private final ArrayList<QueueItem> queueItems;
  private final OnQueueEmptyListener onQueueEmptyListener;
  private final OnMultiTypeErrorListener onErrorListener;
  private final OnLoadingListener onLoadingListener;
  private final RequestQueue requestQueue;
  private final String uuidQueue;
  private int requestsNotFinishedCount;
  private boolean isRunning; // state of queue
  private boolean isLoading; // state of "loading" circle

  public NetworkQueue(
      RequestQueue requestQueue,
      OnQueueEmptyListener onQueueEmptyListener,
      OnMultiTypeErrorListener onErrorListener,
      OnLoadingListener onLoadingListener
  ) {
    this.onQueueEmptyListener = onQueueEmptyListener;
    this.onErrorListener = onErrorListener;
    this.onLoadingListener = onLoadingListener;
    this.requestQueue = requestQueue;
    queueItems = new ArrayList<>();
    uuidQueue = UUID.randomUUID().toString();
    requestsNotFinishedCount = 0;
    isRunning = false;
    isLoading = false;
  }

  public NetworkQueue append(QueueItem... queueItems) {
    for (QueueItem queueItem : queueItems) {
      if (queueItem == null) continue;
      this.queueItems.add(queueItem);
      requestsNotFinishedCount++;
    }
    return this;
  }

  public void appendWhileRunning(QueueItem queueItem) {
    if (queueItem == null) return;
    this.queueItems.add(queueItem);
    requestsNotFinishedCount++;
    executeQueueItems();
  }

  public void start() {
    if (isRunning) {
      return;
    } else {
      isRunning = true;
      isLoading = false;
    }
    if (queueItems.isEmpty()) {
      if (onLoadingListener != null) {
        onLoadingListener.onLoadingChanged(false);
      }
      if (onQueueEmptyListener != null) {
        onQueueEmptyListener.onQueueEmpty(false);
      }
      return;
    }
    executeQueueItems();
  }

  private void executeQueueItems() {
    if (queueItems.isEmpty() || requestsNotFinishedCount == 0) {
      return;
    }

    for (QueueItem queueItem : queueItems) {
      if (!(queueItem instanceof QueueItemWithoutLoading) && !isLoading
          && onLoadingListener != null) {
        // this prevents loading circle to appear when shopping mode updates data but nothing has
        // changed on server. In this case, all QueueItems are null except for products because
        // QuantityUnitConversions rely on it and are updated after products. So loading circle
        // only appears if QueueItem is not QueueItemWithoutLoading, which is always the case
        // except in the condition explained.
        onLoadingListener.onLoadingChanged(true);
        isLoading = true;
      }
      queueItem.perform(response -> {
        requestsNotFinishedCount--;
        if (requestsNotFinishedCount > 0) {
          return;
        }
        isRunning = false;
        isLoading = false;
        if (onLoadingListener != null) {
          onLoadingListener.onLoadingChanged(false);
        }
        if (onQueueEmptyListener != null) {
          onQueueEmptyListener.onQueueEmpty(true); // TODO: Test it
        }
        reset(false);
      }, error -> {
        isRunning = false;
        isLoading = false;
        if (onLoadingListener != null) {
          onLoadingListener.onLoadingChanged(false);
        }
        if (onErrorListener != null) {
          onErrorListener.onError(error);
        }
        reset(true);
      }, uuidQueue);
    }
    queueItems.clear();
  }

  public int getSize() {
    return requestsNotFinishedCount;
  }

  public boolean isEmpty() {
    return requestsNotFinishedCount == 0;
  }

  public void reset(boolean cancelAll) {
    if (cancelAll) {
      requestQueue.cancelAll(uuidQueue);
    }
    queueItems.clear();
    requestsNotFinishedCount = 0;
  }

  public abstract static class QueueItem {
    public abstract void perform(
        OnStringResponseListener responseListener,
        OnMultiTypeErrorListener errorListener,
        String uuid
    );

    public void perform(String uuid) {
      // UUID is for cancelling the requests; should be uuidHelper from above
      perform(null, null, uuid);
    }
  }

  public abstract static class QueueItemWithoutLoading extends QueueItem {

  }

  public interface OnQueueEmptyListener {
    void onQueueEmpty(boolean updated);
  }
}
