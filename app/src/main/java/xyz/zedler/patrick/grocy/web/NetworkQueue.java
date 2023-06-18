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
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;

public class NetworkQueue {

  private final ArrayList<QueueItem> queueItems;
  private final OnQueueEmptyListener onQueueEmptyListener;
  private final OnMultiTypeErrorListener onErrorListener;
  private final RequestQueue requestQueue;
  private final String uuidQueue;
  private int queueSize;
  private boolean isRunning;

  public NetworkQueue(
      OnQueueEmptyListener onQueueEmptyListener,
      OnMultiTypeErrorListener onErrorListener,
      RequestQueue requestQueue
  ) {
    this.onQueueEmptyListener = onQueueEmptyListener;
    this.onErrorListener = onErrorListener;
    this.requestQueue = requestQueue;
    queueItems = new ArrayList<>();
    uuidQueue = UUID.randomUUID().toString();
    queueSize = 0;
    isRunning = false;
  }

  public NetworkQueue append(QueueItem... queueItems) {
    for (QueueItem queueItem : queueItems) {
      if (queueItem == null) continue;
      this.queueItems.add(queueItem);
      queueSize++;
    }
    return this;
  }

  public void appendWhileRunning(QueueItem queueItem) {
    if (queueItem == null) return;
    this.queueItems.add(queueItem);
    queueSize++;
    executeQueueItem();
  }

  public void start() {
    if (isRunning) {
      return;
    } else {
      isRunning = true;
    }
    if (queueItems.isEmpty()) {
      if (onQueueEmptyListener != null) {
        onQueueEmptyListener.onQueueEmpty(false);
      }
      return;
    }

    executeQueueItem();
  }

  private void executeQueueItem() {
    if (queueItems.isEmpty() || queueSize == 0) {
      return;
    }

    QueueItem queueItem = queueItems.remove(0);
    queueItem.perform(response -> {
      queueSize--;
      if (queueSize > 0) {
        executeQueueItem();
        return;
      }
      isRunning = false;
      if (onQueueEmptyListener != null) {
        onQueueEmptyListener.onQueueEmpty(true);
      }
      reset(false);
    }, error -> {
      isRunning = false;
      if (onErrorListener != null) {
        onErrorListener.onError(error);
      }
      reset(true);
    }, uuidQueue);
  }

  public int getSize() {
    return queueSize;
  }

  public boolean isEmpty() {
    return queueSize == 0;
  }

  public void reset(boolean cancelAll) {
    if (cancelAll) {
      requestQueue.cancelAll(uuidQueue);
    }
    queueItems.clear();
    queueSize = 0;
  }

  public interface OnQueueEmptyListener {
    void onQueueEmpty(boolean dataLoaded); // dataLoaded = queue was not empty on start
  }
}
