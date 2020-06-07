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
 * along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;

public class ArrayUtil {

    public static HashMap<Integer, Location> getLocationsHashMap(ArrayList<Location> locations) {
        HashMap <Integer, Location> hashMap = new HashMap<>();
        for(Location l : locations) hashMap.put(l.getId(), l);
        return hashMap;
    }

    public static HashMap<Integer, ProductGroup> getProductGroupsHashMap(
            ArrayList<ProductGroup> productGroups
    ) {
        HashMap <Integer, ProductGroup> hashMap = new HashMap<>();
        for(ProductGroup p : productGroups) hashMap.put(p.getId(), p);
        return hashMap;
    }

    public static HashMap<Integer, QuantityUnit> getQuantityUnitsHashMap(
            ArrayList<QuantityUnit> quantityUnits
    ) {
        HashMap <Integer, QuantityUnit> hashMap = new HashMap<>();
        for(QuantityUnit q : quantityUnits) hashMap.put(q.getId(), q);
        return hashMap;
    }
}
