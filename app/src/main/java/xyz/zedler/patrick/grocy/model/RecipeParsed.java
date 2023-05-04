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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;

public class RecipeParsed implements Parcelable {

  @SerializedName("title")
  private String title;

  @SerializedName("total_time")
  private String totalTime;

  @SerializedName("yields")
  private String yields;

  @SerializedName("instructions")
  private String instructions;

  @SerializedName("image")
  private String image;

  @SerializedName("ingredients")
  private ArrayList<Ingredient> ingredients;

  public RecipeParsed() {

  }

  public RecipeParsed(Parcel parcel) {
    title = parcel.readString();
    totalTime = parcel.readString();
    yields = parcel.readString();
    instructions = parcel.readString();
    image = parcel.readString();
    ingredients = new ArrayList<>();
    parcel.readList(ingredients, Ingredient.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(title);
    dest.writeString(totalTime);
    dest.writeString(yields);
    dest.writeString(instructions);
    dest.writeString(image);
    dest.writeList(ingredients);
  }

  public static final Creator<RecipeParsed> CREATOR = new Creator<>() {

    @Override
    public RecipeParsed createFromParcel(Parcel in) {
      return new RecipeParsed(in);
    }

    @Override
    public RecipeParsed[] newArray(int size) {
      return new RecipeParsed[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTotalTime() {
    return totalTime;
  }

  public void setTotalTime(String totalTime) {
    this.totalTime = totalTime;
  }

  public String getYields() {
    return yields;
  }

  public void setYields(String yields) {
    this.yields = yields;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public ArrayList<Ingredient> getIngredients() {
    return ingredients;
  }

  public void setIngredients(ArrayList<Ingredient> ingredients) {
    this.ingredients = ingredients;
  }

  public static RecipeParsed fromJson(JSONObject jsonObject) throws JSONException {
    RecipeParsed recipeParsed = new RecipeParsed();
    if (jsonObject.has("title")) {
      recipeParsed.setTitle(jsonObject.getString("title"));
    }
    if (jsonObject.has("total_time")) {
      recipeParsed.setTotalTime(jsonObject.getString("total_time"));
    }
    if (jsonObject.has("yields")) {
      recipeParsed.setYields(jsonObject.getString("yields"));
    }
    if (jsonObject.has("ingredients")) {
      Log.i("TAG", "fromJson: " + jsonObject.getJSONArray("ingredients"));
      ArrayList<Ingredient> ingredients = new ArrayList<>();
      JSONArray jArray = jsonObject.getJSONArray("ingredients");
      for (int i=0; i<jArray.length(); i++){
        ingredients.add(new Ingredient(jArray.getString(i)));
      }
      recipeParsed.setIngredients(ingredients);
    }
    if (jsonObject.has("instructions")) {
      recipeParsed.setInstructions(jsonObject.getString("instructions"));
    }
    if (jsonObject.has("image")) {
      recipeParsed.setImage(jsonObject.getString("image"));
    }
    return recipeParsed;
  }

  @NonNull
  @Override
  public String toString() {
    return "RecipeParsed(" + title + ')';
  }

  public static class Ingredient {
    private ArrayList<IngredientWord> ingredientWords;
    private HashMap<String, IngredientPart> ingredientParts;

    public Ingredient(String text) {
      ingredientWords = new ArrayList<>();
      int currentIndex = 0;
      for (String s : text.split(" ")) {
        int startIndex = currentIndex;
        int endIndex = startIndex + s.length();

        IngredientWord word = new IngredientWord(s, startIndex, endIndex, !s.equals(","));
        updateWordColor(word);
        ingredientWords.add(word);

        currentIndex += s.length() + 1;
      }
    }

    public ArrayList<IngredientWord> getIngredientWords() {
      return ingredientWords;
    }

    public void markWord(IngredientWord word, String entity) {
      if (ingredientParts == null) {
        ingredientParts = new HashMap<>();
      }
      if (ingredientParts.get(entity) == null) {
        ingredientParts.put(entity, new IngredientPart(entity, word.getStartIndex(), word.getEndIndex()));
      }/* else if (part.start == example.entities[entity].start && part.end == example.entities[entity].end) {
        delete example.entities[entity];
      } else if (part.start == example.entities[entity].start && part.end < example.entities[entity].end) {
        example.entities[entity] = {start: part.start, end: part.end};
      } else if (part.start <= example.entities[entity].start && part.end <= example.entities[entity].start) {
        example.entities[entity] = {start: part.start, end: example.entities[entity].end};
      } else if (part.start >= example.entities[entity].end && part.end > example.entities[entity].end) {
        example.entities[entity] = {start: example.entities[entity].start, end: part.end};
      } else if (part.start > example.entities[entity].start && part.end <= example.entities[entity].end) {
        example.entities[entity] = {start: example.entities[entity].start, end: part.end};
      }*/

      updateWordColor(word);
    }

    private void updateWordColor(IngredientWord word) {
      IngredientPart amount = ingredientParts != null ? ingredientParts.get(IngredientPart.ENTITY_AMOUNT) : null;
      IngredientPart unit = ingredientParts != null ? ingredientParts.get(IngredientPart.ENTITY_UNIT) : null;
      IngredientPart product = ingredientParts != null ? ingredientParts.get(IngredientPart.ENTITY_PRODUCT) : null;
      IngredientPart extraInfo = ingredientParts != null ? ingredientParts.get(IngredientPart.ENTITY_EXTRA_INFO) : null;

      String entity = null;
      @ColorRes int markedColor = R.color.white;
      if (amount != null && word.startIndex >= amount.startIndex && word.endIndex <= amount.endIndex) {
        entity = IngredientPart.ENTITY_AMOUNT;
        markedColor = R.color.blue;
      } else if (unit != null && word.startIndex >= unit.startIndex && word.endIndex <= unit.endIndex) {
        entity = IngredientPart.ENTITY_UNIT;
        markedColor = R.color.red;
      } else if (product != null && word.startIndex >= product.startIndex && word.endIndex <= product.endIndex) {
        entity = IngredientPart.ENTITY_PRODUCT;
        markedColor = R.color.green;
      } else if (extraInfo != null && word.startIndex >= extraInfo.startIndex && word.endIndex <= extraInfo.endIndex) {
        entity = IngredientPart.ENTITY_EXTRA_INFO;
        markedColor = R.color.orange;
      }
      word.setEntity(entity);
      word.setMarkedColor(markedColor);
    }

    public void updateWordsClickableState(String currentMappingEntity) {
      for (IngredientWord word : ingredientWords) {
        word.setClickable(word.getEntity() == null || word.getEntity() != null
            && word.getEntity().equals(currentMappingEntity));
      }
    }
  }

  public static class IngredientWord {
    private String text;
    private int startIndex;
    private int endIndex;
    private boolean isCard;
    private boolean isClickable;
    private String entity;
    @ColorRes private int markedColor;

    public IngredientWord(String text, int startIndex, int endIndex, boolean isCard) {
      this.text = text;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.isCard = isCard;
      this.isClickable = true;
    }

    public String getText() {
      return text;
    }

    public int getStartIndex() {
      return startIndex;
    }

    public int getEndIndex() {
      return endIndex;
    }

    public boolean isCard() {
      return isCard;
    }

    public boolean isClickable() {
      return isClickable;
    }

    public void setClickable(boolean clickable) {
      isClickable = clickable;
    }

    public void setEntity(String entity) {
      this.entity = entity;
    }

    public String getEntity() {
      return entity;
    }

    public int getMarkedColor() {
      return markedColor;
    }

    public void setMarkedColor(int markedColor) {
      this.markedColor = markedColor;
    }

    public boolean isMarked() {
      return markedColor != R.color.white;
    }
  }

  public static class IngredientPart {
    public final static String ENTITY_AMOUNT = "amount";
    public final static String ENTITY_UNIT = "unit";
    public final static String ENTITY_PRODUCT = "product";
    public final static String ENTITY_EXTRA_INFO = "extra_info";

    private String type;
    private int startIndex;
    private int endIndex;

    public IngredientPart(String type, int startIndex, int endIndex) {
      this.type = type;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    public int getStartIndex() {
      return startIndex;
    }

    public int getEndIndex() {
      return endIndex;
    }
  }
}
