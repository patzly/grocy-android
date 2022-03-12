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

package xyz.zedler.patrick.grocy.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import xyz.zedler.patrick.grocy.model.Task;

@Dao
public interface TaskDao {

  @Query("SELECT * FROM task_table")
  LiveData<List<Task>> getAllLive();

  @Query("SELECT * FROM task_table")
  List<Task> getAll();

  @Query("SELECT * FROM task_table")
  Single<List<Task>> getTasks();

  @Query("SELECT COUNT(*) FROM task_table")
  int count();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertAll(List<Task> tasks);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(Task task);

  @Delete
  void delete(Task task);

  @Query("DELETE FROM task_table")
  void deleteAll();

}
