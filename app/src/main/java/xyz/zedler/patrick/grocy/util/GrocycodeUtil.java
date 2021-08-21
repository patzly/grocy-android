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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrocycodeUtil {

  private static Matcher getMatcher(String barcode) {
    return Pattern.compile("grcy:([a-z]+):([0-9]+)(:.+)*").matcher(barcode);
  }

  public static Grocycode getGrocycode(String barcode) {
    Matcher matcher = getMatcher(barcode);
    if (!matcher.matches()) return null;
    return new Grocycode(matcher);
  }

  public static class Grocycode {
    public static final String TYPE_PRODUCTS = "p";
    public static final String TYPE_BATTERIES = "b";
    public static final String TYPE_CHORES = "c";
    private final String entityIdentifier;
    private final int objectIdentifier;
    private final String[] additionalData;

    private Grocycode(Matcher matcher) {
      this.entityIdentifier = matcher.group(1);
      String objectIdStr = matcher.group(2);
      assert objectIdStr != null;
      this.objectIdentifier = Integer.parseInt(objectIdStr);
      String additionalDataStr = matcher.group(3);
      if (additionalDataStr != null) {
        additionalData = additionalDataStr.substring(1).split(":");
      } else {
        additionalData = null;
      }
    }

    public boolean isProduct() {
      return entityIdentifier.equals(TYPE_PRODUCTS);
    }

    public String getProductStockEntryId() {
      if (!isProduct() || additionalData == null || additionalData.length == 0) return null;
      return additionalData[0];
    }

    public int getObjectId() {
      return objectIdentifier;
    }
  }
}
