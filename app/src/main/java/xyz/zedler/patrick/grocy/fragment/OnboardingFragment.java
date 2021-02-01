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

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentOnboardingBinding;
import xyz.zedler.patrick.grocy.databinding.FragmentOnboardingPageBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class OnboardingFragment extends BaseFragment {

    private final static String TAG = OnboardingFragment.class.getSimpleName();

    private FragmentOnboardingBinding binding;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private PagerAdapter pagerAdapter;
    private final ClickUtil clickUtil = new ClickUtil();

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
            if(binding.pagerOnboarding.getCurrentItem() > 0) {
                IconUtil.start(binding.imageOnboardingPrevious);
                binding.pagerOnboarding.setCurrentItem(binding.pagerOnboarding.getCurrentItem() - 1);
            }
        });
        binding.frameOnboardingNext.setOnClickListener(v -> {
            if(binding.pagerOnboarding.getCurrentItem() < 3) {
                IconUtil.start(binding.imageOnboardingNext);
                binding.pagerOnboarding.setCurrentItem(binding.pagerOnboarding.getCurrentItem() + 1);
            }
        });
        setArrows(0, false);

        binding.buttonOnboardingStart.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            activity.onBackPressed();
        });

        pagerAdapter = new OnboardingPagerAdapter(activity.getSupportFragmentManager());
        binding.pagerOnboarding.setAdapter(pagerAdapter);
        binding.pagerOnboarding.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setOffset(position, position, positionOffset); // active page
                if(position != 0) setOffset(position - 1, position, positionOffset);
                if(position != pagerAdapter.getCount() -1) {
                    setOffset(position + 1, position, positionOffset);
                }
                binding.linearOnboardingTextLand.setAlpha(
                        positionOffset < 0.5f
                                ? 1 - 2 * positionOffset
                                : 2 * positionOffset - 1
                );
                setText(positionOffset < 0.5f ? position : position + 1);
            }

            @Override
            public void onPageSelected(int position) {
                setArrows(position, true);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        binding.tabsOnboarding.setupWithViewPager(binding.pagerOnboarding);
        LinearLayout tabStrip = (LinearLayout) binding.tabsOnboarding.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener((v, event) -> true);
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return setStatusBarColor(transit, enter, nextAnim, activity, R.color.background);
    }

    @Override
    public boolean onBackPressed() {
        if(!sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false)) {
            activity.showMessage(R.string.msg_features);
            sharedPrefs.edit().putBoolean(Constants.PREF.INTRO_SHOWN, true).apply();
        }
        if(getBackStackSize() == 2){ // TODO: Better condition
            navigate(OnboardingFragmentDirections.actionOnboardingFragmentToLoginFragment());
            return true;
        } else {
            return false;
        }
    }

    private void setText(int position) {
        binding.textOnboardingTitleLand.setText(OnboardingPageFragment.getTitle(position));
        binding.textOnboardingDescriptionLand.setText(OnboardingPageFragment.getDescription(position));
    }

    private void setArrows(int position, boolean animated) {
        if(animated) {
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
        ((OnboardingPageFragment) pagerAdapter.instantiateItem(binding.pagerOnboarding, targetPos))
                .setOffset(scrollPos, offset);
    }

    private static class OnboardingPagerAdapter extends FragmentStatePagerAdapter {
        public OnboardingPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            OnboardingPageFragment fragment = new OnboardingPageFragment();
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    public static class OnboardingPageFragment extends Fragment {
        private FragmentOnboardingPageBinding binding;
        private int position = 0;

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
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            if(getArguments() != null) position = getArguments().getInt("position");

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

            binding.frameOnboardingContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (getContext() == null) return;
                int orientation = getResources().getConfiguration().orientation;
                ViewGroup.LayoutParams params = binding.frameOnboardingContainer.getLayoutParams();
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    params.height = binding.frameOnboardingContainer.getWidth();
                } else {
                    params.width = binding.frameOnboardingContainer.getHeight();
                }
                binding.frameOnboardingContainer.requestLayout();
            });
        }

        public void setOffset(int position, float offset) {
            if (binding == null) return;
            if(binding.imageOnboardingFront == null || binding.imageOnboardingBack == null) return;
            int frontOffset = 200, backOffset = -200;
            int rotation = 50;
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
            int[] titles = {
                    R.string.feature_1_title,
                    R.string.feature_2_title,
                    R.string.feature_3_title
            };
            return titles[position];
        }

        public static int getDescription(int position) {
            int[] descriptions = {
                    R.string.feature_1_description,
                    R.string.feature_2_description,
                    R.string.feature_3_description
            };
            return descriptions[position];
        }
    }
}
