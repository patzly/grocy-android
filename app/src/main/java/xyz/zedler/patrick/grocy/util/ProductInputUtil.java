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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import java.util.List;
import xyz.zedler.patrick.grocy.model.ProductBarcode;

public class ProductInputUtil {

  private ProductInputUtil() {}

  public static boolean isBarcode(String input, List<ProductBarcode> barcodes) {
    if (input == null || input.trim().isEmpty()) {
      return false;
    }
    String value = input.trim();
    // Unknown numeric codes should use the same lookup flow as a scan. Unknown
    // product names still offer creation without treating the name as a barcode.
    return value.matches("[0-9]+") || GrocycodeUtil.getGrocycode(value) != null
        || (barcodes != null && ProductBarcode.getFromBarcode(barcodes, value) != null);
  }
}
