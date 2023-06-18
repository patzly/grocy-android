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

package xyz.zedler.patrick.grocy.model;

import androidx.annotation.NonNull;
import java.util.Objects;

public class QuantityUnitConversionPath {
  private final int depth;
  private final String productId;
  private final int fromQuId;
  private final int toQuId;
  private final double factor;
  private final String path;

  public QuantityUnitConversionPath(QuantityUnitConversion conversion) {
    this.depth = 1;
    this.productId = conversion.getProductId();
    this.fromQuId = conversion.getFromQuId();
    this.toQuId = conversion.getToQuId();
    this.factor = conversion.getFactor();
    this.path = "/" + conversion.getFromQuId() + "/" + conversion.getToQuId() + "/";
  }

  public QuantityUnitConversionPath(int depth, String productId, int fromQuId, int toQuId, double factor, String path) {
    this.depth = depth;
    this.productId = productId;
    this.fromQuId = fromQuId;
    this.toQuId = toQuId;
    this.factor = factor;
    this.path = path;
  }

  public int getDepth() {
    return depth;
  }

  public String getProductId() {
    return productId;
  }

  public int getProductIdInt() {
    return productId != null ? Integer.parseInt(productId) : -1;
  }

  public int getFromQuId() {
    return fromQuId;
  }

  public int getToQuId() {
    return toQuId;
  }

  public double getFactor() {
    return factor;
  }

  public String getPath() {
    return path;
  }

  public QuantityUnitConversionResolved toConversion(int id) {
    QuantityUnitConversionResolved conversion = new QuantityUnitConversionResolved();
    conversion.setId(id);
    conversion.setFromQuId(fromQuId);
    conversion.setToQuId(toQuId);
    conversion.setProductId(productId);
    conversion.setFactor(factor);
    return conversion;
  }

  @NonNull
  @Override
  public String toString() {
    return path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuantityUnitConversionPath that = (QuantityUnitConversionPath) o;
    return fromQuId == that.fromQuId && toQuId == that.toQuId && Objects.equals(productId,
        that.productId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId, fromQuId, toQuId);
  }
}

