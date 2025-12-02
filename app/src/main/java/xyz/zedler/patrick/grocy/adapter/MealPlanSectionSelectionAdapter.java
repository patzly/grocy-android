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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowMasterEditSelectionSheetBinding;
import xyz.zedler.patrick.grocy.model.MealPlanSection;

public class MealPlanSectionSelectionAdapter extends
    RecyclerView.Adapter<MealPlanSectionSelectionAdapter.ViewHolder> {

  private final List<MealPlanSection> sections;
  private final String selectedSectionId;
  private final OnSectionSelectedListener listener;

  public interface OnSectionSelectedListener {
    void onSectionSelected(MealPlanSection section);
  }

  public MealPlanSectionSelectionAdapter(
      List<MealPlanSection> sections,
      String selectedSectionId,
      OnSectionSelectedListener listener
  ) {
    this.sections = sections;
    this.selectedSectionId = selectedSectionId;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(
        RowMasterEditSelectionSheetBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
    );
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    MealPlanSection section = sections.get(position);

    String displayText = section.getName();
    // Show time info if available and not the "None" option
    if (section.getId() != -1 && section.getTimeInfo() != null && !section.getTimeInfo().isEmpty()) {
      displayText = section.getName() + " (" + section.getTimeInfo() + ")";
    }
    holder.binding.textMasterEditSelectionName.setText(displayText);

    // Highlight selected section
    boolean isSelected = section.getId() == -1
        ? selectedSectionId == null || selectedSectionId.isEmpty()
        : String.valueOf(section.getId()).equals(selectedSectionId);

    if (isSelected) {
      holder.binding.imageMasterEditSelectionSelected.setVisibility(View.VISIBLE);
    } else {
      holder.binding.imageMasterEditSelectionSelected.setVisibility(View.INVISIBLE);
    }

    holder.binding.linearMasterEditSelectionContainer.setOnClickListener(v -> {
      if (listener != null) {
        listener.onSectionSelected(section);
      }
    });
  }

  @Override
  public int getItemCount() {
    return sections.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    private final RowMasterEditSelectionSheetBinding binding;

    public ViewHolder(RowMasterEditSelectionSheetBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}
