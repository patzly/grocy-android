package xyz.zedler.patrick.grocy.model;

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

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;

public class SnackbarMessage extends Event {

    private String message;
    private String actionText;
    private View.OnClickListener action;

    public SnackbarMessage(@NonNull String message) {
        this.message = message;
    }

    public SnackbarMessage setAction(
            @NonNull String actionText,
            @NonNull View.OnClickListener action
    ) {
        this.actionText = actionText;
        this.action = action;
        return this;
    }

    public Snackbar getSnackbar(Context context, View view) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        if(actionText != null) {
            snackbar.setAction(actionText, action);
            snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.secondary));
        }
        return snackbar;
    }

    @Override
    public int getType() {
        return Event.SNACKBAR_MESSAGE;
    }

}