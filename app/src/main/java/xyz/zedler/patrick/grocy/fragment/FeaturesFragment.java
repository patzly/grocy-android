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
import xyz.zedler.patrick.grocy.databinding.FragmentFeaturesBinding;
import xyz.zedler.patrick.grocy.databinding.FragmentFeaturesPageBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class FeaturesFragment extends BaseFragment {

    private final static String TAG = FeaturesFragment.class.getSimpleName();

    private FragmentFeaturesBinding binding;
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
        binding = FragmentFeaturesBinding.inflate(inflater, container, false);
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

        binding.frameFeaturesPrevious.setOnClickListener(v -> {
            if(binding.pagerFeatures.getCurrentItem() > 0) {
                IconUtil.start(binding.imageFeaturesPrevious);
                binding.pagerFeatures.setCurrentItem(binding.pagerFeatures.getCurrentItem() - 1);
            }
        });
        binding.frameFeaturesNext.setOnClickListener(v -> {
            if(binding.pagerFeatures.getCurrentItem() < 3) {
                IconUtil.start(binding.imageFeaturesNext);
                binding.pagerFeatures.setCurrentItem(binding.pagerFeatures.getCurrentItem() + 1);
            }
        });
        setArrows(0, false);

        binding.buttonFeaturesStart.setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            activity.onBackPressed();
        });

        pagerAdapter = new FeaturesPagerAdapter(activity.getSupportFragmentManager());
        binding.pagerFeatures.setAdapter(pagerAdapter);
        binding.pagerFeatures.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setOffset(position, position, positionOffset); // active page
                if(position != 0) setOffset(position - 1, position, positionOffset);
                if(position != pagerAdapter.getCount() -1) {
                    setOffset(position + 1, position, positionOffset);
                }
                binding.linearFeaturesTextLand.setAlpha(
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

        binding.tabsFeatures.setupWithViewPager(binding.pagerFeatures);
        LinearLayout tabStrip = (LinearLayout) binding.tabsFeatures.getChildAt(0);
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
            navigate(FeaturesFragmentDirections.actionFeaturesFragmentToLoginFragment());
            return true;
        } else {
            return false;
        }
    }

    private void setText(int position) {
        binding.textFeaturesTitleLand.setText(FeaturesPageFragment.getTitle(position));
        binding.textFeaturesDescriptionLand.setText(FeaturesPageFragment.getDescription(position));
    }

    private void setArrows(int position, boolean animated) {
        if(animated) {
            binding.frameFeaturesPrevious.animate().alpha(
                    position > 0 ? 1 : 0
            ).setDuration(200).start();
            binding.frameFeaturesNext.animate().alpha(
                    position < 2 ? 1 : 0
            ).setDuration(200).start();
        } else {
            binding.frameFeaturesPrevious.setAlpha(position > 0 ? 1 : 0);
            binding.frameFeaturesNext.setAlpha(position < 2 ? 1 : 0);
        }
        binding.frameFeaturesPrevious.setEnabled(position > 0);
        binding.frameFeaturesNext.setEnabled(position < 2);
    }

    private void setOffset(int targetPos, int scrollPos, float offset) {
        ((FeaturesPageFragment) pagerAdapter.instantiateItem(binding.pagerFeatures, targetPos))
                .setOffset(scrollPos, offset);
    }

    private static class FeaturesPagerAdapter extends FragmentStatePagerAdapter {
        public FeaturesPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            FeaturesPageFragment fragment = new FeaturesPageFragment();
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    public static class FeaturesPageFragment extends Fragment {
        private FragmentFeaturesPageBinding binding;
        private int position = 0;

        @Override
        public View onCreateView(
                @NonNull LayoutInflater inflater,
                ViewGroup container,
                Bundle savedInstanceState
        ) {
            binding = FragmentFeaturesPageBinding.inflate(
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
                    binding.imageFeaturesBack.setImageResource(R.drawable.feature_2_b);
                    binding.imageFeaturesFocused.setImageResource(R.drawable.feature_2_m);
                    binding.imageFeaturesRotate.setImageDrawable(null);
                    binding.imageFeaturesFront.setImageResource(R.drawable.feature_2_f);
                    break;
                case 2:
                    binding.imageFeaturesBack.setImageResource(R.drawable.feature_3_b);
                    binding.imageFeaturesFocused.setImageResource(R.drawable.feature_3_m);
                    binding.imageFeaturesRotate.setImageDrawable(null);
                    binding.imageFeaturesFront.setImageResource(R.drawable.feature_3_f);
                    break;
                default:
                    binding.imageFeaturesBack.setImageResource(R.drawable.feature_1_b);
                    binding.imageFeaturesFocused.setImageResource(R.drawable.feature_1_m);
                    binding.imageFeaturesRotate.setImageResource(R.drawable.feature_1_r);
                    binding.imageFeaturesFront.setImageResource(R.drawable.feature_1_f);
            }

            binding.textFeaturesTitle.setText(getTitle(position));
            binding.textFeaturesDescription.setText(getDescription(position));

            binding.frameFeaturesContainer.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (getContext() == null) return;
                int orientation = getResources().getConfiguration().orientation;
                ViewGroup.LayoutParams params = binding.frameFeaturesContainer.getLayoutParams();
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    params.height = binding.frameFeaturesContainer.getWidth();
                } else {
                    params.width = binding.frameFeaturesContainer.getHeight();
                }
                binding.frameFeaturesContainer.requestLayout();
            });
        }

        public void setOffset(int position, float offset) {
            if (binding == null) return;
            if(binding.imageFeaturesFront == null || binding.imageFeaturesBack == null) return;
            int frontOffset = 200, backOffset = -200;
            int rotation = 50;
            int titleOffset = 150;
            binding.imageFeaturesFront.setTranslationX(
                    position == this.position
                            ? offset * -frontOffset
                            : (1 - offset) * frontOffset
            );
            binding.imageFeaturesBack.setTranslationX(
                    position == this.position
                            ? offset * -backOffset
                            : (1 - offset) * backOffset
            );
            binding.imageFeaturesRotate.setRotation(
                    position == this.position
                            ? offset * -rotation
                            : (1 - offset) * rotation
            );
            binding.textFeaturesTitle.setTranslationX(
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
