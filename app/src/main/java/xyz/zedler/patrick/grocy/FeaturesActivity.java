package xyz.zedler.patrick.grocy;

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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import xyz.zedler.patrick.grocy.util.Constants;

public class FeaturesActivity extends AppCompatActivity {

    private final static boolean DEBUG = false;
    private final static String TAG = "FeaturesActivity";

    private SharedPreferences sharedPrefs;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;
    private FrameLayout frameLayoutPrevious, frameLayoutNext;
    private TextView textViewTitle, textViewDescription;
    private long lastClick = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean(Constants.PREF.DARK_MODE,false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        setContentView(R.layout.activity_features);

        LinearLayout linearLayoutLandTextContainer = findViewById(R.id.linear_features_text_land);
        textViewTitle = findViewById(R.id.text_features_title_land);
        textViewDescription = findViewById(R.id.text_features_description_land);
        frameLayoutPrevious = findViewById(R.id.frame_features_previous);
        frameLayoutPrevious.setOnClickListener(v -> {
            if(viewPager.getCurrentItem() > 0) {
                startAnimatedIcon(R.id.image_features_previous);
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        });
        frameLayoutNext = findViewById(R.id.frame_features_next);
        frameLayoutNext.setOnClickListener(v -> {
            if(viewPager.getCurrentItem() < 3) {
                startAnimatedIcon(R.id.image_features_next);
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });
        setArrows(0, false);

        MaterialButton buttonGetStarted = findViewById(R.id.button_features_start);
        buttonGetStarted.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            sharedPrefs.edit().putBoolean(Constants.PREF.INTRO_SHOWN, true).apply();
            finish();
        });

        viewPager = findViewById(R.id.pager_features);
        pagerAdapter = new FeaturesPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setOffset(position, position, positionOffset); // active page
                if(position != 0) setOffset(position - 1, position, positionOffset);
                if(position != pagerAdapter.getCount() -1) {
                    setOffset(position + 1, position, positionOffset);
                }
                linearLayoutLandTextContainer.setAlpha(
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

        TabLayout tabLayout = findViewById(R.id.tabs_features);
        tabLayout.setupWithViewPager(viewPager);
        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener((v, event) -> true);
        }

        setResult(RESULT_OK);
    }

    @Override
    public void onBackPressed() {
        sharedPrefs.edit().putBoolean(Constants.PREF.INTRO_SHOWN, true).apply();
        super.onBackPressed();
    }

    private void setText(int position) {
        textViewTitle.setText(getTitle(position));
        textViewDescription.setText(getDescription(position));
    }

    private int getTitle(int position) {
        int[] titles = {
                R.string.feature_1_title,
                R.string.feature_2_title,
                R.string.feature_3_title
        };
        return titles[position];
    }

    private int getDescription(int position) {
        int[] descriptions = {
                R.string.feature_1_description,
                R.string.feature_2_description,
                R.string.feature_3_description
        };
        return descriptions[position];
    }

    private void setArrows(int position, boolean animated) {
        if(animated) {
            frameLayoutPrevious.animate().alpha(
                    position > 0 ? 1 : 0
            ).setDuration(200).start();
            frameLayoutNext.animate().alpha(
                    position < 2 ? 1 : 0
            ).setDuration(200).start();
        } else {
            frameLayoutPrevious.setAlpha(position > 0 ? 1 : 0);
            frameLayoutNext.setAlpha(position < 2 ? 1 : 0);
        }
        frameLayoutPrevious.setEnabled(position > 0);
        frameLayoutNext.setEnabled(position < 2);
    }

    private void setOffset(int targetPos, int scrollPos, float offset) {
        ((FeaturesPageFragment) pagerAdapter.instantiateItem(viewPager, targetPos)).setOffset(
                scrollPos,
                offset
        );
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        try {
            ((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(ImageView) requires AVD!");
        }
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
        private ImageView imageViewBack, imageViewFront;
        TextView textViewTitle, textViewDescription;
        private int position = 0;

        @Override
        public View onCreateView(
                LayoutInflater inflater,
                ViewGroup container,
                Bundle savedInstanceState
        ) {
            return inflater.inflate(R.layout.fragment_features_page, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            ImageView imageViewFocused = view.findViewById(R.id.image_features_focused);
            imageViewBack = view.findViewById(R.id.image_features_back);
            imageViewFront = view.findViewById(R.id.image_features_front);
            textViewTitle = view.findViewById(R.id.text_features_title);
            textViewDescription = view.findViewById(R.id.text_features_description);

            if(getArguments() != null) position = getArguments().getInt("position");

            switch (position) {
                case 1:
                    imageViewBack.setImageResource(R.drawable.feature_2_b);
                    imageViewFocused.setImageResource(R.drawable.feature_2_m);
                    imageViewFront.setImageResource(R.drawable.feature_2_f);
                    break;
                case 2:
                    imageViewBack.setImageResource(R.drawable.feature_3_b);
                    imageViewFocused.setImageResource(R.drawable.feature_3_m);
                    imageViewFront.setImageResource(R.drawable.feature_3_f);
                    break;
                default:
                    imageViewBack.setImageResource(R.drawable.feature_1_b);
                    imageViewFocused.setImageResource(R.drawable.feature_1_m);
                    imageViewFront.setImageResource(R.drawable.feature_1_f);
            }

            FeaturesActivity activity = (FeaturesActivity) getActivity();
            if(activity != null)  {
                textViewTitle.setText(activity.getTitle(position));
                textViewDescription.setText(activity.getDescription(position));
            }
        }

        public void setOffset(int position, float offset) {
            if(imageViewFront == null || imageViewBack == null) return;
            int frontOffset = 200, backOffset = -200;
            int titleOffset = 100, descriptionOffset = -100;
            imageViewFront.setTranslationX(
                    position == this.position
                            ? offset * (frontOffset * -1)
                            : (1 - offset) * frontOffset
            );
            imageViewBack.setTranslationX(
                    position == this.position
                            ? offset * (backOffset * -1)
                            : (1 - offset) * backOffset
            );
            /*textViewTitle.setTranslationX(
                    position == this.position
                            ? offset * (titleOffset * -1)
                            : (1 - offset) * titleOffset
            );
            textViewDescription.setTranslationX(
                    position == this.position
                            ? offset * (descriptionOffset * -1)
                            : (1 - offset) * descriptionOffset
            );*/
        }
    }
}
