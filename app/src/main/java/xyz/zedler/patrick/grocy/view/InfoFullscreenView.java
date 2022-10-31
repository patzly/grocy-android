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

package xyz.zedler.patrick.grocy.view;

import static xyz.zedler.patrick.grocy.model.InfoFullscreen.ERROR_NETWORK;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.ERROR_OFFLINE;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.ERROR_UNSPECIFIED;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_CHORES;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_INGREDIENTS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_LOCATIONS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_PRODUCTS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_PRODUCT_BARCODES;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_PRODUCT_GROUPS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_QUS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_SHOPPING_LIST;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_STOCK;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_STORES;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_TASKS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_TASK_CATEGORIES;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_UNIT_CONVERSIONS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_EMPTY_RECIPES;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_NO_FILTER_RESULTS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.INFO_NO_SEARCH_RESULTS;
import static xyz.zedler.patrick.grocy.model.InfoFullscreen.OnRetryButtonClickListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.PartialInfoFullscreenBinding;

public class InfoFullscreenView extends RelativeLayout {

  private boolean inForeground;
  private int type;
  private String exact;

  public InfoFullscreenView(@NonNull Context context) {
    super(context);
  }

  public InfoFullscreenView(
      @NonNull Context context,
      int type,
      @Nullable String exact,
      @Nullable OnRetryButtonClickListener clickListener
  ) {
    super(context);
    PartialInfoFullscreenBinding binding = PartialInfoFullscreenBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    );

    this.type = type;
    this.exact = exact;

    @DrawableRes int picture = -1;
    @StringRes int title = -1;
    @StringRes int subtitle = -1;

    switch (type) {
      case ERROR_UNSPECIFIED:
        picture = R.drawable.illustration_popsicle;
        title = R.string.error_unknown;
        subtitle = R.string.error_undefined;
        inForeground = true;
        break;
      case ERROR_OFFLINE:
        picture = R.drawable.illustration_broccoli;
        title = R.string.error_offline;
        subtitle = R.string.error_offline_subtitle;
        inForeground = true;
        break;
      case ERROR_NETWORK:
        picture = R.drawable.illustration_broccoli;
        title = R.string.error_network;
        subtitle = R.string.error_network_subtitle;
        inForeground = true;
        break;
      case INFO_NO_SEARCH_RESULTS:
        picture = R.drawable.illustration_jar;
        title = R.string.error_search;
        subtitle = R.string.error_search_sub;
        inForeground = false;
        break;
      case INFO_NO_FILTER_RESULTS:
        picture = R.drawable.illustration_coffee;
        title = R.string.error_filter;
        subtitle = R.string.error_filter_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_STOCK:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_stock;
        subtitle = R.string.error_empty_stock_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_SHOPPING_LIST:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_shopping_list;
        subtitle = R.string.error_empty_shopping_list_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_PRODUCTS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_products;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_QUS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_qu;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_STORES:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_stores;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_PRODUCT_GROUPS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_product_groups;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_LOCATIONS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_locations;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_PRODUCT_BARCODES:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_product_barcodes;
        subtitle = R.string.error_empty_barcodes_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_TASKS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_tasks;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_TASK_CATEGORIES:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_task_categories;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_UNIT_CONVERSIONS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_unit_conversions;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_RECIPES:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_recipes;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_INGREDIENTS:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_ingredients;
        subtitle = R.string.error_empty_master_data_sub;
        inForeground = false;
        break;
      case INFO_EMPTY_CHORES:
        picture = R.drawable.illustration_toast;
        title = R.string.error_empty_chores;
        subtitle = R.string.error_empty_chores_sub;
        inForeground = false;
        break;
    }

    if (picture != -1) {
      binding.image.setImageResource(picture);
      binding.image.setVisibility(View.VISIBLE);
    } else {
      binding.image.setVisibility(View.GONE);
    }
    if (title != -1) {
      binding.title.setText(title);
      binding.title.setVisibility(View.VISIBLE);
    } else {
      binding.title.setVisibility(View.GONE);
    }
    if (subtitle != -1) {
      binding.subtitle.setText(subtitle);
      binding.subtitle.setVisibility(View.VISIBLE);
    } else {
      binding.subtitle.setVisibility(View.GONE);
    }
    if (exact != null) {
      binding.exact.setText(exact);
      binding.exact.setVisibility(View.VISIBLE);
    } else {
      binding.exact.setVisibility(View.GONE);
    }
    if (clickListener != null) {
      binding.retry.setOnClickListener(v -> clickListener.onClicked());
    }
    binding.retry.setVisibility(clickListener != null ? View.VISIBLE : View.GONE);
  }

  public boolean isInForeground() {
    return inForeground;
  }

  public int getType() {
    return type;
  }

  @Nullable
  public String getExact() {
    return exact;
  }
}
