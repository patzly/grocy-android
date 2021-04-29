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
