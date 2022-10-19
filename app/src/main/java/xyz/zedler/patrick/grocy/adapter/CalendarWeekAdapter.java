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
import android.content.res.Resources.Theme;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.CalendarWeekAdapter.CalendarDayViewHolder;
import xyz.zedler.patrick.grocy.databinding.RowCalendarDayBinding;
import xyz.zedler.patrick.grocy.databinding.RowCalendarWeekBinding;
import xyz.zedler.patrick.grocy.view.singlerowcalendar.Week;

public class CalendarWeekAdapter extends PagedListAdapter<Week, CalendarDayViewHolder> {

  private final static String TAG = CalendarWeekAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final CalendarWeekAdapterListener listener;

  public static final DiffUtil.ItemCallback<Week> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
    @Override
    public boolean areItemsTheSame(@NonNull Week oldItem, @NonNull Week newItem) {
      return true;
    }

    @Override
    public boolean areContentsTheSame(@NonNull Week oldItem, @NonNull Week newItem) {
      return oldItem.equals(newItem);
    }
  };

  public CalendarWeekAdapter(
      Context context,
      CalendarWeekAdapterListener listener,
      DiffUtil.ItemCallback<Week> dateItemCallback
  ) {
    super(dateItemCallback);
    this.context = context;
    this.listener = listener;
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public class CalendarDayViewHolder extends ViewHolder {

    private final RowCalendarWeekBinding binding;

    public CalendarDayViewHolder(RowCalendarWeekBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }

    public void onBind(Week week) {
      if (week == null) return;

      for (int i = 0; i < binding.container.getChildCount(); i++) {
        RowCalendarDayBinding bindingDay = RowCalendarDayBinding.bind(binding.container.getChildAt(i));
        bindingDay.weekday.setText(
            week.getStartDate().plusDays(i).getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault()));
        bindingDay.day.setText(String.valueOf(week.getStartDate().plusDays(i).getDayOfMonth()));

        if (week.getSelectedDayOfWeek() == i) {
          bindingDay.card.setStrokeColor(ContextCompat.getColor(context, R.color.material_dynamic_secondary50));
          bindingDay.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.material_dynamic_secondary50));
          bindingDay.weekday.setTextColor(ContextCompat.getColor(context, R.color.white));
          bindingDay.day.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
          TypedValue typedValue = new TypedValue();
          Theme theme = context.getTheme();
          theme.resolveAttribute(R.attr.colorOutline, typedValue, true);
          @ColorInt int colorOutline = typedValue.data;

          bindingDay.card.setStrokeColor(colorOutline);
          bindingDay.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.transparent));
          bindingDay.weekday.setTextColor(ContextCompat.getColor(context, R.color.black));
          bindingDay.day.setTextColor(ContextCompat.getColor(context, R.color.black));
        }

        int finalI = i;
        bindingDay.card.setFocusable(true);
        bindingDay.card.setOnFocusChangeListener(
            (view, focus) -> {
              if (focus) {
                onSelect(week, finalI);
              }
            }
        );
      }
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
  }

  public void onSelect(Week week, int dayOfWeek) {
    if (getCurrentList() == null) return;
    int index = 0;
    for (Week weekTemp : getCurrentList()) {
      if (weekTemp.getStartDate().isEqual(week.getStartDate())) {
        onSelect(index, dayOfWeek);
        return;
      }
      index++;
    }
  }

  public void onSelect(int position, int dayOfWeek) {
    if (getCurrentList() == null) return;
    for (Week week : getCurrentList()) {
      week.setSelectedDayOfWeek(-1);
    }
    if (position+1 > getItemCount()) return;
    Week week = getItem(position);
    if (week == null) return;
    week.setSelectedDayOfWeek(dayOfWeek);
    listener.onItemRowClicked(week);
    notifyDataSetChanged();
  }

  public int getPositionOfWeek(LocalDate startDate) {
    if (getCurrentList() == null) return -1;
    int index = 0;
    for (Week weekTemp : getCurrentList()) {
      if (weekTemp.getStartDate().isEqual(startDate)) {
        return index;
      }
      index++;
    }
    return -1;
  }

  public interface CalendarWeekAdapterListener {

    void onItemRowClicked(Week week);
  }
}
