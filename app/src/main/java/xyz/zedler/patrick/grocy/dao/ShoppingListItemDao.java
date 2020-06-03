package xyz.zedler.patrick.grocy.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import xyz.zedler.patrick.grocy.model.ShoppingListItem;

@Dao
public interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_item_table")
    List<ShoppingListItem> getAll();

    @Query("SELECT * FROM shopping_list_item_table WHERE id LIKE :id LIMIT 1")
    ShoppingListItem findById(int id);

    @Query("SELECT COUNT(*) FROM shopping_list_item_table")
    int count();

    @Insert
    void insertAll(List<ShoppingListItem> shoppingListItems);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ShoppingListItem shoppingListItem);

    @Update
    void update(ShoppingListItem shoppingListItem);

    @Delete
    void delete(ShoppingListItem shoppingListItem);

    @Query("DELETE FROM shopping_list_item_table")
    void deleteAll();

}
