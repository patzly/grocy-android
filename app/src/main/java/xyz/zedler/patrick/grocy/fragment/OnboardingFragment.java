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

package xyz.zedler.patrick.grocy.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentOnboardingBinding;
import xyz.zedler.patrick.grocy.databinding.FragmentOnboardingPageBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class OnboardingFragment extends BaseFragment {

  private final static String TAG = OnboardingFragment.class.getSimpleName();

  private FragmentOnboardingBinding binding;
  private MainActivity activity;
  private SharedPreferences sharedPrefs;
  private final ClickUtil clickUtil = new ClickUtil();
  private final HashMap<Integer, OnboardingPageFragment> fragments = new HashMap<>();

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentOnboardingBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

    binding.frameOnboardingPrevious.setOnClickListener(v -> {
      if (binding.pagerOnboarding.getCurrentItem() == 0) {
        return;
      }
      ViewUtil.startIcon(binding.imageOnboardingPrevious);
      binding.pagerOnboarding.setCurrentItem(binding.pagerOnboarding.getCurrentItem() - 1);
    });
    binding.frameOnboardingNext.setOnClickListener(v -> {
      if (binding.pagerOnboarding.getCurrentItem() == 3) {
        return;
      }
      ViewUtil.startIcon(binding.imageOnboardingNext);
      binding.pagerOnboarding.setCurrentItem(binding.pagerOnboarding.getCurrentItem() + 1);
    });
    setArrows(0, false);

    binding.buttonOnboardingStart.setOnClickListener(v -> {
      if (clickUtil.isDisabled()) {
        return;
      }
      activity.onBackPressed();
    });

    binding.pagerOnboarding.setAdapter(new OnboardingPagerAdapter(this));
    binding.pagerOnboarding.setCurrentItem(0);
    binding.pagerOnboarding.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
    binding.pagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageSelected(int position) {
        setArrows(position, true);
      }

      @Override
      public void onPageScrolled(
          int position,
          float positionOffset,
          int positionOffsetPixels
      ) {
        for (int i = 0; i < 3; i++) {
          if (fragments.containsKey(i) && fragments.get(i) != null) {
            continue;
          }
          Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + i);
          if (fragment == null) {
            continue;
          }
          fragments.put(i, (OnboardingPageFragment) fragment);
          ((OnboardingPageFragment) fragment).updateLayout(false);
        }

        setOffset(position, position, positionOffset); // active page
        if (position != 0) {
          setOffset(position - 1, position, positionOffset);
        }
        if (position != binding.pagerOnboarding.getAdapter().getItemCount() - 1) {
          setOffset(position + 1, position, positionOffset);
        }
        binding.linearOnboardingTextLand.setAlpha(
            positionOffset < 0.5f
                ? 1 - 2 * positionOffset
                : 2 * positionOffset - 1
        );
        int positionNeeded = positionOffset < 0.5f ? position : position + 1;
        binding.textOnboardingTitleLand.setText(
            OnboardingPageFragment.getTitle(positionNeeded)
        );
        binding.textOnboardingDescriptionLand.setText(
            OnboardingPageFragment.getDescription(positionNeeded)
        );
      }
    });

    new TabLayoutMediator(
        binding.tabsOnboarding, binding.pagerOnboarding, (tab, position) -> {
    }
    ).attach();

    LinearLayout tabStrip = (LinearLayout) binding.tabsOnboarding.getChildAt(0);
    for (int i = 0; i < tabStrip.getChildCount(); i++) {
      tabStrip.getChildAt(i).setOnTouchListener((v, event) -> true);
    }
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.background);
  }

  @SuppressLint("RestrictedApi")
  @Override
  public boolean onBackPressed() {
    if (!sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false)) {
      activity.showMessage(R.string.msg_features);
      sharedPrefs.edit().putBoolean(Constants.PREF.INTRO_SHOWN, true).apply();
    }
    if (findNavController().getBackQueue().getSize() == 2) { // TODO: Better condition
      navigate(OnboardingFragmentDirections.actionOnboardingFragmentToNavigationLogin());
      return true;
    } else {
      return false;
    }
  }

  private void setArrows(int position, boolean animated) {
    if (animated) {
      binding.frameOnboardingPrevious.animate().alpha(
          position > 0 ? 1 : 0
      ).setDuration(200).start();
      binding.frameOnboardingNext.animate().alpha(
          position < 2 ? 1 : 0
      ).setDuration(200).start();
    } else {
      binding.frameOnboardingPrevious.setAlpha(position > 0 ? 1 : 0);
      binding.frameOnboardingNext.setAlpha(position < 2 ? 1 : 0);
    }
    binding.frameOnboardingPrevious.setEnabled(position > 0);
    binding.frameOnboardingNext.setEnabled(position < 2);
  }

  private void setOffset(int targetPos, int scrollPos, float offset) {
    if (!fragments.containsKey(targetPos)) {
      return;
    }
    OnboardingPageFragment fragment = fragments.get(targetPos);
    if (fragment != null) {
      fragment.setOffset(scrollPos, offset);
    }
  }

  private static class OnboardingPagerAdapter extends FragmentStateAdapter {

    public OnboardingPagerAdapter(Fragment fragment) {
      super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      Bundle bundle = new Bundle();
      bundle.putInt(Constants.ARGUMENT.POSITION, position);
      Fragment fragment = new OnboardingPageFragment();
      fragment.setArguments(bundle);
      return fragment;
    }

    @Override
    public int getItemCount() {
      return 3;
    }
  }

  public static class OnboardingPageFragment extends Fragment {

    private FragmentOnboardingPageBinding binding;
    private int position;

    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState
    ) {
      binding = FragmentOnboardingPageBinding.inflate(
          inflater, container, false
      );
      return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
      super.onDestroyView();
      binding = null;
    }

    @Override
    public void onResume() {
      super.onResume();
      updateLayout(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      if (getArguments() != null) {
        position = getArguments().getInt(
            Constants.ARGUMENT.POSITION
        );
      }

      switch (position) {
        case 1:
          binding.imageOnboardingBack.setImageResource(R.drawable.onboarding_2_b);
          binding.imageOnboardingFocused.setImageResource(R.drawable.onboarding_2_m);
          binding.imageOnboardingRotate.setImageDrawable(null);
          binding.imageOnboardingFront.setImageResource(R.drawable.onboarding_2_f);
          break;
        case 2:
          binding.imageOnboardingBack.setImageResource(R.drawable.onboarding_3_b);
          binding.imageOnboardingFocused.setImageResource(R.drawable.onboarding_3_m);
          binding.imageOnboardingRotate.setImageDrawable(null);
          binding.imageOnboardingFront.setImageResource(R.drawable.onboarding_3_f);
          break;
        default:
          binding.imageOnboardingBack.setImageResource(R.drawable.onboarding_1_b);
          binding.imageOnboardingFocused.setImageResource(R.drawable.onboarding_1_m);
          binding.imageOnboardingRotate.setImageResource(R.drawable.onboarding_1_r);
          binding.imageOnboardingFront.setImageResource(R.drawable.onboarding_1_f);
      }

      binding.textOnboardingTitle.setText(getTitle(position));
      binding.textOnboardingDescription.setText(getDescription(position));

      binding.frameOnboardingContainer.getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              if (binding == null) {
                return;
              }
              updateLayout(true);
              binding.frameOnboardingContainer.getViewTreeObserver()
                  .removeOnGlobalLayoutListener(this);
            }
          }
      );
    }

    public void updateLayout(boolean force) {
      if (getContext() == null) {
        return;
      }
      int orientation = getResources().getConfiguration().orientation;
      ViewGroup.LayoutParams params = binding.frameOnboardingContainer.getLayoutParams();
      if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        if (!force && params.height > 0) {
          return;
        }
        params.height = binding.frameOnboardingContainer.getWidth();
      } else {
        if (!force && params.width > 0) {
          return;
        }
        params.width = binding.frameOnboardingContainer.getHeight();
      }
      binding.frameOnboardingContainer.requestLayout();
      if (force) {
        binding.frameOnboardingContainer.forceLayout();
      }
    }

    public void setOffset(int position, float offset) {
      if (binding == null) {
        return;
      }
      if (binding.imageOnboardingFront == null || binding.imageOnboardingBack == null) {
        return;
      }
      int frontOffset = 200, backOffset = -200;
      int rotation = 100;
      int titleOffset = 150;
      binding.imageOnboardingFront.setTranslationX(
          position == this.position
              ? offset * -frontOffset
              : (1 - offset) * frontOffset
      );
      binding.imageOnboardingBack.setTranslationX(
          position == this.position
              ? offset * -backOffset
              : (1 - offset) * backOffset
      );
      binding.imageOnboardingRotate.setRotation(
          position == this.position
              ? offset * -rotation
              : (1 - offset) * rotation
      );
      binding.textOnboardingTitle.setTranslationX(
          position == this.position
              ? offset * -titleOffset
              : (1 - offset) * titleOffset
      );
    }

    public static int getTitle(int position) {
      return new int[]{
          R.string.feature_1_title, R.string.feature_2_title, R.string.feature_3_title
      }[position];
    }

    public static int getDescription(int position) {
      return new int[]{
          R.string.feature_1_description,
          R.string.feature_2_description,
          R.string.feature_3_description
      }[position];
    }
  }
}
