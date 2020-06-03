package xyz.zedler.patrick.grocy.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import xyz.zedler.patrick.grocy.model.QuantityUnit;

@Dao
public interface QuantityUnitDao {
    @Query("SELECT * FROM quantity_unit_table")
    List<QuantityUnit> getAll();

    @Query("SELECT COUNT(*) FROM quantity_unit_table")
    int count();

    @Insert
    void insertAll(QuantityUnit... quantityUnits);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(QuantityUnit quantityUnit);

    @Delete
    void delete(QuantityUnit quantityUnit);

    @Query("DELETE FROM quantity_unit_table")
    void deleteAll();

}
