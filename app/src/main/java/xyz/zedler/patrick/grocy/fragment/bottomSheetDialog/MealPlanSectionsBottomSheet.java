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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MealPlanSectionAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MealPlanSectionsBottomSheet extends BaseBottomSheetDialogFragment
    implements MealPlanSectionAdapter.MealPlanSectionAdapterListener {

  private final static String TAG = MealPlanSectionsBottomSheet.class.getSimpleName();

  private FragmentBottomsheetListSelectionBinding binding;
  private MainActivity activity;
  private GrocyApi grocyApi;
  private DownloadHelper dlHelper;
  private ArrayList<MealPlanSection> sections;
  private MealPlanSectionAdapter adapter;

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
    grocyApi = activity.getGrocyApi();
    dlHelper = new DownloadHelper(activity, TAG);

    Bundle bundle = requireArguments();
    ArrayList<MealPlanSection> sectionsArg = bundle
        .getParcelableArrayList(ARGUMENT.MEAL_PLAN_SECTIONS);
    assert sectionsArg != null;
    sections = new ArrayList<>(sectionsArg);

    SortUtil.sortMealPlanSections(sections);

    binding.textListSelectionTitle.setText(getString(R.string.title_meal_plan_sections));

    // Setup "New" button
    binding.buttonListSelectionNew.setVisibility(View.VISIBLE);
    binding.buttonListSelectionNew.setOnClickListener(v -> showAddSectionDialog());

    // Setup RecyclerView
    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    binding.recyclerListSelection.setItemAnimator(new DefaultItemAnimator());
    adapter = new MealPlanSectionAdapter(sections, this);
    binding.recyclerListSelection.setAdapter(adapter);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (dlHelper != null) {
      dlHelper.destroy();
    }
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

  @Override
  public void onEditSection(MealPlanSection section) {
    showEditSectionDialog(section);
  }

  @Override
  public void onDeleteSection(MealPlanSection section) {
    new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            activity.getString(
                R.string.msg_master_delete,
                getString(R.string.property_meal_plan_section),
                section.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          deleteSection(section);
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> {})
        .create()
        .show();
  }

  private void showAddSectionDialog() {
    Bundle bundle = new Bundle();
    bundle.putString(ARGUMENT.TITLE, getString(R.string.title_new_meal_plan_section));
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_name));
    bundle.putString(ARGUMENT.TYPE, "add_section");

    activity.showBottomSheet(new InputBottomSheet(), bundle);
  }

  private void showEditSectionDialog(MealPlanSection section) {
    Bundle bundle = new Bundle();
    bundle.putString(ARGUMENT.TITLE, getString(R.string.title_edit_meal_plan_section));
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_name));
    bundle.putString(ARGUMENT.TEXT, section.getName());
    bundle.putString(ARGUMENT.TYPE, "edit_section");
    bundle.putInt(ARGUMENT.SECTION_ID, section.getId());

    activity.showBottomSheet(new InputBottomSheet(), bundle);
  }

  public void saveInput(String name, Bundle bundle) {
    if (name == null || name.trim().isEmpty()) {
      activity.showSnackbar(R.string.error_empty_field, false);
      return;
    }

    // Trim the name to remove leading/trailing whitespace
    name = name.trim();

    String type = bundle.getString(ARGUMENT.TYPE);
    if ("add_section".equals(type)) {
      createSection(name);
    } else if ("edit_section".equals(type)) {
      int sectionId = bundle.getInt(ARGUMENT.SECTION_ID);
      updateSection(sectionId, name);
    }
  }

  private void createSection(String name) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("name", name);
      jsonObject.put("sort_number", sections.size() + 1);
    } catch (JSONException e) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }

    dlHelper.post(
        grocyApi.getObjects(GrocyApi.ENTITY.MEAL_PLAN_SECTIONS),
        jsonObject,
        response -> {
          activity.showSnackbar(R.string.msg_saved, false);
          refreshData();
        },
        error -> activity.showSnackbar(R.string.error_undefined, false)
    );
  }

  private void updateSection(int sectionId, String name) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("name", name);
    } catch (JSONException e) {
      activity.showSnackbar(R.string.error_undefined, false);
      return;
    }

    dlHelper.put(
        grocyApi.getObject(GrocyApi.ENTITY.MEAL_PLAN_SECTIONS, sectionId),
        jsonObject,
        response -> {
          activity.showSnackbar(R.string.msg_saved, false);
          refreshData();
        },
        error -> activity.showSnackbar(R.string.error_undefined, false)
    );
  }

  private void deleteSection(MealPlanSection section) {
    dlHelper.delete(
        grocyApi.getObject(GrocyApi.ENTITY.MEAL_PLAN_SECTIONS, section.getId()),
        response -> {
          activity.showSnackbar(R.string.msg_deleted, false);
          refreshData();
        },
        error -> {
          // Show more detailed error message
          String errorMessage = error.getMessage();
          if (errorMessage != null && errorMessage.contains("FOREIGN KEY constraint failed")) {
            activity.showSnackbar(R.string.error_cannot_delete_section_in_use, true);
          } else {
            activity.showSnackbar(R.string.error_undefined, false);
          }
        }
    );
  }

  private void refreshData() {
    // Call the parent fragment to refresh its data
    if (activity.getCurrentFragment() instanceof xyz.zedler.patrick.grocy.fragment.MealPlanFragment) {
      ((xyz.zedler.patrick.grocy.fragment.MealPlanFragment) activity.getCurrentFragment())
          .refreshMealPlanData();
    }
    dismiss();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
