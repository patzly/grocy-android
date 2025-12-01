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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.time.LocalDate;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MealPlanEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.MealPlanEntryAdapter.MealPlanEntryAdapterListener;
import xyz.zedler.patrick.grocy.adapter.MealPlanEntryAdapter.SimpleItemTouchHelperCallback;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentMealPlanPagingBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.viewmodel.MealPlanViewModel;

public class MealPlanPagingFragment extends Fragment implements MealPlanEntryAdapterListener {

  private static final String TAG = MealPlanPagingFragment.class.getSimpleName();
  private static final String ARG_DATE = "date";
  private LocalDate date;
  private FragmentMealPlanPagingBinding binding;
  private MealPlanViewModel viewModel;
  private MainActivity activity;
  private DownloadHelper dlHelper;

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
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(requireParentFragment(), new MealPlanViewModel
        .MealPlanViewModelFactory(requireActivity().getApplication())
    ).get(MealPlanViewModel.class);

    dlHelper = new DownloadHelper(activity, TAG);

    MealPlanEntryAdapter adapter = new MealPlanEntryAdapter(
        requireContext(),
        viewModel.getGrocyApi(),
        viewModel.getGrocyAuthHeaders(),
        date.format(viewModel.getDateFormatter()),
        this
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

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (dlHelper != null) {
      dlHelper.destroy();
    }
  }

  @Override
  public void onDeleteMealPlanEntry(MealPlanEntry entry) {
    // Get display name for the entry
    String displayName = getEntryDisplayName(entry);

    // Show confirmation dialog
    new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            activity.getString(
                R.string.msg_master_delete,
                getString(R.string.property_meal_plan_entry),
                displayName
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          // Perform the delete operation
          dlHelper.delete(
              viewModel.getGrocyApi().getObject(GrocyApi.ENTITY.MEAL_PLAN, entry.getId()),
              response -> {
                activity.showSnackbar(R.string.msg_deleted, false);
                viewModel.downloadData(false);
              },
              error -> activity.showSnackbar(R.string.error_undefined, false)
          );
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> {})
        .create()
        .show();
  }

  private String getEntryDisplayName(MealPlanEntry entry) {
    if (entry == null) {
      return "";
    }

    if (MealPlanEntry.TYPE_RECIPE.equals(entry.getType())) {
      try {
        int recipeId = Integer.parseInt(entry.getRecipeId());
        Recipe recipe = viewModel.getRecipeHashMap().get(recipeId);
        if (recipe != null) {
          return recipe.getName();
        }
      } catch (NumberFormatException e) {
        // Ignore and return default
      }
      return getString(R.string.property_recipe);
    } else if (MealPlanEntry.TYPE_PRODUCT.equals(entry.getType())) {
      try {
        int productId = Integer.parseInt(entry.getProductId());
        Product product = viewModel.getProductHashMap().get(productId);
        if (product != null) {
          return product.getName();
        }
      } catch (NumberFormatException e) {
        // Ignore and return default
      }
      return getString(R.string.property_product);
    } else if (MealPlanEntry.TYPE_NOTE.equals(entry.getType())) {
      if (entry.getNote() != null && !entry.getNote().isEmpty()) {
        return entry.getNote().length() > 30
            ? entry.getNote().substring(0, 30) + "..."
            : entry.getNote();
      }
      return getString(R.string.property_note);
    }

    return getString(R.string.property_meal_plan_entry);
  }
}

