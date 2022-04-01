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

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;

public class TasksRepository {

  private final AppDatabase appDatabase;

  public TasksRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface TasksDataListener {

    void actionFinished(TasksData data);
  }

  public static class TasksData {

    private final List<TaskCategory> taskGroups;
    private final List<Task> tasks;
    private final List<User> users;

    public TasksData(
        List<TaskCategory> taskGroups,
        List<Task> tasks,
        List<User> users
    ) {
      this.taskGroups = taskGroups;
      this.tasks = tasks;
      this.users = users;
    }

    public List<TaskCategory> getTaskGroups() {
      return taskGroups;
    }

    public List<Task> getTasks() {
      return tasks;
    }

    public List<User> getUsers() {
      return users;
    }
  }

  public void loadFromDatabase(TasksDataListener listener) {
    Single
        .zip(
            appDatabase.taskCategoryDao().getTaskCategories(),
            appDatabase.taskDao().getTasks(),
            appDatabase.userDao().getUsers(),
            TasksData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }
}
