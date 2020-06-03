package xyz.zedler.patrick.grocy.database;

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
    List<ShoppingList> getAll();

    @Query("SELECT * FROM shopping_list_table WHERE id IN (:shoppingListIds)")
    List<ShoppingList> loadAllByIds(int[] shoppingListIds);

    @Query("SELECT * FROM shopping_list_table WHERE name LIKE :name LIMIT 1")
    ShoppingList findByName(String name);

    @Query("SELECT COUNT(*) FROM shopping_list_table")
    int count();

    @Insert
    void insertAll(ShoppingList... shoppingLists);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ShoppingList shoppingList);

    @Delete
    void delete(ShoppingList shoppingList);

    @Query("DELETE FROM shopping_list_table")
    void deleteAll();

}
