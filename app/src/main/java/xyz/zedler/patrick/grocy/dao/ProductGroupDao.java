package xyz.zedler.patrick.grocy.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import xyz.zedler.patrick.grocy.model.ProductGroup;

@Dao
public interface ProductGroupDao {
    @Query("SELECT * FROM product_group_table")
    List<ProductGroup> getAll();

    @Query("SELECT COUNT(*) FROM product_group_table")
    int count();

    @Insert
    void insertAll(ProductGroup... productGroups);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ProductGroup productGroup);

    @Delete
    void delete(ProductGroup productGroup);

    @Query("DELETE FROM product_group_table")
    void deleteAll();

}
