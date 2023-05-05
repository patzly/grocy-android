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

package xyz.zedler.patrick.grocy.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.RecipeImportMappingAdapter.IngredientViewHolder;
import xyz.zedler.patrick.grocy.adapter.RecipeImportMappingAdapter.IngredientViewHolder.OnWordClickListener;
import xyz.zedler.patrick.grocy.databinding.RowRecipeImportMappingBinding;
import xyz.zedler.patrick.grocy.model.RecipeParsed;
import xyz.zedler.patrick.grocy.model.RecipeParsed.Ingredient;
import xyz.zedler.patrick.grocy.model.RecipeParsed.IngredientWord;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class RecipeImportMappingAdapter extends RecyclerView.Adapter<IngredientViewHolder> {

  private final RecipeParsed recipeParsed;
  private final OnWordClickListener onWordClickListener;

  public RecipeImportMappingAdapter(RecipeParsed recipeParsed, OnWordClickListener onWordClickListener) {
    this.recipeParsed = recipeParsed;
    this.onWordClickListener = onWordClickListener;
  }

  public static class IngredientViewHolder extends RecyclerView.ViewHolder {
    private final RowRecipeImportMappingBinding binding;
    private final ConstraintLayout constraintLayout;
    private final OnWordClickListener onWordClickListener;

    public IngredientViewHolder(RowRecipeImportMappingBinding binding, OnWordClickListener onWordClickListener) {
      super(binding.getRoot());
      this.binding = binding;
      constraintLayout = binding.getRoot();
      this.onWordClickListener = onWordClickListener;
    }

    public void bind(Ingredient ingredient) {
      clearTextViews();

      List<Integer> ids = new ArrayList<>();
      for (IngredientWord word : ingredient.getIngredientWords()) {
        TextView textView = createTextView(word);
        constraintLayout.addView(textView);
        ids.add(textView.getId());

        textView.setOnClickListener(v -> onWordClicked(ingredient, word));
      }

      binding.flowLayout.setReferencedIds(convertIntArray(ids));
    }

    private void clearTextViews() {
      for (int i = constraintLayout.getChildCount() - 1; i >= 0; i--) {
        View child = constraintLayout.getChildAt(i);
        if (child instanceof TextView) {
          constraintLayout.removeView(child);
        }
      }
    }

    private TextView createTextView(IngredientWord word) {
      Context context = binding.getRoot().getContext();
      TextView textView = new TextView(context);
      textView.setId(View.generateViewId());
      textView.setText(word.getText());
      textView.setTypeface(ResourcesCompat.getFont(context, R.font.jost_book));
      textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
      int paddingVertical = UiUtil.dpToPx(context, 4);
      int paddingHorizontal;

      if (word.isCard()) {
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel()
            .toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, UiUtil.dpToPx(context, 8))
            .build();
        MaterialShapeDrawable backgroundDrawable = new MaterialShapeDrawable(shapeAppearanceModel);

        int baseColor = ContextCompat.getColor(context, word.getMarkedColor());
        int adjustedColor = adjustBrightness(baseColor, word.isClickable() ? 1.0f : 1.8f);
        ColorStateList colorStateList = ColorStateList.valueOf(adjustedColor);

        backgroundDrawable.setFillColor(colorStateList);
        ViewCompat.setBackground(textView, backgroundDrawable);
        textView.setClickable(word.isClickable());
        textView.setFocusable(word.isClickable());
        paddingHorizontal = UiUtil.dpToPx(context, 8);
      } else {
        paddingHorizontal = UiUtil.dpToPx(context, 0);
      }

      if (word.isMarked()) {
        textView.setTextColor(ContextCompat.getColorStateList(context, R.color.white));
      } else {
        textView.setTextColor(ContextCompat.getColorStateList(context, R.color.black));
      }

      textView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
      textView.setTranslationZ(word.isClickable() ? 6f : 0f);
      return textView;
    }

    public int adjustBrightness(int color, float brightnessFactor) {
      float[] hsv = new float[3];
      Color.colorToHSV(color, hsv); // Convert color to HSV format
      hsv[2] *= brightnessFactor; // Adjust the brightness (value) component
      hsv[2] = Math.min(hsv[2], 1.0f); // Make sure the brightness doesn't exceed 1.0
      return Color.HSVToColor(hsv); // Convert the color back to RGB format
    }

    private int[] convertIntArray(List<Integer> integers) {
      int[] intArray = new int[integers.size()];
      for (int i = 0; i < integers.size(); i++) {
        intArray[i] = integers.get(i);
      }
      return intArray;
    }

    private void onWordClicked(Ingredient ingredient, IngredientWord word) {
      if (onWordClickListener != null) {
        onWordClickListener.onWordClick(ingredient, word, getAdapterPosition());
      }
    }

    public interface OnWordClickListener {
      void onWordClick(Ingredient ingredient, IngredientWord word, int position);
    }
  }


  @NonNull
  @Override
  public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new IngredientViewHolder(
        RowRecipeImportMappingBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        ), onWordClickListener
    );
  }

  @Override
  public void onBindViewHolder(@NonNull final IngredientViewHolder holder, int positionDoNotUse) {
    int position = holder.getAdapterPosition();

    Ingredient ingredient = this.recipeParsed.getIngredients().get(position);
    holder.bind(ingredient);
  }

  @Override
  public int getItemCount() {
    return recipeParsed.getIngredients().size();
  }
}
