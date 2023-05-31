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

package xyz.zedler.patrick.grocy.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.FAB;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeImportBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.viewmodel.RecipeImportViewModel;
import xyz.zedler.patrick.grocy.viewmodel.RecipeImportViewModel.RecipeImportViewModelFactory;

public class RecipeImportFragment extends BaseFragment {

  private final static String TAG = RecipeImportFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentRecipeImportBinding binding;
  private RecipeImportViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private ActivityResultLauncher<Intent> jsonFilePickerLauncher;
  private ActivityResultLauncher<String> requestPermissionLauncher;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentRecipeImportBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    RecipeImportFragmentArgs args = RecipeImportFragmentArgs.fromBundle(requireArguments());

    viewModel = new ViewModelProvider(this, new RecipeImportViewModelFactory(
        activity.getApplication(),
        args.toBundle()
    )).get(RecipeImportViewModel.class);
    binding.setActivity(activity);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );
    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isDownloading ->
        binding.swipe.setRefreshing(isDownloading)
    );
    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.FOCUS_INVALID_VIEWS) {
        focusNextInvalidView();
      } else if (event.getType() == Event.TRANSACTION_SUCCESS) {
        activity.navUtil.navigateFragment(RecipeImportFragmentDirections
            .actionRecipeImportFragmentToRecipeImportGeneralFragment(viewModel.getRecipeParsed()));
      }
    });

    setupActivityResultLauncher();
    setupRequestPermissionLauncher();

    if (savedInstanceState == null) {
        viewModel.loadFromDatabase(true);
    }

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_recipe_import, this::onMenuItemClick);
    activity.updateFab(
        R.drawable.ic_round_arrow_forward,
        R.string.action_import,
        FAB.TAG.IMPORT,
        savedInstanceState == null,
        () -> {
          if (!viewModel.isRecipeWebsiteValid()) {
            //focusNextInvalidView();
            //return;
            viewModel.getRecipeWebsiteLive().setValue("https://www.chefkoch.de/rezepte/2565541401553383/Topfen-Kokoskuechlein.html");
          }
          viewModel.scrapeRecipe();
        }
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.dummyFocusView.requestFocus();
    binding.textInputWebsite.clearFocus();
  }

  public void focusNextInvalidView() {
    EditText nextView = binding.editTextWebsite;

    nextView.requestFocus();
    activity.showKeyboard(nextView);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_import_from_file) {
      requestReadExternalStoragePermission();
      return true;
    }
    return false;
  }

  public void openSupportedWebsites() {
    activity.showTextBottomSheet(R.raw.recipe_websites, R.string.title_supported_websites, 0);
  }

  private void requestReadExternalStoragePermission() {
    if (Build.VERSION.SDK_INT < VERSION_CODES.Q && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
    } else {
      openFilePicker();
    }
  }

  private void setupRequestPermissionLauncher() {
    requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
      if (isGranted) {
        openFilePicker();
      } else {
        activity.showSnackbar(R.string.error_permission, false);
      }
    });
  }

  private void setupActivityResultLauncher() {
    jsonFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri jsonFileUri = result.getData().getData();

            JSONObject jsonObject = readJsonFromUri(jsonFileUri, requireContext().getContentResolver());
            if (jsonObject != null) {
              activity.showSnackbar(jsonObject.toString(), false);
            } else {
              activity.showSnackbar(R.string.error_undefined, false);
            }
          }
        });
  }

  private void openFilePicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    jsonFilePickerLauncher.launch(intent);
  }

  public static JSONObject readJsonFromUri(@NonNull Uri jsonFileUri, @NonNull ContentResolver contentResolver) {
    try (InputStream inputStream = contentResolver.openInputStream(jsonFileUri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line);
      }

      String jsonString = stringBuilder.toString();
      return new JSONObject(jsonString);

    } catch (IOException | JSONException e) {
      e.printStackTrace();
      return null;
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
