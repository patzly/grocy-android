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

package xyz.zedler.patrick.grocy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import xyz.zedler.patrick.grocy.model.QuantityUnit;

public class PluralUtil {
  final LangPluralDetails pluralDetails;

  public PluralUtil(Locale locale) {
    String localeCode = locale.getLanguage();
    if (locale.getCountry().isEmpty()) {
      localeCode += "_" + locale.getCountry();
    }

    switch (localeCode) {
      case "cs":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n == 1 && n % 1 == 0) ? 0
                : (n >= 2 && n <= 4 && n % 1 == 0) ? 1 : (n % 1 != 0) ? 2 : 3
        );
        break;
      case "fr":
      case "pt_BR":
      case "pt_PT":
        pluralDetails = new LangPluralDetails(2, (n) -> (n > 1) ? 1 : 0);
        break;
      case "iw":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n == 1 && n % 1 == 0) ? 0
                : (n == 2 && n % 1 == 0) ? 1 : (n % 10 == 0 && n % 1 == 0 && n > 10) ? 2 : 3
        );
        break;
      case "pl":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n == 1 ? 0
                : (n % 10 >= 2 && n % 10 <= 4) && (n % 100 < 12 || n % 100 > 14) ? 1
                    : n != 1 && (n % 10 >= 0 && n % 10 <= 1) || (n % 10 >= 5 && n % 10 <= 9)
                        || (n % 100 >= 12 && n % 100 <= 14) ? 2 : 3)
        );
        break;
      case "ru":
      case "uk":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n%10==1 && n%100!=11 ? 0
                : n%10>=2 && n%10<=4 && (n%100<12 || n%100>14) ? 1
                    : n%10==0 || (n%10>=5 && n%10<=9) || (n%100>=11 && n%100<=14) ? 2 : 3)
        );
        break;
      case "zh_CN":
        pluralDetails = new LangPluralDetails(1, n -> 0);
        break;
      default:  // en de es it nb nl sv
        pluralDetails = new LangPluralDetails(2, (n) -> (n != 1) ? 1 : 0);
    }
  }

  public String getQuantityUnitPlural(QuantityUnit quantityUnit, double amount) {
    if (pluralDetails.nPlurals == 1) {
      return quantityUnit.getName();
    } else if (pluralDetails.nPlurals == 2) {
      return pluralDetails.pluralRule.getPluralPos(amount) == 0
          ? quantityUnit.getName() : quantityUnit.getNamePlural();
    } else {
      if (quantityUnit.getPluralForms() == null || quantityUnit.getPluralForms().isEmpty()) {
        return quantityUnit.getName();
      }
      String[] plurals = quantityUnit.getPluralForms().split("\n");
      ArrayList<String> pluralsFiltered = new ArrayList<>();
      for (String plural : plurals) {
        if (!plural.isEmpty()) {
          pluralsFiltered.add(plural);
        }
      }
      int pluralPos = pluralDetails.pluralRule.getPluralPos(amount);
      if (pluralsFiltered.size() != pluralDetails.nPlurals || pluralPos >= pluralsFiltered.size()) {
        return quantityUnit.getName();
      }
      return pluralsFiltered.get(pluralPos);
    }
  }

  public String getQuantityUnitPlural(
      HashMap<Integer, QuantityUnit> unitHashMap,
      int quantityUnitId,
      double amount
  ) {
    return getQuantityUnitPlural(unitHashMap.get(quantityUnitId), amount);
  }

  interface PluralRule {
    int getPluralPos(double n);
  }

  static class LangPluralDetails {
    final int nPlurals;
    final PluralRule pluralRule;

    public LangPluralDetails(int nPlurals, PluralRule pluralRule) {
      this.nPlurals = nPlurals;
      this.pluralRule = pluralRule;
    }
  }
}
