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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import androidx.lifecycle.LifecycleOwner;
import xyz.zedler.patrick.grocy.model.Event;

/**
 * A SingleLiveEvent used for all kinds of events. Like a {@link SingleLiveEvent} but also prevents
 * null messages and uses a custom observer.
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
public class EventHandler extends SingleLiveEvent<Event> {

  public void observeEvent(LifecycleOwner owner, final EventObserver observer) {
    super.observe(owner, t -> {
      if (t == null) {
        return;
      }
      observer.onNewMessage(t);
    });
  }

  public interface EventObserver {

    /**
     * Called when there is a new message to be shown.
     *
     * @param event The new event, non-null.
     */
    void onNewMessage(Event event);
  }

}