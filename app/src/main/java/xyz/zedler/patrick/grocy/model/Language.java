package xyz.zedler.patrick.grocy.model;

import xyz.zedler.patrick.grocy.util.LocaleUtil;

public class Language {

    private final String code;
    private final String translators;
    private final String name;

    public Language(String codeAndTranslators) {
        String[] parts = codeAndTranslators.split("\n");
        code = parts[0];
        translators = parts[1];
        name = LocaleUtil.getLocaleFromCode(code).getDisplayName(LocaleUtil.getDeviceLocale());
    }

    public String getCode() {
        return code;
    }

    public String getTranslators() {
        return translators;
    }

    public String getName() {
        return name;
    }
}
