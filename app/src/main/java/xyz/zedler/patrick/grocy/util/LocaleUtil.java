package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        if (code == null) return getDeviceLocale();
        try {
            return LocaleUtil.getLocaleFromCode(code);
        } catch (Exception e) {
            return getDeviceLocale();
        }
    }

    public static List<Language> getLanguages(Context context) {
        List<Language> languages = new ArrayList<>();
        String localesRaw = TextUtil.readFromFile(context, "LOCALES.txt");
        if (localesRaw.trim().isEmpty()) return languages;
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
            return new Locale(codeParts[0], "", codeParts[1]);
        } else {
            return new Locale(languageCode);
        }
    }
}
