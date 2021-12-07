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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import android.os.AsyncTask;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;

public class TasksRepository {

  private final AppDatabase appDatabase;

  public TasksRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface TasksDataListener {

    void actionFinished(
        ArrayList<TaskCategory> taskGroups,
        ArrayList<Task> tasks
    );
  }

  public interface TasksDataUpdatedListener {

    void actionFinished();
  }

  public void loadFromDatabase(TasksDataListener listener) {
    new loadAsyncTask(appDatabase, listener).execute();
  }

  private static class loadAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final TasksDataListener listener;

    private ArrayList<TaskCategory> taskCategories;
    private ArrayList<Task> tasks;

    loadAsyncTask(AppDatabase appDatabase, TasksDataListener listener) {
      this.appDatabase = appDatabase;
      this.listener = listener;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      taskCategories = new ArrayList<>(appDatabase.taskCategoryDao().getAll());
      tasks = new ArrayList<>(appDatabase.taskDao().getAll());
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished(taskCategories, tasks);
      }
    }
  }

  public void updateDatabase(
      ArrayList<TaskCategory> taskCategories,
      ArrayList<Task> tasks,
      TasksDataUpdatedListener listener
  ) {
    new updateAsyncTask(
        appDatabase,
        taskCategories,
        tasks,
        listener
    ).execute();
  }

  private static class updateAsyncTask extends AsyncTask<Void, Void, Void> {

    private final AppDatabase appDatabase;
    private final TasksDataUpdatedListener listener;

    private final ArrayList<TaskCategory> taskCategories;
    private final ArrayList<Task> tasks;

    updateAsyncTask(
        AppDatabase appDatabase,
        ArrayList<TaskCategory> taskCategories,
        ArrayList<Task> tasks,
        TasksDataUpdatedListener listener
    ) {
      this.appDatabase = appDatabase;
      this.listener = listener;
      this.taskCategories = taskCategories;
      this.tasks = tasks;
    }

    @Override
    protected final Void doInBackground(Void... params) {
      appDatabase.taskCategoryDao().deleteAll();
      appDatabase.taskCategoryDao().insertAll(taskCategories);
      appDatabase.taskDao().deleteAll();
      appDatabase.taskDao().insertAll(tasks);
      return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      if (listener != null) {
        listener.actionFinished();
      }
    }
  }
}
