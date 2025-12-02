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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MealPlanSectionSelectionAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MealPlanSectionsSelectionBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = MealPlanSectionsSelectionBottomSheet.class.getSimpleName();

  private FragmentBottomsheetListSelectionBinding binding;
  private MainActivity activity;
  private ArrayList<MealPlanSection> sections;
  private String selectedSectionId;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetListSelectionBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) requireActivity();

    Bundle bundle = requireArguments();
    ArrayList<MealPlanSection> sectionsArg = bundle
        .getParcelableArrayList(ARGUMENT.MEAL_PLAN_SECTIONS);
    assert sectionsArg != null;
    sections = new ArrayList<>();

    // Filter out any sections with null or empty names
    for (MealPlanSection section : sectionsArg) {
      if (section.getName() != null && !section.getName().trim().isEmpty()) {
        sections.add(section);
      }
    }

    selectedSectionId = bundle.getString(ARGUMENT.SELECTED_ID);

    // Sort sections first
    SortUtil.sortMealPlanSections(sections);

    // Add "None" option at the beginning AFTER sorting
    MealPlanSection noneSection = new MealPlanSection();
    noneSection.setId(-1);
    noneSection.setName(getString(R.string.subtitle_none));
    sections.add(0, noneSection);

    binding.textListSelectionTitle.setText(getString(R.string.title_select_section));

    // Hide "New" button for selection mode
    binding.buttonListSelectionNew.setVisibility(View.GONE);

    // Setup RecyclerView
    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    binding.recyclerListSelection.setItemAnimator(new DefaultItemAnimator());

    MealPlanSectionSelectionAdapter adapter = new MealPlanSectionSelectionAdapter(
        sections,
        selectedSectionId,
        section -> {
          // Find the MealPlanFragment to notify of section selection
          androidx.fragment.app.Fragment parentFragment = getParentFragment();

          // Try to find MealPlanFragment in the fragment hierarchy
          while (parentFragment != null) {
            if (parentFragment instanceof xyz.zedler.patrick.grocy.fragment.MealPlanFragment) {
              ((xyz.zedler.patrick.grocy.fragment.MealPlanFragment) parentFragment).selectMealPlanSection(section);
              break;
            } else if (parentFragment instanceof AddRecipeToMealPlanBottomSheet) {
              ((AddRecipeToMealPlanBottomSheet) parentFragment).selectSection(section);
              break;
            }
            parentFragment = parentFragment.getParentFragment();
          }

          dismiss();
        }
    );
    binding.recyclerListSelection.setAdapter(adapter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerListSelection.setPadding(
        binding.recyclerListSelection.getPaddingLeft(),
        binding.recyclerListSelection.getPaddingTop(),
        binding.recyclerListSelection.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
