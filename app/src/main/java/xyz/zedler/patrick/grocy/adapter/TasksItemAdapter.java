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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.databinding.RowTaskItemBinding;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class TasksItemAdapter extends
    RecyclerView.Adapter<TasksItemAdapter.ViewHolder> {

  private final static String TAG = TasksItemAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final ArrayList<Task> tasks;
  private final PluralUtil pluralUtil;
  private final TasksItemAdapterListener listener;
  private String sortMode;
  private boolean sortAscending;

  public TasksItemAdapter(
      Context context,
      ArrayList<Task> tasks,
      TasksItemAdapterListener listener,
      String sortMode,
      boolean sortAscending
  ) {
    this.context = context;
    this.tasks = new ArrayList<>(tasks);
    this.pluralUtil = new PluralUtil(context);
    this.listener = listener;
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class TaskViewHolder extends ViewHolder {

    private final RowTaskItemBinding binding;

    public TaskViewHolder(RowTaskItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new TaskViewHolder(RowTaskItemBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    Task task = tasks.get(position);
    TaskViewHolder holder = (TaskViewHolder) viewHolder;

    // NAME

    holder.binding.title.setText(task.getName());

    // CONTAINER

    holder.binding.linearContainer.setOnClickListener(
        view -> listener.onItemRowClicked(task)
    );
  }

  @Override
  public int getItemCount() {
    return tasks.size();
  }

  public interface TasksItemAdapterListener {

    void onItemRowClicked(Task task);
  }

  public void updateData(
      ArrayList<Task> newList,
      String sortMode,
      boolean sortAscending
  ) {

    TasksItemAdapter.DiffCallback diffCallback = new TasksItemAdapter.DiffCallback(
        this.tasks,
        newList,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.tasks.clear();
    this.tasks.addAll(newList);
    this.sortMode = sortMode;
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Task> oldItems;
    ArrayList<Task> newItems;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;

    public DiffCallback(
        ArrayList<Task> oldItems,
        ArrayList<Task> newItems,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      Task newItem = newItems.get(newItemPos);
      Task oldItem = oldItems.get(oldItemPos);

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }
      if (sortAscendingOld != sortAscendingNew) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }
}
