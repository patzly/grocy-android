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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.RecipeImportMappingAdapter.IngredientViewHolder;
import xyz.zedler.patrick.grocy.adapter.RecipeImportMappingAdapter.IngredientViewHolder.OnPartClickListener;
import xyz.zedler.patrick.grocy.adapter.RecipeImportMappingAdapter.IngredientViewHolder.OnWordClickListener;
import xyz.zedler.patrick.grocy.databinding.RowRecipeImportMappingBinding;
import xyz.zedler.patrick.grocy.model.RecipeParsed;
import xyz.zedler.patrick.grocy.model.RecipeParsed.Ingredient;
import xyz.zedler.patrick.grocy.model.RecipeParsed.IngredientPart;
import xyz.zedler.patrick.grocy.model.RecipeParsed.IngredientWord;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class RecipeImportMappingAdapter extends RecyclerView.Adapter<IngredientViewHolder> {

  private final RecipeParsed recipeParsed;
  private final OnWordClickListener onWordClickListener;
  private final OnPartClickListener onPartClickListener;
  private final boolean isAssignmentMode;
  private boolean showErrors = false;

  public RecipeImportMappingAdapter(
      RecipeParsed recipeParsed,
      OnWordClickListener onWordClickListener,
      OnPartClickListener onPartClickListener,
      boolean isAssignmentMode
  ) {
    this.recipeParsed = recipeParsed;
    this.onWordClickListener = onWordClickListener;
    this.onPartClickListener = onPartClickListener;
    this.isAssignmentMode = isAssignmentMode;
  }

  public static class IngredientViewHolder extends RecyclerView.ViewHolder {
    private final RowRecipeImportMappingBinding binding;
    private final ConstraintLayout constraintLayout;
    private final OnWordClickListener onWordClickListener;
    private final OnPartClickListener onPartClickListener;

    public IngredientViewHolder(
        RowRecipeImportMappingBinding binding,
        OnWordClickListener onWordClickListener,
        OnPartClickListener onPartClickListener
    ) {
      super(binding.getRoot());
      this.binding = binding;
      constraintLayout = binding.getRoot();
      this.onWordClickListener = onWordClickListener;
      this.onPartClickListener = onPartClickListener;
    }

    public void bind(Ingredient ingredient, boolean isAssignmentMode, boolean showErrors) {
      clearTextViews();

      List<Integer> ids = new ArrayList<>();
      IngredientPart currentPart = null;
      for (IngredientWord word : ingredient.getIngredientWords()) {
        if (!isAssignmentMode && word.isCard() || isAssignmentMode && word.isMarked()) {
          Chip chip;
          if (isAssignmentMode) {
            IngredientPart part = ingredient.getPartFromWord(word);
            if (currentPart != null && part == currentPart) {
              continue;
            } else if (part == null) {
              chip = createChip(word.getText(), word, true);
            } else {
              currentPart = part;
              chip = createChip(ingredient.getTextFromPart(part), word, true);
              chip.setOnClickListener(v -> onPartClicked(ingredient, part));
            }
            chip.setEnabled(word.isAssignable());
            chip.setAlpha(word.isAssignable() ? 1.0f : 0.6f);
          } else {
            chip = createChip(word.getText(), word, false);
            chip.setOnClickListener(v -> onWordClicked(ingredient, word));
          }
          constraintLayout.addView(chip);
          ids.add(chip.getId());
        } else {
          TextView textView = createTextView(word);
          if (isAssignmentMode) textView.setAlpha(0.5f);
          constraintLayout.addView(textView);
          ids.add(textView.getId());
        }
      }
      binding.flowLayout.setReferencedIds(convertIntArray(ids));

      ConstraintSet constraintSet = new ConstraintSet();
      constraintSet.clone(constraintLayout);
      if (showErrors && ingredient.hasNoProductTag()) {
        constraintSet.connect(binding.flowLayout.getId(), ConstraintSet.END,
            binding.toolbarInfo.getId(), ConstraintSet.START, 0);
        TransitionManager.beginDelayedTransition(constraintLayout);
        constraintSet.applyTo(constraintLayout);
        binding.toolbarInfo.setVisibility(View.VISIBLE);
      } else {
        constraintSet.connect(binding.flowLayout.getId(), ConstraintSet.END,
            binding.toolbar.getId(), ConstraintSet.START, 0);
        constraintSet.applyTo(constraintLayout);
        binding.toolbarInfo.setVisibility(View.GONE);
      }
    }

    private void clearTextViews() {
      for (int i = constraintLayout.getChildCount() - 1; i >= 0; i--) {
        View child = constraintLayout.getChildAt(i);
        if (child instanceof TextView) {
          constraintLayout.removeView(child);
        }
      }
    }

    private Chip createChip(String text, IngredientWord word, boolean isAssignmentMode) {
      Context context = binding.getRoot().getContext();
      Chip chip = new Chip(context);
      chip.setId(View.generateViewId());
      chip.setText(text);
      chip.setTypeface(ResourcesCompat.getFont(context, R.font.jost_medium));
      chip.setChipStartPadding(2);
      chip.setChipEndPadding(2);
      chip.setEnsureMinTouchTargetSize(false);

      if (word.isMarked()) {
        chip.setChipBackgroundColorResource(word.getMarkedColor());
      }
      if (isAssignmentMode && word.isAssignable()) {
        chip.setChipIconVisible(true);
        chip.setChipIconTint(ColorStateList.valueOf(
            ResUtil.getColorAttr(context, R.attr.colorOnSecondaryContainer)
        ));
        chip.setIconStartPadding(UiUtil.dpToPx(context, 4));
        chip.setIconEndPadding(UiUtil.dpToPx(context, -2));
        if (word.isAssigned()) {
          chip.setChipIconResource(R.drawable.ic_round_done);
        } else {
          chip.setChipIconResource(R.drawable.ic_round_error_outline);
        }
      }
      return chip;
    }

    private TextView createTextView(IngredientWord word) {
      Context context = binding.getRoot().getContext();
      TextView textView = new TextView(context);
      textView.setId(View.generateViewId());
      textView.setText(word.getText());
      textView.setTypeface(ResourcesCompat.getFont(context, R.font.jost_medium));
      textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
      int paddingVertical = UiUtil.dpToPx(context, 4);
      textView.setPadding(0, paddingVertical, 0, paddingVertical);
      return textView;
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

    private void onPartClicked(Ingredient ingredient, IngredientPart part) {
      if (onPartClickListener != null) {
        onPartClickListener.onPartClick(ingredient, part, getAdapterPosition());
      }
    }

    public interface OnWordClickListener {
      void onWordClick(Ingredient ingredient, IngredientWord word, int position);
    }

    public interface OnPartClickListener {
      void onPartClick(Ingredient ingredient, IngredientPart part, int position);
    }
  }


  @NonNull
  @Override
  public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new IngredientViewHolder(
        RowRecipeImportMappingBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        ), onWordClickListener, onPartClickListener
    );
  }

  @Override
  public void onBindViewHolder(@NonNull final IngredientViewHolder holder, int positionDoNotUse) {
    int position = holder.getAdapterPosition();

    Ingredient ingredient = recipeParsed.getIngredients().get(position);
    holder.bind(ingredient, isAssignmentMode, showErrors);
  }

  public boolean isShowErrors() {
    return showErrors;
  }

  public void setShowErrors(boolean showErrors) {
    this.showErrors = showErrors;
  }

  @Override
  public int getItemCount() {
    return recipeParsed.getIngredients().size();
  }
}
