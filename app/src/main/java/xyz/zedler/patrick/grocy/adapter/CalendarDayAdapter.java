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
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import xyz.zedler.patrick.grocy.adapter.CalendarDayAdapter.CalendarDayViewHolder;
import xyz.zedler.patrick.grocy.databinding.RowCalendarWeekBinding;

public class CalendarDayAdapter extends PagedListAdapter<LocalDate, CalendarDayViewHolder> {

  private final static String TAG = CalendarDayAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final int screenWidth;
  private final CalendarDayAdapterListener listener;

  public static final DiffUtil.ItemCallback<LocalDate> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
    @Override
    public boolean areItemsTheSame(@NonNull LocalDate oldItem, @NonNull LocalDate newItem) {
      return oldItem.isEqual(newItem);
    }

    @Override
    public boolean areContentsTheSame(@NonNull LocalDate oldItem, @NonNull LocalDate newItem) {
      return oldItem.isEqual(newItem);
    }
  };

  public CalendarDayAdapter(
      Context context,
      int width,
      CalendarDayAdapterListener listener,
      DiffUtil.ItemCallback<LocalDate> dateItemCallback
      ) {
    super(dateItemCallback);
    this.context = context;
    this.screenWidth = width;
    this.listener = listener;
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class CalendarDayViewHolder extends ViewHolder {

    private final RowCalendarWeekBinding binding;

    public CalendarDayViewHolder(RowCalendarWeekBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    public void onBind(LocalDate date) {
      if (date == null) return;
      binding.card1.weekday.setText(date.getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
      binding.card2.weekday.setText(date.plusDays(1).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
      binding.card3.weekday.setText(date.plusDays(2).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
      binding.card4.weekday.setText(date.plusDays(3).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
      binding.card5.weekday.setText(date.plusDays(4).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
      binding.card6.weekday.setText(date.plusDays(5).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
      binding.card7.weekday.setText(date.plusDays(6).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));

      binding.card1.day.setText(String.valueOf(date.getDayOfMonth()));
      binding.card2.day.setText(String.valueOf(date.plusDays(1).getDayOfMonth()));
      binding.card3.day.setText(String.valueOf(date.plusDays(2).getDayOfMonth()));
      binding.card4.day.setText(String.valueOf(date.plusDays(3).getDayOfMonth()));
      binding.card5.day.setText(String.valueOf(date.plusDays(4).getDayOfMonth()));
      binding.card6.day.setText(String.valueOf(date.plusDays(5).getDayOfMonth()));
      binding.card7.day.setText(String.valueOf(date.plusDays(6).getDayOfMonth()));
    }
  }

  @NonNull
  @Override
  public CalendarDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new CalendarDayViewHolder(RowCalendarWeekBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull CalendarDayViewHolder holder, int position) {
    if (getItem(position) != null) {
      holder.onBind(getItem(position));
    }

    // CONTAINER

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked()
    );
  }

  public interface CalendarDayAdapterListener {

    void onItemRowClicked();
  }
}
