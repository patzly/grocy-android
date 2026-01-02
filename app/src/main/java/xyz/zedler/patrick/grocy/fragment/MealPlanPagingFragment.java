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

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import java.time.LocalDate;
import xyz.zedler.patrick.grocy.adapter.MealPlanEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.MealPlanEntryAdapter.SimpleItemTouchHelperCallback;
import xyz.zedler.patrick.grocy.databinding.FragmentMealPlanPagingBinding;
import xyz.zedler.patrick.grocy.viewmodel.MealPlanViewModel;

public class MealPlanPagingFragment extends Fragment {

  private static final String ARG_DATE = "date";
  private LocalDate date;
  private FragmentMealPlanPagingBinding binding;
  private MealPlanViewModel viewModel;

  // required empty constructor
  public MealPlanPagingFragment() {
  }

  public static MealPlanPagingFragment newInstance(LocalDate date) {
    MealPlanPagingFragment fragment = new MealPlanPagingFragment();
    Bundle args = new Bundle();
    args.putSerializable(ARG_DATE, date);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      date = (LocalDate) getArguments().getSerializable(ARG_DATE);
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentMealPlanPagingBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    viewModel = new ViewModelProvider(requireParentFragment(), new MealPlanViewModel
        .MealPlanViewModelFactory(requireActivity().getApplication())
    ).get(MealPlanViewModel.class);

    MealPlanEntryAdapter adapter = new MealPlanEntryAdapter(
        requireContext(),
        viewModel.getGrocyApi(),
        viewModel.getGrocyAuthHeaders(),
        date.format(viewModel.getDateFormatter())
    );
    binding.recycler.setAdapter(adapter);

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter, binding.recycler);
    ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
    touchHelper.attachToRecyclerView(binding.recycler);

    viewModel.getMealPlanEntriesLive().observe(getViewLifecycleOwner(), entries -> {
      if (entries != null) {
        String dateFormatted = date.format(viewModel.getDateFormatter());
        adapter.updateData(
            entries.get(dateFormatted),
            viewModel.getMealPlanSections(),
            viewModel.getRecipeHashMap(),
            viewModel.getProductHashMap(),
            viewModel.getQuantityUnitHashMap(),
            viewModel.getProductLastPurchasedHashMap(),
            viewModel.getRecipeResolvedFulfillmentHashMap(),
            viewModel.getStockItemHashMap(),
            viewModel.getUserFieldHashMap(),
            viewModel.getFilterChipLiveDataEntriesFields().getActiveFields()
        );
      }
    });
  }
}

