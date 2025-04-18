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
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class FilterChipLiveDataProductGroup extends FilterChipLiveData {

  public final static int NO_FILTER = -1;

  private final Application application;

  public FilterChipLiveDataProductGroup(Application application, Runnable clickListener) {
    this.application = application;
    setSelectedId(-1, null);
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setSelectedId(item.getItemId(), item.getTitle().toString());
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public int getSelectedId() {
    return getItemIdChecked();
  }

  public void setSelectedId(int id, @Nullable String text) {
    if (id == NO_FILTER) {
      setActive(false);
      setText(application.getString(R.string.property_product_group));
    } else {
      setActive(true);
      assert text != null;
      setText(text);
    }
    setItemIdChecked(id);
  }

  public void setProductGroups(List<ProductGroup> productGroups) {
    SortUtil.sortProductGroupsByName(productGroups, true);
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        NO_FILTER,
        0,
        application.getString(R.string.action_no_filter)
    ));
    for (ProductGroup productGroup : productGroups) {
      menuItemDataList.add(new MenuItemData(productGroup.getId(), 0, productGroup.getName()));
    }
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, true));
    emitValue();
  }
}