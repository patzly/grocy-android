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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowTaskEntryBinding;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class TaskEntryAdapter extends
    RecyclerView.Adapter<TaskEntryAdapter.ViewHolder> {

  private final static String TAG = TaskEntryAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<Task> tasks;
  private final HashMap<Integer, TaskCategory> taskCategoriesHashMap;
  private final HashMap<Integer, User> usersHashMap;
  private final TasksItemAdapterListener listener;
  private String sortMode;
  private boolean sortAscending;

  public TaskEntryAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ArrayList<Task> tasks,
      HashMap<Integer, TaskCategory> taskCategories,
      HashMap<Integer, User> usersHashMap,
      TasksItemAdapterListener listener,
      String sortMode,
      boolean sortAscending
  ) {
    this.context = context;
    this.linearLayoutManager = linearLayoutManager;
    this.tasks = new ArrayList<>(tasks);
    this.taskCategoriesHashMap = new HashMap<>(taskCategories);
    this.usersHashMap = new HashMap<>(usersHashMap);
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

    private final RowTaskEntryBinding binding;

    public TaskViewHolder(RowTaskEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new TaskViewHolder(RowTaskEntryBinding.inflate(
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

    if (task.isDone()) {
      holder.binding.title.setPaintFlags(
          holder.binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
      holder.binding.title.setAlpha(0.6f);
    } else {
      holder.binding.title.setPaintFlags(
          holder.binding.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
      );
      holder.binding.title.setAlpha(1.0f);
    }

    // DUE DATE

    String date = task.getDueDate();
    Integer days = null;
    boolean colorDays = false;
    if (date != null && !date.isEmpty()) {
      days = DateUtil.getDaysFromNow(date);
    }

    if (days != null && !task.isDone()) {
      holder.binding.days.setVisibility(View.VISIBLE);
      holder.binding.days.setText(new DateUtil(context).getHumanForDaysFromNow(date));
      if (days <= 5) {
        colorDays = true;
      }
    } else {
      holder.binding.days.setVisibility(View.GONE);
    }

    if (colorDays) {
      holder.binding.days.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_medium)
      );
      @ColorRes int color;
      if (days < 0) {
        color = R.color.retro_red_fg;
      } else if (days == 0) {
        color = R.color.retro_blue_fg;
      } else {
        color = R.color.retro_yellow;
      }
      holder.binding.days.setTextColor(ContextCompat.getColor(context, color));
    } else {
      holder.binding.days.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_book)
      );
      holder.binding.days.setTextColor(
          ContextCompat.getColor(context, R.color.on_background_secondary)
      );
    }

    // CATEGORY

    TaskCategory category = NumUtil.isStringInt(task.getCategoryId())
        ? taskCategoriesHashMap.get(Integer.parseInt(task.getCategoryId())) : null;
    if (task.isDone()) {
      holder.binding.category.setVisibility(View.GONE);
    } else if (category != null) {
      holder.binding.category.setText(category.getName());
      holder.binding.category.setTypeface(holder.binding.category.getTypeface(), Typeface.NORMAL);
      holder.binding.category.setVisibility(View.VISIBLE);
    } else {
      holder.binding.category.setText(holder.binding.category.getContext()
          .getString(R.string.subtitle_uncategorized));
      holder.binding.category.setTypeface(holder.binding.category.getTypeface(), Typeface.ITALIC);
      holder.binding.category.setVisibility(View.VISIBLE);
    }

    // USER

    User user = NumUtil.isStringInt(task.getAssignedToUserId())
        ? usersHashMap.get(Integer.parseInt(task.getAssignedToUserId())) : null;
    if (user != null && !task.isDone()) {
      holder.binding.user.setText(user.getDisplayName());
      holder.binding.user.setVisibility(View.VISIBLE);
    } else {
      holder.binding.user.setVisibility(View.GONE);
    }

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
      HashMap<Integer, TaskCategory> taskCategoriesHashMap,
      HashMap<Integer, User> usersHashMap,
      String sortMode,
      boolean sortAscending
  ) {

    TaskEntryAdapter.DiffCallback diffCallback = new TaskEntryAdapter.DiffCallback(
        this.tasks,
        newList,
        this.taskCategoriesHashMap,
        taskCategoriesHashMap,
        this.usersHashMap,
        usersHashMap,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.tasks.clear();
    this.tasks.addAll(newList);
    this.taskCategoriesHashMap.clear();
    this.taskCategoriesHashMap.putAll(taskCategoriesHashMap);
    this.usersHashMap.clear();
    this.usersHashMap.putAll(usersHashMap);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Task> oldItems;
    ArrayList<Task> newItems;
    HashMap<Integer, TaskCategory> taskCategoriesHashMapOld;
    HashMap<Integer, TaskCategory> taskCategoriesHashMapNew;
    HashMap<Integer, User> usersHashMapOld;
    HashMap<Integer, User> usersHashMapNew;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;

    public DiffCallback(
        ArrayList<Task> oldItems,
        ArrayList<Task> newItems,
        HashMap<Integer, TaskCategory> taskCategoriesHashMapOld,
        HashMap<Integer, TaskCategory> taskCategoriesHashMapNew,
        HashMap<Integer, User> usersHashMapOld,
        HashMap<Integer, User> usersHashMapNew,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.taskCategoriesHashMapOld = taskCategoriesHashMapOld;
      this.taskCategoriesHashMapNew = taskCategoriesHashMapNew;
      this.usersHashMapOld = usersHashMapOld;
      this.usersHashMapNew = usersHashMapNew;
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

      TaskCategory taskCategoryOld = NumUtil.isStringInt(oldItem.getCategoryId())
          ? taskCategoriesHashMapOld.get(Integer.parseInt(oldItem.getCategoryId())) : null;
      TaskCategory taskCategoryNew = NumUtil.isStringInt(newItem.getCategoryId())
          ? taskCategoriesHashMapNew.get(Integer.parseInt(newItem.getCategoryId())) : null;
      if (taskCategoryOld == null && taskCategoryNew != null
          || taskCategoryOld != null && taskCategoryNew == null
          || taskCategoryOld != null && !taskCategoryOld.equals(taskCategoryNew)) {
        return false;
      }

      User userOld = NumUtil.isStringInt(oldItem.getAssignedToUserId())
          ? usersHashMapOld.get(Integer.parseInt(oldItem.getAssignedToUserId())) : null;
      User userNew = NumUtil.isStringInt(newItem.getAssignedToUserId())
          ? usersHashMapNew.get(Integer.parseInt(newItem.getAssignedToUserId())) : null;
      if (userOld == null && userNew != null
          || userOld != null && userNew == null
          || userOld != null && !userOld.equals(userNew)) {
        return false;
      }

      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final TaskEntryAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull TaskEntryAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
