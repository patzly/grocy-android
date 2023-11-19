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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.chip.Chip;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.ChipData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields.Field;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanEntry.ViewData;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.repository.MealPlanRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.MaterialTimelineView;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class MealPlanViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();
  public final static String[] DISPLAYED_USERFIELD_ENTITIES = { ENTITY.RECIPES, ENTITY.PRODUCTS };

  public final static String FIELD_WEEK_COSTS = "field_week_costs";
  public final static String FIELD_FULFILLMENT = "field_fulfillment";
  public final static String FIELD_ENERGY = "field_energy";
  public final static String FIELD_PRICE = "field_price";
  public final static String FIELD_PICTURE = "field_picture";
  public final static String FIELD_AMOUNT = "field_amount";
  public final static String FIELD_DAY_SUMMARY = "field_day_summary";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final MealPlanRepository repository;
  private final PluralUtil pluralUtil;
  private final DateTimeFormatter dateFormatter;
  private final DateTimeFormatter weekFormatter;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final FilterChipLiveDataFields filterChipLiveDataHeaderFields;
  private final FilterChipLiveDataFields filterChipLiveDataEntriesFields;
  private final MutableLiveData<LocalDate> selectedDateLive;
  private final MutableLiveData<String> weekCostsTextLive;
  private final MutableLiveData<HashMap<String, List<MealPlanEntry>>> mealPlanEntriesLive;

  private List<MealPlanEntry> mealPlanEntries;
  private List<MealPlanSection> mealPlanSections;
  private List<Recipe> shadowRecipes;
  private HashMap<Integer, Recipe> recipeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private HashMap<String, RecipeFulfillment> recipeResolvedFulfillmentHashMap;
  private HashMap<Integer, StockItem> stockItemHashMap;
  private HashMap<String, Userfield> userfieldHashMap;

  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;
  private final String energyUnit;
  private boolean initialScrollDone;
  private final boolean debug;

  public MealPlanViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    this.decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    this.currency = sharedPrefs.getString(PREF.CURRENCY, "");
    this.energyUnit = sharedPrefs.getString(PREF.ENERGY_UNIT, PREF.ENERGY_UNIT_DEFAULT);
    initialScrollDone = false;
    dateFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter();
    weekFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-ww").toFormatter();

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(getApplication());
    repository = new MealPlanRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    selectedDateLive = new MutableLiveData<>(LocalDate.now());
    weekCostsTextLive = new MutableLiveData<>();
    mealPlanEntriesLive = new MutableLiveData<>();
    filterChipLiveDataHeaderFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.MEAL_PLAN_HEADER_FIELDS,
        () -> {},
        new Field(FIELD_WEEK_COSTS, getString(R.string.property_week_costs), isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING))
    );
    filterChipLiveDataEntriesFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.MEAL_PLAN_ENTRIES_FIELDS,
        () -> loadFromDatabase(false),
        new Field(FIELD_DAY_SUMMARY, getString(R.string.property_day_summary), true),
        new Field(FIELD_AMOUNT, getString(R.string.property_amount), true),
        new Field(FIELD_FULFILLMENT, getString(R.string.property_requirements_fulfilled), true),
        new Field(FIELD_ENERGY, getString(R.string.property_energy_only), true),
        new Field(FIELD_PRICE, getString(R.string.property_price), true),
        new Field(FIELD_PICTURE, getString(R.string.property_picture), true)
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productLastPurchasedHashMap = ArrayUtil
          .getProductLastPurchasedHashMap(data.getProductsLastPurchased());
      shadowRecipes = ArrayUtil.getShadowRecipes(data.getRecipes());
      recipeHashMap = ArrayUtil.getRecipesHashMap(data.getRecipes());
      recipeResolvedFulfillmentHashMap = ArrayUtil.getRecipeResolvedFulfillmentForMealplanHashMap(
          ArrayUtil.getRecipeFulfillmentHashMap(data.getRecipeFulfillments()), data.getRecipes()
      );
      weekCostsTextLive.setValue(getWeekCostsText());
      stockItemHashMap = ArrayUtil.getStockItemHashMap(data.getStockItems());
      userfieldHashMap = ArrayUtil.getUserfieldHashMap(data.getUserfields());
      this.mealPlanSections = data.getMealPlanSections();
      SortUtil.sortMealPlanSections(this.mealPlanSections);
      this.mealPlanEntries = data.getMealPlanEntries();
      mealPlanEntriesLive.setValue(ArrayUtil.getMealPlanEntriesForDayHashMap(
          data.getMealPlanEntries()
      ));
      filterChipLiveDataEntriesFields.setUserfields(
          data.getUserfields(),
          DISPLAYED_USERFIELD_ENTITIES
      );

      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      return;
    }
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        },
        error -> onError(error, TAG),
        forceUpdate,
        true,
        QuantityUnit.class,
        MealPlanEntry.class,
        MealPlanSection.class,
        Recipe.class,
        RecipeFulfillment.class,
        Product.class,
        StockItem.class,
        Userfield.class
    );
  }

  private ArrayList<GroupedListItem> getGroupedListItems(
      List<MealPlanEntry> mealPlanEntries,
      List<MealPlanSection> mealPlanSections,
      boolean showDaySummary
  ) {
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    if (mealPlanEntries == null || mealPlanEntries.isEmpty()) {
      return groupedListItems;
    }
    HashMap<Integer, List<MealPlanEntry>> mealPlanEntriesGrouped = new HashMap<>();
    for (MealPlanEntry entry : mealPlanEntries) {
      int sectionId = NumUtil.isStringInt(entry.getSectionId())
          ? Integer.parseInt(entry.getSectionId()) : -1;
      List<MealPlanEntry> list = mealPlanEntriesGrouped.get(sectionId);
      if (list == null) {
        list = new ArrayList<>();
        mealPlanEntriesGrouped.put(sectionId, list);
      }
      list.add(entry);
    }
    if (showDaySummary) {
      MealPlanEntry dayInfo = new MealPlanEntry();
      dayInfo.setId(-1);
      dayInfo.setType(MealPlanEntry.TYPE_DAY_INFO);
      groupedListItems.add(dayInfo);
    }
    for (MealPlanSection section : mealPlanSections) {
      List<MealPlanEntry> list = mealPlanEntriesGrouped.get(section.getId());
      if (list == null || list.isEmpty()) {
        continue;
      }
      if (section.getName() != null && !section.getName().isBlank()) {
        if (!groupedListItems.isEmpty()) {
          GroupedListItem lastItem = groupedListItems.get(groupedListItems.size()-1);
          if (lastItem instanceof MealPlanEntry) {
            ((MealPlanEntry) lastItem).setItemPosition(groupedListItems.size() > 1
                ? MaterialTimelineView.POSITION_MIDDLE : MaterialTimelineView.POSITION_FIRST);
          }
        }
        groupedListItems.add(section);
        section.setTopItem(groupedListItems.indexOf(section) == 0);
      }

      for (MealPlanEntry entry : list) {
        groupedListItems.add(entry);
        int index = groupedListItems.indexOf(entry);
        if (index == 0) {
          entry.setItemPosition(MaterialTimelineView.POSITION_FIRST);
        } else {
          entry.setItemPosition(MaterialTimelineView.POSITION_MIDDLE);
        }
      }
    }
    GroupedListItem lastItem = groupedListItems.get(groupedListItems.size()-1);
    if (lastItem instanceof MealPlanEntry) {
      ((MealPlanEntry) lastItem).setItemPosition(groupedListItems.size() > 1
          ? MaterialTimelineView.POSITION_LAST : MaterialTimelineView.POSITION_SINGLE);
    }
    return groupedListItems;
  }

  private ViewData createViewData(MealPlanEntry entry, String date) {
    MealPlanEntry.ViewData viewData = new MealPlanEntry.ViewData();
    List<String> activeFields = filterChipLiveDataEntriesFields.getActiveFields();
    viewData.chipDataList = new ArrayList<>();
    switch (entry.getType()) {
      case MealPlanEntry.TYPE_RECIPE: {
        if (!NumUtil.isStringInt(entry.getRecipeId())) {
          viewData.name = entry.getType();
          return viewData;
        }
        Recipe recipe = recipeHashMap.get(Integer.parseInt(entry.getRecipeId()));
        if (recipe == null) {
          viewData.name = entry.getType();
          return viewData;
        }
        viewData.name = recipe.getName();

        if (activeFields.contains(MealPlanViewModel.FIELD_AMOUNT)) {
          double servings = NumUtil.isStringDouble(entry.getRecipeServings()) ? Double.parseDouble(
              entry.getRecipeServings()) : 1;
          String amount = pluralUtil
              .getQuantityString(R.plurals.msg_servings, servings, maxDecimalPlacesAmount);
          viewData.chipDataList.add(ChipData.getTextChipData(amount));
        }
        RecipeFulfillment recipeFulfillment = recipeResolvedFulfillmentHashMap
            .get(date + "#" + entry.getId());
        if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)
            && recipeFulfillment != null) {
          viewData.chipDataList.add(ChipData.getRecipeFulfillmentChipData(getApplication(), recipeFulfillment));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            && recipeFulfillment != null) {
          viewData.chipDataList.add(ChipData.getTextChipData(NumUtil.trimAmount(
              recipeFulfillment.getCalories(), maxDecimalPlacesAmount
          ) + " " + energyUnit, getString(R.string.subtitle_per_serving)));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PRICE)
            && recipeFulfillment != null) {
          viewData.chipDataList.add(ChipData.getTextChipData(getString(
              R.string.property_price_with_currency,
              NumUtil.trimPrice(recipeFulfillment.getCostsPerServing(), decimalPlacesPriceDisplay),
              currency
          ), getString(R.string.title_total_price)));
        }
        for (String activeField : activeFields) {
          if (activeField.startsWith(Userfield.NAME_PREFIX)) {
            String userfieldName = activeField.substring(
                Userfield.NAME_PREFIX.length()
            );
            Userfield userfield = userfieldHashMap.get(userfieldName);
            if (userfield == null)
              continue;
            ChipData chipUserfieldData = ChipData.getUserfieldChipData(
                getApplication(),
                userfield,
                recipe.getUserfields().get(userfieldName)
            );
            if (chipUserfieldData != null)
              viewData.chipDataList.add(chipUserfieldData);
          }
        }

        String pictureFileName = recipe.getPictureFileName();
        binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
            getResources(),
            R.drawable.ic_round_image,
            null
        ));
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)
            && pictureFileName != null && !pictureFileName.isEmpty()) {
          binding.picture.layout(0, 0, 0, 0);

          PictureUtil.loadPicture(
              binding.picture,
              null,
              binding.picturePlaceholder,
              grocyApi.getRecipePictureServeSmall(pictureFileName),
              grocyAuthHeaders,
              false
          );
        } else if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      }
      case MealPlanEntry.TYPE_NOTE:
        viewData.name = entry.getNote();
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          viewData.placeholderIcon = ResourcesCompat.getDrawable(
              getResources(),
              R.drawable.ic_round_short_text,
              null
          );
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      case MealPlanEntry.TYPE_DAY_INFO:
        viewData.name = entry.getNote();
        RecipeFulfillment recipeFulfillment = recipeResolvedFulfillmentHashMap.get(date);
        if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createRecipeFulfillmentChip(recipeFulfillment));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createTextChip(NumUtil.trimAmount(
              recipeFulfillment.getCalories(), maxDecimalPlacesAmount
          ) + " " + energyUnit));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PRICE)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createTextChip(getString(
              R.string.property_price_with_currency,
              NumUtil.trimPrice(recipeFulfillment.getCostsPerServing(), decimalPlacesPriceDisplay),
              currency
          ), getString(R.string.title_total_price)));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.title.setText(R.string.property_day_summary);
          binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
              getResources(),
              R.drawable.ic_round_summarize,
              null
          ));
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      case MealPlanEntry.TYPE_PRODUCT: {
        if (!NumUtil.isStringInt(entry.getProductId())) {
          viewData.name = entry.getType();
          return viewData;
        }
        Product product = productHashMap.get(Integer.parseInt(entry.getProductId()));
        if (product == null) {
          viewData.name = entry.getType();
          return viewData;
        }
        viewData.name = product.getName();

        if (activeFields.contains(MealPlanViewModel.FIELD_AMOUNT)) {
          double amount = NumUtil.isStringDouble(entry.getProductAmount()) ? Double.parseDouble(
              entry.getProductAmount()) : 1;
          int quId = NumUtil.isStringInt(entry.getProductQuId())
              ? Integer.parseInt(entry.getProductQuId()) : -1;
          if (quId == -1 && NumUtil.isStringInt(product.getQuIdStock())) {
            quId = Integer.parseInt(product.getQuIdStock());
          }
          QuantityUnit quantityUnit = quId != -1 ? quantityUnitHashMap.get(quId) : null;
          String amountText;
          if (quantityUnit != null) {
            amountText = getString(
                R.string.subtitle_amount,
                NumUtil.trimAmount(amount, maxDecimalPlacesAmount),
                pluralUtil.getQuantityUnitPlural(quantityUnit, amount)
            );
          } else {
            amountText = NumUtil.trimAmount(amount, maxDecimalPlacesAmount);
          }
          binding.flexboxLayout.addView(chipUtil.createTextChip(amountText));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)) {
          StockItem stockItem = stockItemHashMap.get(product.getId());
          binding.flexboxLayout.addView(chipUtil.createProductFulfillmentChip(
              stockItem != null
                  && NumUtil.isStringDouble(entry.getProductAmount())
                  && stockItem.getAmountAggregatedDouble()
                  >= Double.parseDouble(entry.getProductAmount()))
          );
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            && NumUtil.isStringDouble(product.getCalories())) {
          double calories = Double.parseDouble(product.getCalories());
          binding.flexboxLayout.addView(chipUtil.createTextChip(NumUtil.trimAmount(
              calories, maxDecimalPlacesAmount
          ) + " " + energyUnit, getString(R.string.subtitle_per_serving)));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PRICE)) {
          ProductLastPurchased p = productLastPurchasedHashMap.get(product.getId());
          if (p != null && NumUtil.isStringDouble(p.getPrice())
              && NumUtil.isStringDouble(entry.getProductAmount())) {
            binding.flexboxLayout.addView(chipUtil.createTextChip(getString(
                R.string.property_price_with_currency,
                NumUtil.trimPrice(Double.parseDouble(p.getPrice())
                    * Double.parseDouble(entry.getProductAmount()), decimalPlacesPriceDisplay),
                currency
            ), getString(R.string.title_total_price)));
          }
        }
        for (String activeField : activeFields) {
          if (activeField.startsWith(Userfield.NAME_PREFIX)) {
            String userfieldName = activeField.substring(
                Userfield.NAME_PREFIX.length()
            );
            Userfield userfield = userfieldHashMap.get(userfieldName);
            if (userfield == null)
              continue;
            Chip chipUserfield = chipUtil.createUserfieldChip(
                userfield,
                product.getUserfields().get(userfieldName)
            );
            if (chipUserfield != null)
              binding.flexboxLayout.addView(chipUserfield);
          }
        }

        String pictureFileName = product.getPictureFileName();
        binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_image,
            null
        ));
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)
            && pictureFileName != null && !pictureFileName.isEmpty()) {
          binding.picture.layout(0, 0, 0, 0);

          PictureUtil.loadPicture(
              binding.picture,
              null,
              binding.picturePlaceholder,
              grocyApi.getProductPictureServeSmall(pictureFileName),
              grocyAuthHeaders,
              false
          );
        } else if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      }
    }
    return viewData;
  }

  public DayOfWeek getFirstDayOfWeek() {
    return DateUtil.getMealPlanFirstDayOfWeek(sharedPrefs);
  }

  public MutableLiveData<LocalDate> getSelectedDateLive() {
    return selectedDateLive;
  }

  public LocalDate getSelectedDate() {
    return selectedDateLive.getValue();
  }

  public DateTimeFormatter getDateFormatter() {
    return dateFormatter;
  }

  public String getWeekCostsText() {
    if (shadowRecipes == null || !filterChipLiveDataHeaderFields.getActiveFields()
        .contains(MealPlanViewModel.FIELD_WEEK_COSTS)) {
      return getString(R.string.property_week_costs_insert, getString(R.string.subtitle_unknown));
    };
    LocalDate selectedDate = getSelectedDate();
    String weekFormatted = selectedDate.format(weekFormatter);
    RecipeFulfillment recipeFulfillment = recipeResolvedFulfillmentHashMap.get(weekFormatted);
    double costs = recipeFulfillment != null ? recipeFulfillment.getCosts() : 0;
    return getString(R.string.property_week_costs_insert, getString(
        R.string.property_price_with_currency,
        NumUtil.trimPrice(costs, decimalPlacesPriceDisplay),
        currency
    ));
  }

  public MutableLiveData<HashMap<String, List<MealPlanEntry>>> getMealPlanEntriesLive() {
    return mealPlanEntriesLive;
  }

  public List<MealPlanSection> getMealPlanSections() {
    return mealPlanSections;
  }

  public MutableLiveData<String> getWeekCostsTextLive() {
    return weekCostsTextLive;
  }

  public HashMap<Integer, Recipe> getRecipeHashMap() {
    return recipeHashMap;
  }

  public HashMap<String, RecipeFulfillment> getRecipeResolvedFulfillmentHashMap() {
    return recipeResolvedFulfillmentHashMap;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, StockItem> getStockItemHashMap() {
    return stockItemHashMap;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public HashMap<Integer, ProductLastPurchased> getProductLastPurchasedHashMap() {
    return productLastPurchasedHashMap;
  }

  public HashMap<String, Userfield> getUserFieldHashMap() {
    return userfieldHashMap;
  }

  public FilterChipLiveDataFields getFilterChipLiveDataHeaderFields() {
    return filterChipLiveDataHeaderFields;
  }

  public FilterChipLiveDataFields getFilterChipLiveDataEntriesFields() {
    return filterChipLiveDataEntriesFields;
  }

  public boolean isInitialScrollDone() {
    return initialScrollDone;
  }

  public void setInitialScrollDone(boolean initialScrollDone) {
    this.initialScrollDone = initialScrollDone;
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public GrocyApi getGrocyApi() {
    return grocyApi;
  }

  public LazyHeaders getGrocyAuthHeaders() {
    return grocyAuthHeaders;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  public String getCurrency() {
    return sharedPrefs.getString(
        PREF.CURRENCY,
        ""
    );
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MealPlanViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;

    public MealPlanViewModelFactory(Application application) {
      this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MealPlanViewModel(application);
    }
  }
}
