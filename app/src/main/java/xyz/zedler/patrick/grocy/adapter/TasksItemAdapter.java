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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowFilterChipsBinding;
import xyz.zedler.patrick.grocy.databinding.RowTasksBinding;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMultiTasks;
import xyz.zedler.patrick.grocy.model.horizontalFilterBarSingleTask;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarSingleTasks;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;

public class TasksItemAdapter extends
    RecyclerView.Adapter<TasksItemAdapter.ViewHolder> {

  private final static String TAG = TasksItemAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final ArrayList<Task> tasks;
  private final PluralUtil pluralUtil;
  private final TasksItemAdapterListener listener;
  private final HorizontalFilterBarSingleTasks horizontalFilterBarSingleTasks;
  private final HorizontalFilterBarMultiTasks horizontalFilterBarMultiTasks;
  private final boolean showDateTracking;
  private final int daysExpiringSoon;
  private String sortMode;

  public TasksItemAdapter(
      Context context,
      ArrayList<Task> tasks,
      TasksItemAdapterListener listener,
      HorizontalFilterBarSingleTasks horizontalFilterBarSingleTasks,
      HorizontalFilterBarMultiTasks horizontalFilterBarMultiTasks,
      int tasksDoneCountInitial,
      int tasksNotDoneCountInitial,
      int tasksDueCountInitial,
      int tasksOverdueCountInitial,
      boolean showDateTracking, 
      int daysExpiringSoon,
      String sortMode
  ) {
    this.context = context;
    this.tasks = new ArrayList<>(tasks);
    this.pluralUtil = new PluralUtil(context);
    this.listener = listener;
    this.horizontalFilterBarSingleTasks = horizontalFilterBarSingleTasks;
    this.horizontalFilterBarMultiTasks = horizontalFilterBarMultiTasks;
    this.horizontalFilterBarSingleTasks
        .setItemsCount(HorizontalFilterBarSingleTasks.DONE, tasksDoneCountInitial);
    this.horizontalFilterBarSingleTasks
        .setItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE, tasksNotDoneCountInitial);
    this.horizontalFilterBarSingleTasks.setItemsCount(HorizontalFilterBarSingleTasks.DUE, tasksDueCountInitial);
    this.horizontalFilterBarSingleTasks.setItemsCount(HorizontalFilterBarSingleTasks.OVERDUE, tasksOverdueCountInitial);
    this.showDateTracking = showDateTracking;
    this.daysExpiringSoon = daysExpiringSoon;
    this.sortMode = sortMode;
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

    private final RowTaskBinding binding;

    public TaskViewHolder(RowTaskBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class FilterSingleRowViewHolder extends ViewHolder {

    private final WeakReference<Context> weakContext;
    private FilterChip chipDone;
    private FilterChip chipNotDone;
    private FilterChip chipDue;
    private FilterChip chipOverdue;
    private final HorizontalFilterBarSingleTasks horizontalFilterBarSingleTasks;

    public FilterSingleRowViewHolder(
        RowFilterChipsBinding binding,
        Context context,
        HorizontalFilterBarSingleTasks horizontalFilterBarSingleTasks
    ) {
      super(binding.getRoot());

      this.horizontalFilterBarSingleTasks = horizontalFilterBarSingleTasks;
      weakContext = new WeakReference<>(context);
      chipDue = new FilterChip(
          context,
          R.color.retro_yellow_bg,
          context.getResources().getQuantityString(R.plurals.msg_due_tasks,
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DUE),
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DUE)),
          () -> {
            FilterChip.changeStateToInactive(chipOverdue, chipDone, chipNotDone);
            horizontalFilterBarSingleTasks.setSingleFilterActive(HorizontalFilterBarSingleTasks.DUE);
          },
          horizontalFilterBarSingleTasks::resetAllFilters
      );
      chipOverdue = new FilterChip(
          context,
          R.color.retro_dirt,
          context.getResources().getQuantityString(R.plurals.msg_overdue_tasks,
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.OVERDUE),
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.OVERDUE)),
          () -> {
            FilterChip.changeStateToInactive(chipDue, chipNotDone, chipDone);
            horizontalFilterBarSingleTasks.setSingleFilterActive(HorizontalFilterBarSingleTasks.OVERDUE);
          },
          horizontalFilterBarSingleTasks::resetAllFilters
      );
      chipDone = new FilterChip(
          context,
          R.color.retro_green_bg_black,
          context.getResources().getQuantityString(R.plurals.msg_done_tasks,
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DONE),
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DONE)),
          () -> {
            FilterChip.changeStateToInactive(chipNotDone, chipDue, chipOverdue);
            horizontalFilterBarSingleTasks.setSingleFilterActive(HorizontalFilterBarSingleTasks.DONE);
          },
          horizontalFilterBarSingleTasks::resetAllFilters
      );
      chipNotDone = new FilterChip(
          context,
          R.color.retro_blue_bg,
          context.getResources().getQuantityString(R.plurals.msg_not_done_tasks,
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE),
              horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE)),
          () -> {
            FilterChip.changeStateToInactive(chipDone, chipDue, chipOverdue);
            horizontalFilterBarSingleTasks.setSingleFilterActive(HorizontalFilterBarSingleTasks.NOT_DONE);
          },
          horizontalFilterBarSingleTasks::resetAllFilters
      );
      binding.container.addView(chipDue);
      binding.container.addView(chipOverdue);
      binding.container.addView(chipDone);
      binding.container.addView(chipNotDone);
    }

    public void bind() {
      if (horizontalFilterBarSingleTasks.isNoFilterActive()) {
        FilterChip
            .changeStateToInactive(chipDone, chipNotDone, chipDue, chipOverdue);
      }
      else if (horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.DUE)) {
        FilterChip.changeStateToInactive(chipOverdue, chipDone, chipNotDone);
        FilterChip.changeStateToActive(chipDue);
      } else if (horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.OVERDUE)) {
        FilterChip.changeStateToInactive(chipDue, chipDone, chipNotDone);
        FilterChip.changeStateToActive(chipOverdue);
      } else if (horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.DONE)) {
        FilterChip.changeStateToInactive(chipDue, chipOverdue, chipNotDone);
        FilterChip.changeStateToActive(chipDone);
      } else if (horizontalFilterBarSingleTasks.isFilterActive(HorizontalFilterBarSingleTasks.NOT_DONE)) {
        FilterChip.changeStateToInactive(chipDue, chipOverdue, chipDone);
        FilterChip.changeStateToActive(chipNotDone);
      }
      chipDue.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_due_tasks,
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DUE),
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DUE)
      ));
      chipOverdue.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_overdue_tasks,
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.OVERDUE),
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.OVERDUE)
      ));
      chipDone.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_done_tasks,
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DONE),
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DONE)
      ));
      chipNotDone.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_not_done_tasks,
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE),
          horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE)
      ));
    }
  }

  public static class FilterMultiRowViewHolder extends ViewHolder {

    private final WeakReference<Context> weakContext;
    private final RowFilterChipsBinding binding;
    private InputChip chipTaskCategory;
    private final HorizontalFilterBarMultiTasks horizontalFilterBarMultiTasks;

    public FilterMultiRowViewHolder(
        RowFilterChipsBinding binding,
        Context context,
        HorizontalFilterBarMultiTasks horizontalFilterBarMultiTasks
    ) {
      super(binding.getRoot());
      this.binding = binding;
      this.horizontalFilterBarMultiTasks = horizontalFilterBarMultiTasks;
      weakContext = new WeakReference<>(context);
    }

    public void bind() {
      HorizontalFilterBarMultiTasks.Filter filterPg = horizontalFilterBarMultiTasks
          .getFilter(HorizontalFilterBarMultiTasks.TASK_CATEGORY);
      if (filterPg != null && chipTaskCategory == null) {
        chipTaskCategory = new InputChip(
            weakContext.get(),
            filterPg.getObjectName(),
            R.drawable.ic_round_category,
            true,
            () -> {
              horizontalFilterBarMultiTasks.removeFilter(HorizontalFilterBarMultiTasks.TASK_CATEGORY);
              chipTaskCategory = null;
            }
        );
        binding.container.addView(chipTaskCategory);
      } else if (filterPg != null) {
        chipTaskCategory.setText(filterPg.getObjectName());
      }
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0) {
      return -2; // filter single row
    }
    if (position == 1) {
      return -1; // filter multi row
    }
    return 0;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == -2) { // filter single row
      RowFilterChipsBinding binding = RowFilterChipsBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      );
      return new FilterSingleRowViewHolder(
          binding,
          context,
          horizontalFilterBarSingleTasks
      );
    } else if (viewType == -1) { // filter multi row
      RowFilterChipsBinding binding = RowFilterChipsBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      );
      return new FilterMultiRowViewHolder(
          binding,
          context,
          horizontalFilterBarMultiTasks
      );
    } else {
      return new TaskViewHolder(RowTaskBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();
    int movedPosition = position - 2;

    if (viewHolder.getItemViewType() == -2) { // Filter single row
      ((FilterSingleRowViewHolder) viewHolder).bind();
      return;
    } else if (viewHolder.getItemViewType() == -1) { // Filter multi row
      ((FilterMultiRowViewHolder) viewHolder).bind();
      return;
    }

    Task task = tasks.get(movedPosition);
    TaskViewHolder holder = (TaskViewHolder) viewHolder;

    // NAME

    holder.binding.textName.setText(task.getName());

    // DESCRIPTION

    holder.binding.textDescription.setText(task.getDescription());

    // DUE DATE

    String date = task.getBestBeforeDate();
    String days = null;
    boolean colorDays = false;
    if (date != null) {
      days = String.valueOf(DateUtil.getDaysFromNow(date));
    }

    if (!showDateTracking) {
      holder.binding.linearDays.setVisibility(View.GONE);
    } else if (days != null && (sortMode.equals(StockOverviewViewModel.SORT_DUE_DATE)
        || Integer.parseInt(days) <= daysExpiringSoon
        && !date.equals(Constants.DATE.NEVER_OVERDUE))
    ) {
      holder.binding.linearDays.setVisibility(View.VISIBLE);
      holder.binding.textDays.setText(new DateUtil(context).getHumanForDaysFromNow(date));
      if (Integer.parseInt(days) <= daysExpiringSoon) {
        colorDays = true;
      }
    } else {
      holder.binding.linearDays.setVisibility(View.GONE);
      holder.binding.textDays.setText(null);
    }

    if (colorDays) {
      holder.binding.textDays.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_medium)
      );
      @ColorRes int color;
      if (Integer.parseInt(days) >= 0) {
        color = R.color.retro_yellow_fg;
      } else if (task.getDueTypeInt() == Task.DUE_TYPE_BEST_BEFORE) {
        color = R.color.retro_dirt_fg;
      } else {
        color = R.color.retro_red_fg;
      }
      holder.binding.textDays.setTextColor(ContextCompat.getColor(context, color));
    } else {
      holder.binding.textDays.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_book)
      );
      holder.binding.textDays.setTextColor(
          ContextCompat.getColor(context, R.color.on_background_secondary)
      );
    }

    // CONTAINER

    holder.binding.linearContainer.setOnClickListener(
        view -> listener.onItemRowClicked(task)
    );
  }

  @Override
  public int getItemCount() {
    return tasks.size() + 2;
  }

  public interface TasksItemAdapterListener {

    void onItemRowClicked(Task task);
  }

  public void updateData(
      ArrayList<Task> newList,
      int itemsDoneCount,
      int itemsNotDoneCount,
      int itemsDueCount,
      int itemsOverdueCount,
      String sortMode
  ) {
    if (horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DONE) != itemsDoneCount
        || horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE)
        != itemsNotDoneCount
        || horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.DUE)
        != itemsDueCount
        || horizontalFilterBarSingleTasks.getItemsCount(HorizontalFilterBarSingleTasks.OVERDUE)
        != itemsOverdueCount) {
      horizontalFilterBarSingleTasks.setItemsCount(HorizontalFilterBarSingleTasks.DONE, itemsDoneCount);
      horizontalFilterBarSingleTasks.setItemsCount(HorizontalFilterBarSingleTasks.NOT_DONE, itemsNotDoneCount);
      horizontalFilterBarSingleTasks.setItemsCount(HorizontalFilterBarSingleTasks.DUE, itemsDueCount);
      horizontalFilterBarSingleTasks.setItemsCount(HorizontalFilterBarSingleTasks.OVERDUE, itemsOverdueCount);
      notifyItemChanged(0); // update viewHolder with filter row
    }

    TasksItemAdapter.DiffCallback diffCallback = new TasksItemAdapter.DiffCallback(
        this.tasks,
        newList,
        this.sortMode,
        sortMode
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.tasks.clear();
    this.tasks.addAll(newList);
    this.sortMode = sortMode;
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this));
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Task> oldItems;
    ArrayList<Task> newItems;
    String sortModeOld;
    String sortModeNew;

    public DiffCallback(
        ArrayList<Task> oldItems,
        ArrayList<Task> newItems,
        String sortModeOld,
        String sortModeNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
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
      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that dispatches update events to the given adapter with offset of 1,
   * because the first item is the filter row.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final TasksItemAdapter mAdapter;

    public AdapterListUpdateCallback(@NonNull TasksItemAdapter adapter) {
      mAdapter = adapter;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position + 2, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position + 2, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      mAdapter.notifyItemMoved(fromPosition + 2, toPosition + 2);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position + 2, count, payload);
    }
  }
}
