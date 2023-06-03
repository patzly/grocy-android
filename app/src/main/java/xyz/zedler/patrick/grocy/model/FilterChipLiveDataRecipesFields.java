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

import android.app.Application;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataRecipesFields extends FilterChipLiveDataFields {

  public final static int ID_FIELD_DUE_SCORE = 0;
  public final static int ID_FIELD_FULFILLMENT = 1;
  public final static int ID_FIELD_CALORIES = 2;
  public final static int ID_FIELD_DESIRED_SERVINGS = 3;
  public final static int ID_FIELD_PICTURE = 4;

  public final static String FIELD_DUE_SCORE = "field_due_score";
  public final static String FIELD_FULFILLMENT = "field_fulfillment";
  public final static String FIELD_CALORIES = "field_calories";
  public final static String FIELD_DESIRED_SERVINGS = "field_desired_servings";
  public final static String FIELD_PICTURE = "field_picture";

  private final Application application;

  public FilterChipLiveDataRecipesFields(Application application, Runnable clickListener) {
    super(PREF.RECIPES_FIELDS, application, FIELD_DUE_SCORE, FIELD_FULFILLMENT, FIELD_PICTURE);
    this.application = application;
    setFilterText();
    setItems();
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setValues(item.getItemId());
        setItems();
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  private void setFilterText() {
    setText(application.getString(R.string.property_fields));
  }

  public void setValues(int id) {
    String field = null;
    if (id == ID_FIELD_DUE_SCORE) {
      field = FIELD_DUE_SCORE;
    } else if (id == ID_FIELD_FULFILLMENT) {
      field = FIELD_FULFILLMENT;
    } else if (id == ID_FIELD_CALORIES) {
      field = FIELD_CALORIES;
    } else if (id == ID_FIELD_DESIRED_SERVINGS) {
      field = FIELD_DESIRED_SERVINGS;
    } else if (id == ID_FIELD_PICTURE) {
      field = FIELD_PICTURE;
    }
    enableOrDisableField(field);
    storeFieldStates();
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_DUE_SCORE,
        0,
        application.getString(R.string.property_due_score),
        isFieldActive(FIELD_DUE_SCORE)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_FULFILLMENT,
        0,
        application.getString(R.string.property_requirements_fulfilled),
        isFieldActive(FIELD_FULFILLMENT)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_CALORIES,
        0,
        application.getString(R.string.property_calories),
        isFieldActive(FIELD_CALORIES)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_DESIRED_SERVINGS,
        0,
        application.getString(R.string.property_servings_desired),
        isFieldActive(FIELD_DESIRED_SERVINGS)
    ));
    menuItemDataList.add(new MenuItemData(
        ID_FIELD_PICTURE,
        0,
        application.getString(R.string.property_picture),
        isFieldActive(FIELD_PICTURE)
    ));
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, false));
    emitValue();
  }
}