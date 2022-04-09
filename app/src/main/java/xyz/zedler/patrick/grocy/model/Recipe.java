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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.grocy.util.NumUtil;

@Entity(tableName = "recipe_table")
public class Recipe implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "description")
  @SerializedName("description")
  private String description;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "type")
  @SerializedName("type")
  private String type;

  @ColumnInfo(name = "picture_file_name")
  @SerializedName("picture_file_name")
  private String pictureFileName;

  @ColumnInfo(name = "base_servings")
  @SerializedName("base_servings")
  private int baseServings;

  @ColumnInfo(name = "desired_servings")
  @SerializedName("desired_servings")
  private int desiredServings;

  @ColumnInfo(name = "not_check_shoppinglist")
  @SerializedName("not_check_shoppinglist")
  private int notCheckShoppingList;

  public Recipe() {
  }  // for Room

  @Ignore
  public Recipe(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    productId = parcel.readInt();
    type = parcel.readString();
    pictureFileName = parcel.readString();
    baseServings = parcel.readInt();
    desiredServings = parcel.readInt();
    notCheckShoppingList = parcel.readInt();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeInt(productId);
    dest.writeString(type);
    dest.writeString(pictureFileName);
    dest.writeInt(baseServings);
    dest.writeInt(desiredServings);
    dest.writeInt(notCheckShoppingList);
  }

  public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {

    @Override
    public Recipe createFromParcel(Parcel in) {
      return new Recipe(in);
    }

    @Override
    public Recipe[] newArray(int size) {
      return new Recipe[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description == null ? "" : description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPictureFileName() {
    return pictureFileName;
  }

  public void setPictureFileName(String pictureFileName) {
    this.pictureFileName = pictureFileName;
  }

  public int getBaseServings() {
    return baseServings;
  }

  public void setBaseServings(int baseServings) {
    this.baseServings = baseServings;
  }

  public int getDesiredServings() {
    return desiredServings;
  }

  public void setDesiredServings(int desiredServings) {
    this.desiredServings = desiredServings;
  }

  public int getNotCheckShoppingList() {
    return notCheckShoppingList;
  }

  public boolean isNotCheckShoppingList() {
    return notCheckShoppingList == 1;
  }

  public void setNotCheckShoppingList(int notCheckShoppingList) {
    this.notCheckShoppingList = notCheckShoppingList;
  }

  public void setNotCheckShoppingList(boolean notCheckShoppingList) {
    this.notCheckShoppingList = notCheckShoppingList ? 1 : 0;
  }

  public static JSONObject getJsonFromRecipe(Recipe recipe, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object name = recipe.name;
      Object description = recipe.description != null ? recipe.description : "";
      Object productId = recipe.productId;
      Object type = recipe.type;
      Object pictureFileName = recipe.pictureFileName;
      Object baseServings = recipe.baseServings;
      Object desiredServings = recipe.desiredServings;
      Object notCheckShoppingList = recipe.notCheckShoppingList;

      json.put("name", name);
      json.put("description", description);
      json.put("product_id", productId);
      json.put("type", type);
      json.put("picture_file_name", pictureFileName);
      json.put("base_servings", baseServings);
      json.put("desired_servings", desiredServings);
      json.put("not_check_shoppinglist", notCheckShoppingList);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromRecipe: " + e);
      }
    }
    return json;
  }

  public static Recipe getRecipeFromId(List<Recipe> recipes, int id) {
    for (Recipe recipe : recipes) {
      if (recipe.getId() == id) {
        return recipe;
      }
    }
    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Recipe recipe = (Recipe) o;
    return Objects.equals(id, recipe.id) &&
        Objects.equals(name, recipe.name) &&
        Objects.equals(description, recipe.description) &&
        Objects.equals(productId, recipe.productId) &&
        Objects.equals(type, recipe.type) &&
        Objects.equals(pictureFileName, recipe.pictureFileName) &&
        Objects.equals(baseServings, recipe.baseServings) &&
        Objects.equals(desiredServings, recipe.desiredServings) &&
        Objects.equals(notCheckShoppingList, recipe.notCheckShoppingList);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, name, description, productId, type, pictureFileName, baseServings, desiredServings, notCheckShoppingList);
  }

  @NonNull
  @Override
  public String toString() {
    return "Recipe(" + name + ")";
  }
}
