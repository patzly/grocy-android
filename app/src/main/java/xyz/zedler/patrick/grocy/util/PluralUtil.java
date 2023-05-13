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
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.model.QuantityUnit;

public class PluralUtil {
  final LangPluralDetails pluralDetails;
  boolean rulesImplemented = true;

  public PluralUtil(Context context) {
    String langCode = context.getResources().getConfiguration().locale.getLanguage();

    switch (langCode) {
      case "ar":
        pluralDetails = new LangPluralDetails(
            6,
            n -> n==0 ? 0 : n==1 ? 1 : n==2 ? 2 : n%100>=3 && n%100<=10 ? 3 : n%100>=11 && n%100<=99 ? 4 : 5
        );
        break;

      case "ca":
      case "da":
      case "de":
      case "el":
      case "et":
      case "eu":
      case "fi":
      case "hu":
      case "nb":
      case "nn":
      case "nl":
      case "no":
      case "sv":
        pluralDetails = new LangPluralDetails(
            2,
            n -> (n != 1) ? 1 : 0
        );
        break;

      case "cs":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n == 1 && n % 1 == 0) ? 0 : (n >= 2 && n <= 4 && n % 1 == 0) ? 1: (n % 1 != 0 ) ? 2 : 3
        );
        break;

      case "es":
      case "it":
        pluralDetails = new LangPluralDetails(
            3,
            n -> n == 1 ? 0 : n != 0 && n % 1000000 == 0 ? 1 : 2
        );
        break;

      case "fr":
      case "pt":
        pluralDetails = new LangPluralDetails(
            3,
            n -> (n == 0 || n == 1) ? 0 : n != 0 && n % 1000000 == 0 ? 1 : 2
        );
        break;

      case "he":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n == 1 && n % 1 == 0) ? 0 : (n == 2 && n % 1 == 0) ? 1: (n % 10 == 0 && n % 1 == 0 && n > 10) ? 2 : 3
        );
        break;

      case "id":
      case "ja":
      case "ko":
      case "zh":
        pluralDetails = new LangPluralDetails(
            1,
            n -> 0
        );
        break;

      case "lt":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n % 10 == 1 && (n % 100 > 19 || n % 100 < 11) ? 0 : (n % 10 >= 2 && n % 10 <=9) && (n % 100 > 19 || n % 100 < 11) ? 1 : n % 1 != 0 ? 2: 3)
        );
        break;

      case "lv":
        pluralDetails = new LangPluralDetails(
            3,
            n -> (n%10==1 && n%100!=11 ? 0 : n != 0 ? 1 : 2)
        );
        break;

      case "pl":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n==1 ? 0 : (n%10>=2 && n%10<=4) && (n%100<12 || n%100>14) ? 1 : n!=1 && (n%10>=0 && n%10<=1) || (n%10>=5 && n%10<=9) || (n%100>=12 && n%100<=14) ? 2 : 3)
        );
        break;

      case "ro":
        pluralDetails = new LangPluralDetails(
            3,
            n -> (n==1?0:(((n%100>19)||((n%100==0)&&(n!=0)))?2:1))
        );
        break;

      case "ru":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<12 || n%100>14) ? 1 : n%10==0 || (n%10>=5 && n%10<=9) || (n%100>=11 && n%100<=14)? 2 : 3)
        );
        break;

      case "sk":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n % 1 == 0 && n == 1 ? 0 : n % 1 == 0 && n >= 2 && n <= 4 ? 1 : n % 1 != 0 ? 2: 3)
        );
        break;

      case "tr":
        pluralDetails = new LangPluralDetails(
            2,
            n -> (n > 1) ? 1 : 0
        );
        break;

      case "uk":
        pluralDetails = new LangPluralDetails(
            4,
            n -> (n % 1 == 0 && n % 10 == 1 && n % 100 != 11 ? 0 : n % 1 == 0 && n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 > 14) ? 1 : n % 1 == 0 && (n % 10 ==0 || (n % 10 >=5 && n % 10 <=9) || (n % 100 >=11 && n % 100 <=14 )) ? 2: 3)
        );
        break;
      default:  // en de es it nb nl sv
        pluralDetails = new LangPluralDetails(2, (n) -> (n != 1) ? 1 : 0);
        rulesImplemented = false;
    }
  }

  public boolean languageRulesNotImplemented() {
    return !rulesImplemented;
  }

  public boolean isPluralFormsFieldNecessary() {
    return pluralDetails.nPlurals > 2;
  }

  public String getQuantityUnitPlural(QuantityUnit quantityUnit, double amount) {
    if (quantityUnit == null) {
      return null;
    }
    if (pluralDetails.nPlurals == 1) {
      return quantityUnit.getName();
    } else if (pluralDetails.nPlurals == 2) {
      return pluralDetails.pluralRule.getPluralPos(amount) == 0
          ? quantityUnit.getName() : quantityUnit.getNamePlural();
    } else {
      int pluralPos = pluralDetails.pluralRule.getPluralPos(amount);
      ArrayList<String> pluralForms = getFilteredPluralForms(quantityUnit.getPluralForms());

      if (pluralForms.isEmpty() && pluralPos > 0) {
        return quantityUnit.getNamePlural();
      } else if (pluralForms.isEmpty()) {
        return quantityUnit.getName();
      } else if (pluralForms.size() == pluralDetails.nPlurals && pluralPos < pluralForms.size()) {
        return pluralForms.get(pluralPos);
      } else if (pluralForms.size() == pluralDetails.nPlurals) {
        return pluralForms.get(0);
      } else if (pluralPos > 0) {
        return quantityUnit.getNamePlural();
      } else {
        return quantityUnit.getName();
      }
    }
  }

  public String getQuantityUnitPlural(
      HashMap<Integer, QuantityUnit> unitHashMap,
      int quantityUnitId,
      double amount
  ) {
    return getQuantityUnitPlural(unitHashMap.get(quantityUnitId), amount);
  }

  private ArrayList<String> getFilteredPluralForms(@Nullable String pluralForms) {
    ArrayList<String> pluralFormsFiltered = new ArrayList<>();
    if (pluralForms == null || pluralForms.isEmpty()) {
      return pluralFormsFiltered;
    }
    String[] plurals = pluralForms.split("\n");
    for (String plural : plurals) {
      if (!plural.isEmpty()) {
        pluralFormsFiltered.add(plural);
      }
    }
    return pluralFormsFiltered;
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
