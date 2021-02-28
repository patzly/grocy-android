package xyz.zedler.patrick.grocy.model;

import xyz.zedler.patrick.grocy.util.LocaleUtil;

public class Language {

    private final String code;
    private final String demoDomain;
    private final String translators;
    private final String name;

    public Language(String codeDomainTranslators) {
        String[] parts = codeDomainTranslators.split("\n");
        code = parts[0];
        demoDomain = parts[1];
        translators = parts[2];
        name = LocaleUtil.getLocaleFromCode(code).getDisplayName(LocaleUtil.getDeviceLocale());
    }

    public String getCode() {
        return code;
    }

    public String getDemoDomain() {
        return demoDomain;
    }

    public String getTranslators() {
        return translators;
    }

    public String getName() {
        return name;
    }
}
