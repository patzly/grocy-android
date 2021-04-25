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
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import android.os.AsyncTask;

import xyz.zedler.patrick.grocy.database.AppDatabase;

public class MainRepository {
    private final AppDatabase appDatabase;

    public MainRepository(Application application) {
        this.appDatabase = AppDatabase.getAppDatabase(application);
    }

    public void clearAllTables() {
        new clearAsyncTask(appDatabase).execute();
    }

    private static class clearAsyncTask extends AsyncTask<Void, Void, Void> {
        private final AppDatabase appDatabase;

        clearAsyncTask(AppDatabase appDatabase) {
            this.appDatabase = appDatabase;
        }

        @Override
        protected final Void doInBackground(Void... params) {
            appDatabase.clearAllTables();
            return null;
        }
    }
}
