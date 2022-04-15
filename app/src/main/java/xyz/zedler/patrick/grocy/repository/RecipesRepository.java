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

import androidx.lifecycle.LiveData;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.User;

public class RecipesRepository {

  private final AppDatabase appDatabase;

  public RecipesRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface RecipesDataListener {

    void actionFinished(RecipesData data);
  }

  public static class RecipesData {

    private final List<Recipe> recipes;
    private final List<RecipeFulfillment> recipeFulfillments;

    public RecipesData(List<Recipe> recipes, List<RecipeFulfillment> recipeFulfillments) {
      this.recipes = recipes;
      this.recipeFulfillments = recipeFulfillments;
    }

    public List<Recipe> getRecipes() {
      return recipes;
    }

    public List<RecipeFulfillment> getRecipeFulfillments() {
      return recipeFulfillments;
    }
  }

  public void loadFromDatabase(RecipesDataListener listener) {
    Single
        .zip(
            appDatabase.recipeDao().getRecipes(),
            appDatabase.recipeFulfillmentDao().getRecipeFulfillments(),
            RecipesData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(listener::actionFinished)
        .subscribe();
  }

  public void updateDatabase(List<Recipe> recipes, List<RecipeFulfillment> recipeFulfillments, Runnable listener) {

    Single.concat(
            appDatabase.recipeDao().deleteRecipes(),
            appDatabase.recipeDao().insertRecipes(recipes),
            appDatabase.recipeFulfillmentDao().deleteRecipeFulfillments(),
            appDatabase.recipeFulfillmentDao().insertRecipeFulfillments(recipeFulfillments)
    )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally(listener::run)
            .subscribe();
  }
}
