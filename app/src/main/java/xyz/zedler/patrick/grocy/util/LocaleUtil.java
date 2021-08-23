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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Language;

public class LocaleUtil {

  public static Locale getDeviceLocale() {
    Locale device;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      device = Resources.getSystem().getConfiguration().getLocales().get(0);
    } else {
      device = Resources.getSystem().getConfiguration().locale;
    }
    return device;
  }

  public static Locale getUserLocale(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    String code = sharedPrefs.getString(
        Constants.SETTINGS.APPEARANCE.LANGUAGE,
        Constants.SETTINGS_DEFAULT.APPEARANCE.LANGUAGE
    );
    if (code == null) {
      return getNearestSupportedLocale(context, getDeviceLocale());
    }
    try {
      return LocaleUtil.getLocaleFromCode(code);
    } catch (Exception e) {
      return getNearestSupportedLocale(context, getDeviceLocale());
    }
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
    SortUtil.sortLanguagesByName(languages);
    return languages;
  }

  public static Locale getLocaleFromCode(String languageCode) {
    String[] codeParts = languageCode.split("_");
    if (codeParts.length > 1) {
      return new Locale(codeParts[0], codeParts[1]);
    } else {
      return new Locale(languageCode);
    }
  }

  public static Locale getNearestSupportedLocale(Context context, @NonNull Locale input) {
    final HashMap<String, Language> languageHashMap = getLanguagesHashMap(context);
    return getNearestSupportedLocale(languageHashMap, input);
  }

  public static Locale getNearestSupportedLocale(HashMap<String, Language> languageHashMap,
      @NonNull Locale input) {
    Language language = languageHashMap.get(input.toString());
    if (language == null) {
      language = languageHashMap.get(input.getLanguage());
    }
    if (language == null) {
      return input;
    } else {
      try {
        return getLocaleFromCode(language.getCode());
      } catch (Exception e) {
        return input;
      }
    }
  }

  public static HashMap<String, Language> getLanguagesHashMap(Context context) {
    final HashMap<String, Language> languageHashMap = new HashMap<>();
    for (Language language : getLanguages(context)) {
      languageHashMap.put(language.getCode(), language);
    }
    return languageHashMap;
  }
}
