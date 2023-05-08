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
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.R;

public class RecipeParsed implements Parcelable {

  private String title;
  private String totalTime;
  private String yields;
  private String instructions;
  private String image;
  private ArrayList<Ingredient> ingredients;
  private HashMap<String, Integer> assignedUnits;

  public RecipeParsed() {

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

  public void storeUnitAssignment(String part, Integer unitId) {
    if (part == null) return;
    if (assignedUnits == null) assignedUnits = new HashMap<>();
    assignedUnits.put(part, unitId);
  }

  public void updateAllWordsWithEntities() {
    for (Ingredient ingredient : ingredients) {
      ingredient.updateWordsWithEntities();
    }
  }

  public void updateWordsAssignmentState() {
    if (ingredients == null) return;
    for (Ingredient ingredient : ingredients) {
      ingredient.updateWordsAssignmentState();
    }
  }

  public void updateUnitParts() {
    if (ingredients == null) return;
    for (Ingredient ingredient : ingredients) {
      ingredient.updateUnitPart(assignedUnits);
    }
  }

  public void setIngredientParts(JSONObject parsedIngredients) throws JSONException {
    for (Ingredient ingredient : this.ingredients) {
      JSONObject ingredient_entities = parsedIngredients.getJSONObject(ingredient.getIngredientText());

      for (Iterator<String> itEntities = ingredient_entities.keys(); itEntities.hasNext(); ) {
        String entity = itEntities.next();
        if (!ingredient_entities.has(entity)) continue;
        JSONObject entities_info = ingredient_entities.getJSONObject(entity);

        switch (entity) {
          case "product":
            ingredient.addIngredientPart(IngredientPart.ENTITY_PRODUCT,
                entities_info.getInt("start"), entities_info.getInt("end"));
            break;
          case "quantity":
            ingredient.addIngredientPart(IngredientPart.ENTITY_AMOUNT,
                entities_info.getInt("start"), entities_info.getInt("end"));
            break;
          case "unit":
            ingredient.addIngredientPart(IngredientPart.ENTITY_UNIT, entities_info.getInt("start"),
                entities_info.getInt("end"));
            break;
          default:
            ingredient.addIngredientPart(IngredientPart.ENTITY_EXTRA_INFO,
                entities_info.getInt("start"), entities_info.getInt("end"));
            break;
        }
      }
    }
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

  public static class Ingredient implements Parcelable {
    private final String ingredientText;
    private final ArrayList<IngredientWord> ingredientWords;
    private HashMap<String, IngredientPart> ingredientParts;

    public Ingredient(String text) {
      ingredientText = text;
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
      IngredientPart partToDelete = null;
      for (IngredientPart part : ingredientParts.values()) {
        if (!entity.equals(part.getEntity()) && word.getStartIndex() >= part.getStartIndex()
            && word.getEndIndex() <= part.getEndIndex()) {
          int endIndex = word.getStartIndex()-1;
          if (endIndex > part.getStartIndex()) {
            part.setEndIndex(endIndex);
          } else {
            partToDelete = part;
          }
          break;
        }
      }
      if (partToDelete != null) ingredientParts.remove(partToDelete.getEntity());

      IngredientPart part = ingredientParts.get(entity);
      if (part == null) {
        ingredientParts.put(entity, new IngredientPart(entity, word.getStartIndex(), word.getEndIndex()));
      } else if (word.getStartIndex() == part.getStartIndex() && word.getEndIndex() == part.getEndIndex()) {
        ingredientParts.remove(entity);
      } else if (word.getStartIndex() == part.getStartIndex() && word.getEndIndex() < part.getEndIndex()) {
        ingredientParts.put(entity, new IngredientPart(entity, word.getStartIndex(), word.getEndIndex()));
      } else if (word.getStartIndex() < part.getStartIndex() && word.getEndIndex() <= part.getStartIndex()) {
        ingredientParts.put(entity, new IngredientPart(entity, word.getStartIndex(), part.getEndIndex()));
      } else if (word.getStartIndex() >= part.getEndIndex() && word.getEndIndex() > part.getEndIndex()) {
        ingredientParts.put(entity, new IngredientPart(entity, part.getStartIndex(), word.getEndIndex()));
      } else if (word.getStartIndex() >= part.getStartIndex() && word.getEndIndex() <= part.getEndIndex()) {
        ingredientParts.put(entity, new IngredientPart(entity, part.getStartIndex(), word.getEndIndex()));
      }

      updateWordColor(word);
      updateWordsWithEntities();
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
        markedColor = R.color.teal_primary_90;
      } else if (unit != null && word.startIndex >= unit.startIndex && word.endIndex <= unit.endIndex) {
        entity = IngredientPart.ENTITY_UNIT;
        markedColor = R.color.yellow_primary_90;
      } else if (product != null && word.startIndex >= product.startIndex && word.endIndex <= product.endIndex) {
        entity = IngredientPart.ENTITY_PRODUCT;
        markedColor = R.color.red_primary_90;
      } else if (extraInfo != null && word.startIndex >= extraInfo.startIndex && word.endIndex <= extraInfo.endIndex) {
        entity = IngredientPart.ENTITY_EXTRA_INFO;
        markedColor = R.color.green_primary_90;
      }
      word.setEntity(entity);
      word.setMarkedColor(markedColor);
    }

    private void updateWordsWithEntities() {
      if (ingredientParts == null) return;
      for (IngredientWord word : ingredientWords) {
        for (IngredientPart part : ingredientParts.values()) {
          if (word.getStartIndex() >= part.getStartIndex()
              && word.getEndIndex() <= part.getEndIndex()) {
            word.setEntity(part.getEntity());
          }
        }
        updateWordColor(word);
      }
    }

    public void updateUnitPart(HashMap<String, Integer> assignedUnits) {
      if (assignedUnits == null) return;
      IngredientPart part = ingredientParts.get(IngredientPart.ENTITY_UNIT);
      if (part == null) return;
      String text = getTextFromPart(part);
      if (text == null) return;
      if (assignedUnits.containsKey(text)) {
        part.setAssignedGrocyObjectId(assignedUnits.get(text));
      }
    }

    private void updateWordsAssignmentState() {
      for (IngredientWord word : ingredientWords) {
        if (word.getEntity() != null && (word.getEntity().equals(IngredientPart.ENTITY_UNIT)
            || word.getEntity().equals(IngredientPart.ENTITY_PRODUCT))) {
          word.setAssignable(true);
          IngredientPart part = getPartFromWord(word);
          if (part == null) continue;
          word.setAssigned(part.getAssignedGrocyObjectId() != null);
        }
      }
    }

    public void updateWordsClickableState(String currentMappingEntity) {
      for (IngredientWord word : ingredientWords) {
        word.setClickable(word.getEntity() == null || word.getEntity() != null
            && word.getEntity().equals(currentMappingEntity));
      }
    }

    public void addIngredientPart(String entity, int start, int end) {
      if (ingredientParts == null) ingredientParts = new HashMap<>();
      ingredientParts.put(entity, new IngredientPart(entity, start, end));
    }

    @Nullable
    public IngredientPart getPartFromWord(IngredientWord word) {
      if (word.getEntity() == null) return null;
      return ingredientParts.get(word.getEntity());
    }

    public String getIngredientText() {
      return ingredientText;
    }

    public String getTextFromPart(IngredientPart part) {
      if (part == null) return null;
      return ingredientText.substring(part.getStartIndex(), part.getEndIndex());
    }

    public IngredientPart getIngredientPart(String entity) {
      return ingredientParts.get(entity);
    }

    public boolean hasNoProductTag() {
      return ingredientParts == null || !ingredientParts.containsKey(IngredientPart.ENTITY_PRODUCT);
    }

    public boolean hasProductIdAssigned() {
      if (ingredientParts == null) return false;
      IngredientPart part = ingredientParts.get(IngredientPart.ENTITY_PRODUCT);
      return part != null && part.getAssignedGrocyObjectId() != null;
    }

    @NonNull
    @Override
    public String toString() {
      return ingredientWords.toString();
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(this.ingredientText);
      dest.writeList(this.ingredientWords);
      dest.writeSerializable(this.ingredientParts);
    }

    protected Ingredient(Parcel in) {
      this.ingredientText = in.readString();
      this.ingredientWords = new ArrayList<>();
      in.readList(this.ingredientWords, IngredientWord.class.getClassLoader());
      this.ingredientParts = in.readHashMap(IngredientPart.class.getClassLoader());
    }

    public static final Creator<Ingredient> CREATOR = new Creator<>() {
      @Override
      public Ingredient createFromParcel(Parcel source) {
        return new Ingredient(source);
      }

      @Override
      public Ingredient[] newArray(int size) {
        return new Ingredient[size];
      }
    };
  }

  public static class IngredientWord implements Parcelable {
    private final String text;
    private final int startIndex;
    private final int endIndex;
    private final boolean isCard;
    private boolean isClickable;
    private String entity;
    private boolean isAssignable;
    private boolean isAssigned;
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

    public boolean isAssignable() {
      return isAssignable;
    }

    public void setAssignable(boolean assignable) {
      isAssignable = assignable;
    }

    public boolean isAssigned() {
      return isAssigned;
    }

    public void setAssigned(boolean assigned) {
      isAssigned = assigned;
    }

    public int getMarkedColor() {
      return markedColor;
    }

    public void setMarkedColor(int markedColor) {
      this.markedColor = markedColor;
    }

    public boolean isMarked() {
      return markedColor != 0 && markedColor != R.color.white;
    }

    @NonNull
    @Override
    public String toString() {
      return text;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(this.text);
      dest.writeInt(this.startIndex);
      dest.writeInt(this.endIndex);
      dest.writeByte(this.isCard ? (byte) 1 : (byte) 0);
      dest.writeByte(this.isClickable ? (byte) 1 : (byte) 0);
      dest.writeString(this.entity);
      dest.writeByte(this.isAssignable ? (byte) 1 : (byte) 0);
      dest.writeByte(this.isAssigned ? (byte) 1 : (byte) 0);
      dest.writeInt(this.markedColor);
    }

    protected IngredientWord(Parcel in) {
      this.text = in.readString();
      this.startIndex = in.readInt();
      this.endIndex = in.readInt();
      this.isCard = in.readByte() != 0;
      this.isClickable = in.readByte() != 0;
      this.entity = in.readString();
      this.isAssignable = in.readByte() != 0;
      this.isAssigned = in.readByte() != 0;
      this.markedColor = in.readInt();
    }

    public static final Creator<IngredientWord> CREATOR = new Creator<>() {
      @Override
      public IngredientWord createFromParcel(Parcel source) {
        return new IngredientWord(source);
      }

      @Override
      public IngredientWord[] newArray(int size) {
        return new IngredientWord[size];
      }
    };
  }

  public static class IngredientPart {
    public final static String ENTITY_AMOUNT = "amount";
    public final static String ENTITY_UNIT = "unit";
    public final static String ENTITY_PRODUCT = "product";
    public final static String ENTITY_EXTRA_INFO = "extra_info";

    private final String entity;
    private final int startIndex;
    private int endIndex;
    private Integer assignedGrocyObjectId;

    public IngredientPart(String entity, int startIndex, int endIndex) {
      this.entity = entity;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    public int getStartIndex() {
      return startIndex;
    }

    public int getEndIndex() {
      return endIndex;
    }

    public void setEndIndex(int endIndex) {
      this.endIndex = endIndex;
    }

    public String getEntity() {
      return entity;
    }

    public void setAssignedGrocyObjectId(Integer assignedGrocyObjectId) {
      this.assignedGrocyObjectId = assignedGrocyObjectId;
    }

    public Integer getAssignedGrocyObjectId() {
      return assignedGrocyObjectId;
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.title);
    dest.writeString(this.totalTime);
    dest.writeString(this.yields);
    dest.writeString(this.instructions);
    dest.writeString(this.image);
    dest.writeList(this.ingredients);
    dest.writeSerializable(this.assignedUnits);
  }

  protected RecipeParsed(Parcel in) {
    this.title = in.readString();
    this.totalTime = in.readString();
    this.yields = in.readString();
    this.instructions = in.readString();
    this.image = in.readString();
    this.ingredients = new ArrayList<>();
    in.readList(this.ingredients, Ingredient.class.getClassLoader());
    this.assignedUnits = in.readHashMap(Integer.class.getClassLoader());
  }

  public static final Creator<RecipeParsed> CREATOR = new Creator<>() {
    @Override
    public RecipeParsed createFromParcel(Parcel source) {
      return new RecipeParsed(source);
    }

    @Override
    public RecipeParsed[] newArray(int size) {
      return new RecipeParsed[size];
    }
  };
}
