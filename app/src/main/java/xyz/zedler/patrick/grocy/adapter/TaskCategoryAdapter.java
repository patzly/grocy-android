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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.TaskCategory;

public class TaskCategoryAdapter extends RecyclerView.Adapter<TaskCategoryAdapter.ViewHolder> {

  private final static String TAG = TaskCategoryAdapter.class.getSimpleName();

  private final ArrayList<TaskCategory> taskCategories;
  private final int selectedId;
  private final TaskCategoryAdapterListener listener;

  public TaskCategoryAdapter(
      ArrayList<TaskCategory> taskCategories,
      int selectedId,
      TaskCategoryAdapterListener listener
  ) {
    this.taskCategories = taskCategories;
    this.selectedId = selectedId;
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final LinearLayout linearLayoutContainer;
    private final TextView textViewName;
    private final ImageView imageViewSelected;

    public ViewHolder(View view) {
      super(view);

      linearLayoutContainer = view.findViewById(R.id.linear_master_edit_selection_container);
      textViewName = view.findViewById(R.id.text_master_edit_selection_name);
      imageViewSelected = view.findViewById(R.id.image_master_edit_selection_selected);
    }
  }

  @NonNull
  @Override
  public TaskCategoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new TaskCategoryAdapter.ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(
            R.layout.row_master_edit_selection_sheet,
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final TaskCategoryAdapter.ViewHolder holder,
      int position
  ) {
    TaskCategory taskCategory = taskCategories.get(holder.getAdapterPosition());

    // NAME

    holder.textViewName.setText(taskCategory.getName());

    // SELECTED

    if (taskCategory.getId() == selectedId) {
      holder.imageViewSelected.setVisibility(View.VISIBLE);
    }

    // CONTAINER

    holder.linearLayoutContainer.setOnClickListener(
        view -> listener.onItemRowClicked(taskCategory)
    );
  }

  @Override
  public long getItemId(int position) {
    return taskCategories.get(position).getId();
  }

  @Override
  public int getItemCount() {
    return taskCategories.size();
  }

  public interface TaskCategoryAdapterListener {

    void onItemRowClicked(TaskCategory taskCategory);
  }
}
