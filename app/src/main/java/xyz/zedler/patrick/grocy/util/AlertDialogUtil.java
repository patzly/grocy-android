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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;

public class AlertDialogUtil {

  public static void showConfirmationDialog(
      Context context,
      String question,
      @Nullable HashMap<String, Boolean> multiChoiceItems,
      ConfirmationListener confirmationListener
  ) {
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);

    TextView title = new TextView(context);
    title.setTextAppearance(context, R.style.Widget_Grocy_TextView);
    int paddingTitle = UiUtil.dpToPx(context, 8);
    title.setPadding(paddingTitle*3, paddingTitle*2, paddingTitle*3, 0);
    title.setText(question);
    title.setTextSize(UiUtil.spToPx(context, 6));
    alertBuilder.setCustomTitle(title);

    if (multiChoiceItems != null) {
      CharSequence[] names = multiChoiceItems.keySet().toArray(new String[0]);
      Boolean[] namesChecked = multiChoiceItems.values().toArray(new Boolean[0]);
      alertBuilder.setMultiChoiceItems(
          names,
          toPrimitiveArray(namesChecked),
          (dialog, which, isChecked) -> multiChoiceItems.put((String) names[which], isChecked)
      );
    }

    alertBuilder.setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss());
    alertBuilder.setPositiveButton(R.string.action_proceed, (dialog, which) -> {
      confirmationListener.onConfirm(multiChoiceItems);
      dialog.dismiss();
    });
    alertBuilder.show();
  }

  public static void showConfirmationDialog(
      Context context,
      String question,
      Runnable confirmationListener
  ) {
    showConfirmationDialog(
        context, question,
        null,
        multiChoiceItems -> confirmationListener.run()
    );
  }

  public interface ConfirmationListener {
    void onConfirm(HashMap<String, Boolean> multiChoiceItems);
  }

  private static boolean[] toPrimitiveArray(Boolean... booleanList) {
    final boolean[] primitives = new boolean[booleanList.length];
    int index = 0;
    for (Boolean object : booleanList) {
      primitives[index++] = object;
    }
    return primitives;
  }

}
