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
import androidx.room.ColumnInfo;
import androidx.room.DatabaseView;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@DatabaseView("WITH RECURSIVE r1(recipe_id, includes_recipe_id, includes_servings, level)\n"
    + "AS ( "
    + " SELECT "
    + "  id AS recipe_id, "
    + "  id AS includes_recipe_id, "
    + "  1 AS includes_servings, "
    + "  0 AS level "
    + " FROM recipe_table "
    + " "
    + " UNION ALL "
    + " "
    + " SELECT "
    + "  rn.recipe_id, "
    + "  r1.includes_recipe_id, "
    + "  CASE WHEN r1.level = 0 THEN rn.servings ELSE (SELECT servings FROM recipe_nesting_table WHERE recipe_id = r1.recipe_id AND includes_recipe_id = r1.includes_recipe_id) END AS includes_servings, "
    + "  r1.level + 1 AS level "
    + " FROM recipe_nesting_table rn, r1 r1 "
    + " WHERE rn.includes_recipe_id = r1.recipe_id "
    + ") "
    + "SELECT "
    + " *, "
    + " 1 AS id "
    + "FROM r1;")
public class RecipeNestingResolved {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "recipe_id")
  @SerializedName("recipe_id")
  private int recipeId;

  @ColumnInfo(name = "includes_recipe_id")
  @SerializedName("includes_recipe_id")
  private int includesRecipeId;

  @ColumnInfo(name = "includes_servings")
  @SerializedName("includes_servings")
  private double includesServings;

  @ColumnInfo(name = "level")
  @SerializedName("level")
  private int level;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getRecipeId() {
    return recipeId;
  }

  public void setRecipeId(int recipeId) {
    this.recipeId = recipeId;
  }

  public int getIncludesRecipeId() {
    return includesRecipeId;
  }

  public void setIncludesRecipeId(int includesRecipeId) {
    this.includesRecipeId = includesRecipeId;
  }

  public double getIncludesServings() {
    return includesServings;
  }

  public void setIncludesServings(double includesServings) {
    this.includesServings = includesServings;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  @NonNull
  @Override
  public String toString() {
    return "RecipeNestingResolved(" + id + ")";
  }
}
