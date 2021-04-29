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

package xyz.zedler.patrick.grocy.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import xyz.zedler.patrick.grocy.model.ShoppingList;

@Dao
public interface ShoppingListDao {

  @Query("SELECT * FROM shopping_list_table")
  LiveData<List<ShoppingList>> getAllLive();

  @Query("SELECT * FROM shopping_list_table")
  List<ShoppingList> getAll();

  @Query("SELECT * FROM shopping_list_table WHERE id IN (:shoppingListIds)")
  List<ShoppingList> loadAllByIds(int[] shoppingListIds);

  @Query("SELECT * FROM shopping_list_table WHERE name LIKE :name LIMIT 1")
  ShoppingList findByName(String name);

  @Query("SELECT COUNT(*) FROM shopping_list_table")
  int count();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<ShoppingList> shoppingLists);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(ShoppingList shoppingList);

  @Delete
  void delete(ShoppingList shoppingList);

  @Query("DELETE FROM shopping_list_table")
  void deleteAll();

}
