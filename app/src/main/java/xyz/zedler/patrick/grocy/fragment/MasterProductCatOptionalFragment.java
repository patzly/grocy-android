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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductCatOptionalBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductCatOptionalViewModel;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class MasterProductCatOptionalFragment extends BaseFragment implements BarcodeListener {

  private final static String TAG = MasterProductCatOptionalFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductCatOptionalBinding binding;
  private MasterProductCatOptionalViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;
  private ActivityResultLauncher<Intent> mActivityResultLauncherTakePicture;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductCatOptionalBinding.inflate(
        inflater, container, false
    );
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    MasterProductCatOptionalFragmentArgs args = MasterProductCatOptionalFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductCatOptionalViewModel
        .MasterProductCatOptionalViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductCatOptionalViewModel.class);
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeMasterProductSimple);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> {
      onBackPressed();
      activity.navUtil.navigateUp();
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.SET_SHOPPING_LIST_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.SELECTED_ID);
        setForDestination(R.id.shoppingListFragment, Constants.ARGUMENT.SELECTED_ID, id);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    Object descriptionEdited = getFromThisDestinationNow(ARGUMENT.DESCRIPTION);
    if (descriptionEdited != null) {
      removeForThisDestination(ARGUMENT.DESCRIPTION);
      viewModel.getFormData().getDescriptionLive().setValue((String) descriptionEdited);
      viewModel.getFormData().getDescriptionSpannedLive()
          .setValue(Html.fromHtml((String) descriptionEdited));
    }

    Object newProductGroupId = getFromThisDestinationNow(ARGUMENT.OBJECT_ID);
    if (newProductGroupId != null) {  // if user created a new product group and navigates back to this fragment this is the new productGroupId
      removeForThisDestination(ARGUMENT.OBJECT_ID);
      viewModel.setQueueEmptyAction(() -> {
        List<ProductGroup> groups = viewModel.getFormData().getProductGroupsLive().getValue();
        if (groups == null) return;
        ProductGroup productGroup = ProductGroup.getFromId(groups, (Integer) newProductGroupId);
        selectProductGroup(productGroup);
      });
    }

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    embeddedFragmentScanner.setScannerVisibilityLive(
        viewModel.getFormData().getScannerVisibilityLive()
    );

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getFormData().getPictureFilenameLive().observe(getViewLifecycleOwner(),
        this::loadProductPicture);

    mActivityResultLauncherTakePicture = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == Activity.RESULT_OK) {
            viewModel.scaleAndUploadBitmap(viewModel.getCurrentFilePath(), null);
          }
        });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        viewModel.isActionEdit()
            ? R.menu.menu_master_product_edit
            : R.menu.menu_master_product_create,
        menuItem -> {
          if (menuItem.getItemId() == R.id.action_delete) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                Constants.ACTION.DELETE
            );
            activity.onBackPressed();
            return true;
          }
          if (menuItem.getItemId() == R.id.action_save) {
            setForDestination(
                R.id.masterProductFragment,
                Constants.ARGUMENT.ACTION,
                ACTION.SAVE_CLOSE
            );
            activity.onBackPressed();
            return true;
          }
          return false;
        }
    );
    boolean showSaveWithCloseButton = viewModel.isActionEdit() || args.getForceSaveWithClose();
    activity.updateFab(
        showSaveWithCloseButton ? R.drawable.ic_round_save : R.drawable.ic_round_save_as,
        showSaveWithCloseButton ? R.string.action_save : R.string.action_save_not_close,
        showSaveWithCloseButton ? Constants.FAB.TAG.SAVE : Constants.FAB.TAG.SAVE_NOT_CLOSE,
        savedInstanceState == null,
        () -> {
          setForDestination(
              R.id.masterProductFragment,
              Constants.ARGUMENT.ACTION,
              showSaveWithCloseButton ? ACTION.SAVE_CLOSE : ACTION.SAVE_NOT_CLOSE
          );
          activity.onBackPressed();
        }
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    clearInputFocus();
    viewModel.getFormData().toggleScannerVisibility();
    viewModel.onBarcodeRecognized(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.textInputParentProduct.clearFocus();
    binding.energy.clearFocus();
  }

  public void onItemAutoCompleteClick(AdapterView<?> adapterView, int pos) {
    Product product = (Product) adapterView.getItemAtPosition(pos);
    viewModel.getFormData().getProductLive().setValue(product);
    clearInputFocus();
  }

  public void onParentProductInputFocusChanged(boolean hasFocus) {
    if (hasFocus) {
      return;
    }
    viewModel.getFormData().isParentProductValid();
  }

  public void navigateToHtmlEditor() {
    if (viewModel.getFormData().getDescriptionLive().getValue() != null) {
      activity.navUtil.navigateFragment(
          MasterProductCatOptionalFragmentDirections
              .actionMasterProductCatOptionalFragmentToEditorHtmlFragment()
              .setText(viewModel.getFormData().getDescriptionLive().getValue())
      );
    } else {
      activity.navUtil.navigateFragment(
          MasterProductCatOptionalFragmentDirections
              .actionMasterProductCatOptionalFragmentToEditorHtmlFragment()
      );
    }
  }

  @Override
  public void saveText(Spanned text) {
    viewModel.getFormData().getDescriptionSpannedLive().setValue(text);
  }

  public void showProductGroupsBottomSheet() {
    Bundle bundle = new Bundle();
    List<ProductGroup> productGroups = viewModel.getFormData()
        .getProductGroupsLive().getValue();
    bundle.putParcelableArrayList(
        Constants.ARGUMENT.PRODUCT_GROUPS,
        productGroups != null ? new ArrayList<>(productGroups) : null
    );
    bundle.putBoolean(ARGUMENT.DISPLAY_EMPTY_OPTION, true);
    bundle.putBoolean(ARGUMENT.DISPLAY_NEW_OPTION, true);

    ProductGroup productGroup = viewModel.getFormData().getProductGroupLive().getValue();
    int productGroupId = productGroup != null ? productGroup.getId() : -1;
    bundle.putInt(Constants.ARGUMENT.SELECTED_ID, productGroupId);
    activity.showBottomSheet(new ProductGroupsBottomSheet(), bundle);
  }

  @Override
  public void createProductGroup() {
    activity.navUtil.navigateFragment(MasterProductCatOptionalFragmentDirections
        .actionMasterProductCatOptionalFragmentToMasterProductGroupFragment());
  }
  @Override
  public void selectProductGroup(ProductGroup productGroup) {
    viewModel.getFormData().getProductGroupLive().setValue(
        productGroup == null || productGroup.getId() == -1 ? null : productGroup
    );
  }

  private void loadProductPicture(String filename) {
    if (filename != null && !filename.isBlank()) {
      GrocyApi grocyApi = new GrocyApi(activity.getApplication());
      PictureUtil.loadPicture(
          binding.picture,
          null,
          null,
          grocyApi.getProductPictureServeLarge(filename),
          RequestHeaders.getGlideGrocyAuthHeaders(requireContext()),
          true
      );
    } else {
      binding.picture.setVisibility(View.GONE);
    }
  }

  public void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Create the File where the photo should go
    File photoFile = null;
    try {
      photoFile = viewModel.createImageFile();
    } catch (IOException ex) {
      viewModel.showErrorMessage();
      viewModel.setCurrentFilePath(null);
    }
    if (photoFile != null) {
      Uri photoURI = FileProvider.getUriForFile(requireContext(),
          "xyz.zedler.patrick.grocy.fileprovider",
          photoFile);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
      mActivityResultLauncherTakePicture.launch(takePictureIntent);
    }
  }

  @Override
  public boolean onBackPressed() {
    setForDestination(
        R.id.masterProductFragment,
        Constants.ARGUMENT.PRODUCT,
        viewModel.getFilledProduct()
    );
    return false;
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
