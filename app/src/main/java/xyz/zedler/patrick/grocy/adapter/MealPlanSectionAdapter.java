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

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.MealPlanSection;

public class MealPlanSectionAdapter extends RecyclerView.Adapter<MealPlanSectionAdapter.ViewHolder> {

  private final static String TAG = MealPlanSectionAdapter.class.getSimpleName();

  private final ArrayList<MealPlanSection> sections;
  private final MealPlanSectionAdapterListener listener;

  public MealPlanSectionAdapter(
      ArrayList<MealPlanSection> sections,
      MealPlanSectionAdapterListener listener
  ) {
    this.sections = sections;
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final TextView textViewName;
    private final TextView textViewTimeInfo;
    private final MaterialButton buttonEdit;
    private final MaterialButton buttonDelete;

    public ViewHolder(View view) {
      super(view);

      textViewName = view.findViewById(R.id.text_section_name);
      textViewTimeInfo = view.findViewById(R.id.text_section_time_info);
      buttonEdit = view.findViewById(R.id.button_edit);
      buttonDelete = view.findViewById(R.id.button_delete);
    }
  }

  @NonNull
  @Override
  public MealPlanSectionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new MealPlanSectionAdapter.ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(
            R.layout.row_meal_plan_section,
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final MealPlanSectionAdapter.ViewHolder holder,
      int position
  ) {
    MealPlanSection section = sections.get(holder.getAdapterPosition());

    // NAME
    holder.textViewName.setText(section.getName());

    // TIME INFO
    if (section.getTimeInfo() != null && !section.getTimeInfo().isEmpty()) {
      holder.textViewTimeInfo.setText(section.getTimeInfo());
      holder.textViewTimeInfo.setVisibility(View.VISIBLE);
    } else {
      holder.textViewTimeInfo.setVisibility(View.GONE);
    }

    // EDIT BUTTON
    holder.buttonEdit.setOnClickListener(v -> {
      if (listener != null) {
        listener.onEditSection(section);
      }
    });

    // DELETE BUTTON
    holder.buttonDelete.setOnClickListener(v -> {
      if (listener != null) {
        listener.onDeleteSection(section);
      }
    });
  }

  @Override
  public int getItemCount() {
    return sections.size();
  }

  public interface MealPlanSectionAdapterListener {
    void onEditSection(MealPlanSection section);
    void onDeleteSection(MealPlanSection section);
  }
}
