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
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.time.LocalDate;
import xyz.zedler.patrick.grocy.fragment.MealPlanPagingFragment;

public class MealPlanPagerAdapter extends FragmentStateAdapter {

  private final LocalDate startDate;

  public MealPlanPagerAdapter(
      @NonNull Fragment parentFragment,
      LocalDate startDate
  ) {
    super(parentFragment);
    this.startDate = startDate;
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    LocalDate date = LocalDate.now().plusDays(position - Integer.MAX_VALUE / 2);
    return MealPlanPagingFragment.newInstance(date);
  }

  @Override
  public int getItemCount() {
    // Return the total number of days for which you want to show meal plans.
    // This could be a fixed number, or based on data you have.
    return Integer.MAX_VALUE;
  }
}
