package xyz.zedler.patrick.grocy.dao;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.model.ShoppingListItem;

@Dao
public interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_item_table")
    LiveData<List<ShoppingListItem>> getAllLive();

    @Query("SELECT * FROM shopping_list_item_table")
    List<ShoppingListItem> getAll();

    @Query("SELECT * FROM shopping_list_item_table WHERE id LIKE :id LIMIT 1")
    ShoppingListItem findById(int id);

    @Query("SELECT COUNT(*) FROM shopping_list_item_table")
    int count();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ArrayList<ShoppingListItem> shoppingListItems);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ShoppingListItem... shoppingListItems);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShoppingListItem shoppingListItem);

    @Update
    void update(ShoppingListItem shoppingListItem);

    @Delete
    void delete(ShoppingListItem shoppingListItem);

    @Query("DELETE FROM shopping_list_item_table")
    void deleteAll();

}
