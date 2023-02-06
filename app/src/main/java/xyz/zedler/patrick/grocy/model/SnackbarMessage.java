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

package xyz.zedler.patrick.grocy.model;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class SnackbarMessage extends Event {

  private final String message;
  private String actionText;
  private View.OnClickListener action;
  private String duration;

  public SnackbarMessage(@NonNull String message) {
    this.message = message;
  }

  public SnackbarMessage(@NonNull String message, int durationSecs) {
    this.message = message;
    this.duration = String.valueOf(durationSecs * 1000);
  }

  public SnackbarMessage setAction(
      @NonNull String actionText,
      @NonNull View.OnClickListener action
  ) {
    this.actionText = actionText;
    this.action = action;
    return this;
  }

  public void setDurationSecs(int duration) {
    this.duration = String.valueOf(duration * 1000);
  }

  public Snackbar getSnackbar(View view) {
    Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
    if (actionText != null) {
      snackbar.setAction(actionText, action);
    }
    if (NumUtil.isStringInt(duration)) {
      snackbar.setDuration(Integer.parseInt(duration));
    }
    View v = snackbar.getView();
    TextView text = v.findViewById(com.google.android.material.R.id.snackbar_text);
    text.setMaxLines(3);
    return snackbar;
  }

  @Override
  public int getType() {
    return Event.SNACKBAR_MESSAGE;
  }

}