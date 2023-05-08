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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import xyz.zedler.patrick.grocy.Constants.FAB;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentRecipeImportGeneralBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.RecipeParsed;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.viewmodel.RecipeImportViewModel;
import xyz.zedler.patrick.grocy.viewmodel.RecipeImportViewModel.RecipeImportViewModelFactory;

public class RecipeImportGeneralFragment extends BaseFragment {

  private final static String TAG = RecipeImportGeneralFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentRecipeImportGeneralBinding binding;
  private RecipeImportViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentRecipeImportGeneralBinding.inflate(inflater, container, false);
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
    RecipeImportGeneralFragmentArgs args = RecipeImportGeneralFragmentArgs
        .fromBundle(requireArguments());

    viewModel = new ViewModelProvider(this, new RecipeImportViewModelFactory(
        activity.getApplication(),
        null)
    ).get(RecipeImportViewModel.class);
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

     RecipeParsed recipeParsed = RecipeImportGeneralFragmentArgs
         .fromBundle(requireArguments()).getRecipeParsed();
    if (viewModel.getRecipeParsed() == null) {
      viewModel.setRecipeParsed(recipeParsed);
    }

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

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
      } else if (event.getType() == Event.LOAD_IMAGE) {
        loadImage();
      }
    });

    loadImage();

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_empty);
    activity.updateFab(
        R.drawable.ic_round_arrow_forward,
        R.string.action_import,
        FAB.TAG.IMPORT,
        savedInstanceState == null,
        () -> {
          activity.navigateFragment(RecipeImportGeneralFragmentDirections
              .actionRecipeImportGeneralFragmentToRecipeImportMappingFragment(
                  viewModel.getRecipeParsed()
              )
          );
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

  private void loadImage() {
    String imageUrl = viewModel.getRecipeParsed() != null
        && viewModel.getRecipeParsed().getImage() != null
        && !viewModel.getRecipeParsed().getImage().isBlank()
        ? viewModel.getRecipeParsed().getImage() : null;
    if (imageUrl != null) {
      binding.image.layout(0, 0, 0, 0);
      RequestBuilder<Drawable> requestBuilder = Glide.with(requireContext()).load(imageUrl);
      requestBuilder = requestBuilder
          .transform(new CenterCrop(), new RoundedCorners(UiUtil.dpToPx(requireContext(), 16)))
          .transition(DrawableTransitionOptions.withCrossFade())
          .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
      requestBuilder.listener(new RequestListener<>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model,
            Target<Drawable> target, boolean isFirstResource) {
          binding.imageContainer.setVisibility(View.GONE);
          return false;
        }
        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
            DataSource dataSource, boolean isFirstResource) {
          binding.imageContainer.setVisibility(View.VISIBLE);
          return false;
        }
      }).into(binding.image);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
