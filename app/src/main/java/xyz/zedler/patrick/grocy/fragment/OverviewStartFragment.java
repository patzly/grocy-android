package xyz.zedler.patrick.grocy.fragment;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentOverviewStartBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.viewmodel.OverviewStartViewModel;

public class OverviewStartFragment extends BaseFragment {

    private final static String TAG = OverviewStartFragment.class.getSimpleName();

    private MainActivity activity;
    private FragmentOverviewStartBinding binding;
    private OverviewStartViewModel viewModel;
    private InfoFullscreenHelper infoFullscreenHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentOverviewStartBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(infoFullscreenHelper != null) {
            infoFullscreenHelper.destroyInstance();
            infoFullscreenHelper = null;
        }
        if(binding != null) {
            binding = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        viewModel = new ViewModelProvider(this).get(OverviewStartViewModel.class);
        viewModel.setOfflineLive(!activity.isOnline());
        binding.setViewModel(viewModel);
        binding.setFragment(this);
        binding.setActivity(activity);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
            binding.swipe.setRefreshing(state);
            if(!state) viewModel.setCurrentQueueLoading(null);
        });
        binding.swipe.setOnRefreshListener(() -> viewModel.downloadDataForceUpdate());
        binding.swipe.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipe.setColorSchemeColors(ContextCompat.getColor(activity, R.color.secondary));

        viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
            if(event.getType() == Event.SNACKBAR_MESSAGE) {
                activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
                        activity,
                        activity.binding.frameMainContainer
                ));
            }
        });

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);
        viewModel.getInfoFullscreenLive().observe(
                getViewLifecycleOwner(),
                infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
        );

        viewModel.getOfflineLive().observe(getViewLifecycleOwner(), this::appBarOfflineInfo);

        // for offline info in app bar
        binding.swipe.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        if(savedInstanceState == null) viewModel.loadFromDatabase(true);

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(binding.scroll);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.CENTER,
                R.menu.menu_stock,
                animated,
                () -> {}
        );
        activity.updateFab(
                R.drawable.ic_round_barcode_scan,
                R.string.action_scan,
                Constants.FAB.TAG.SCAN,
                animated,
                () -> {
                    navigate(R.id.navigation_shopping);
                }
        );
    }

    public void navigateToSettingsCatDebugging() {
        String deepLinkUri = getString(R.string.deep_link_settingsFragment);
        Bundle bundle = new SettingsFragmentArgs.Builder()
                .setShowCategory(Constants.SETTINGS.BEHAVIOR.class.getSimpleName())
                .build().toBundle();
        for(String argName : bundle.keySet()) {
            Object value = bundle.get(argName);
            deepLinkUri = deepLinkUri.replace(
                    "{" + argName + "}",
                    value != null ? value.toString() : ""
            );
        }
        navigateDeepLink(deepLinkUri);
    }

    public void openFeedbackBottomSheet() {
        activity.showBottomSheet(new FeedbackBottomSheet());
    }

    public void openHelpWebsite() {
        boolean success = NetUtil.openURL(activity, Constants.URL.HELP);
        if(!success) {
            Snackbar.make(
                    activity.binding.frameMainContainer,
                    R.string.error_no_browser,
                    Snackbar.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void updateConnectivity(boolean online) {
        if(!online == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!online);
        viewModel.downloadData();
    }

    private void appBarOfflineInfo(boolean visible) {
        boolean currentState = binding.linearOfflineError.getVisibility() == View.VISIBLE;
        if(visible == currentState) return;
        binding.linearOfflineError.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
