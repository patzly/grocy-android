package xyz.zedler.patrick.grocy.viewmodel;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import xyz.zedler.patrick.grocy.util.Constants;

/**
 * A SingleLiveEvent used for Snackbar messages. Like a {@link SingleLiveEvent} but also prevents
 * null messages and uses a custom observer.
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
public class SnackbarMessage extends SingleLiveEvent<SnackbarMessage.Message> {

    public static class Message {
        String msg;
        int type;

        public Message(@NonNull String msg) {
            this.msg = msg;
            this.type = Constants.MessageType.NORMAL;
        }

        public Message(@NonNull String msg, int type) {
            this.msg = msg;
            this.type = type;
        }

        @Nullable
        public String getMsg() {
            return msg;
        }

        public int getType() {
            return type;
        }
    }

    public void observe(LifecycleOwner owner, final SnackbarObserver observer) {
        super.observe(owner, t -> {
            if (t == null) {
                return;
            }
            observer.onNewMessage(t);
        });
    }

    public interface SnackbarObserver {
        /**
         * Called when there is a new message to be shown.
         * @param message The new message, non-null.
         */
        void onNewMessage(Message message);
    }

}