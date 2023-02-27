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

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Language;

public class LocaleUtil {

  public static boolean followsSystem() {
    return AppCompatDelegate.getApplicationLocales().isEmpty();
  }

  @NonNull
  public static Locale getLocale() {
    if (followsSystem()) {
      if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        return Locale.getDefault();
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return Resources.getSystem().getConfiguration().getLocales().get(0);
      } else {
        //noinspection deprecation
        return Resources.getSystem().getConfiguration().locale;
      }
    } else {
      Locale locale = AppCompatDelegate.getApplicationLocales().get(0);
      return locale != null ? locale : Locale.getDefault();
    }
  }

  public static String getLocaleName() {
    Locale locale = getLocale();
    return locale.getDisplayName(locale);
  }

  public static List<Language> getLanguages(Context context) {
    List<Language> languages = new ArrayList<>();
    String localesRaw = ResUtil.getRawText(context, R.raw.locales);
    if (localesRaw.trim().isEmpty()) {
      return languages;
    }
    String[] locales = localesRaw.split("\n\n");
    for (String locale : locales) {
      languages.add(new Language(locale));
    }
    Collections.sort(languages);
    return languages;
  }

  public static String getLanguageCode(LocaleListCompat locales) {
    if (!locales.isEmpty()) {
      return Objects.requireNonNull(locales.get(0)).toLanguageTag();
    } else {
      return null;
    }
  }

  public static Locale getLocaleFromCode(@Nullable String languageCode) {
    if (languageCode == null) {
      return Locale.getDefault();
    }
    try {
      String[] codeParts = languageCode.split("-");
      if (codeParts.length > 1) {
        return new Locale(codeParts[0], codeParts[1]);
      } else {
        return new Locale(languageCode);
      }
    } catch (Exception e) {
      return Locale.getDefault();
    }
  }

  public static String getLangFromLanguageCode(@NonNull String languageCode) {
    String[] codeParts = languageCode.split("-");
    if (codeParts.length > 1) {
      return codeParts[0];
    } else {
      return languageCode;
    }
  }
}
