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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowStockLogEntryBinding;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockLogEntry;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class StockLogEntryAdapter extends
    RecyclerView.Adapter<StockLogEntryAdapter.ViewHolder> {

  private final static String TAG = StockLogEntryAdapter.class.getSimpleName();

  private final ArrayList<StockLogEntry> stockLogEntries;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, Location> locationHashMap;
  private final HashMap<Integer, User> userHashMap;
  private final PluralUtil pluralUtil;
  private final StockLogEntryAdapterListener listener;
  private final DateUtil dateUtil;
  private final String currency;
  private final int dueSoonDays;


  public StockLogEntryAdapter(
      Context context,
      ArrayList<StockLogEntry> stockLogEntries,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      HashMap<Integer, User> userHashMap,
      StockLogEntryAdapterListener listener
  ) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.currency = sharedPrefs.getString(PREF.CURRENCY, "");
    String days = sharedPrefs.getString(STOCK.DUE_SOON_DAYS, SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    if (NumUtil.isStringInt(days)) {
      this.dueSoonDays = Integer.parseInt(days);
    } else {
      this.dueSoonDays = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    }
    this.stockLogEntries = stockLogEntries;
    this.productHashMap = new HashMap<>(productHashMap);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.locationHashMap = new HashMap<>(locationHashMap);
    this.userHashMap = new HashMap<>(userHashMap);
    this.pluralUtil = new PluralUtil(context);
    this.listener = listener;
    this.dateUtil = new DateUtil(context);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class StockLogEntryViewHolder extends ViewHolder {

    private final RowStockLogEntryBinding binding;

    public StockLogEntryViewHolder(RowStockLogEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @Override
  public int getItemCount() {
    return stockLogEntries == null ? 0 : stockLogEntries.size();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new StockLogEntryViewHolder(RowStockLogEntryBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("SetTextI18n")
  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    StockLogEntry stockLogEntry = stockLogEntries.get(position);

    StockLogEntryViewHolder stockLogViewHolder = (StockLogEntryViewHolder) holder;
    Context context = stockLogViewHolder.binding.container.getContext();

    Product product = productHashMap.get(stockLogEntry.getProductId());
    if (product == null) return;
    stockLogViewHolder.binding.productName.setText(product.getName());

    if (!stockLogEntry.getUndoneBoolean()) {
      stockLogViewHolder.binding.productName.setPaintFlags(
          stockLogViewHolder.binding.productName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
      );
      stockLogViewHolder.binding.container.setAlpha(1.0f);
      stockLogViewHolder.binding.undoneTimeContainer.setVisibility(View.GONE);
    } else {
      stockLogViewHolder.binding.productName.setPaintFlags(
          stockLogViewHolder.binding.productName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
      stockLogViewHolder.binding.container.setAlpha(0.6f);
      stockLogViewHolder.binding.undoneTime.setText(context.getString(
          R.string.msg_undone_transaction_description,
          dateUtil.getLocalizedDate(
              stockLogEntry.getUndoneTimestamp(),
              DateUtil.FORMAT_SHORT_WITH_TIME
          )
      ));
      stockLogViewHolder.binding.undoneTimeHuman.setText(dateUtil.getHumanForDaysFromNow(
          stockLogEntry.getUndoneTimestamp()
      ));
      stockLogViewHolder.binding.undoneTimeContainer.setVisibility(View.VISIBLE);
    }

    if (NumUtil.isStringDouble(stockLogEntry.getAmount())) {
      stockLogViewHolder.binding.amount.setText(stockLogEntry.getAmount() + " "
          + pluralUtil.getQuantityUnitPlural(
          quantityUnitHashMap,
          product.getQuIdStockInt(),
          Math.abs(NumUtil.toDouble(stockLogEntry.getAmount()))
      ));
    } else {
      stockLogViewHolder.binding.amount.setText(stockLogEntry.getAmount());
    }

    stockLogViewHolder.binding.transactionTime.setText(dateUtil.getLocalizedDate(
        stockLogEntry.getRowCreatedTimestamp(),
        DateUtil.FORMAT_SHORT_WITH_TIME)
    );
    stockLogViewHolder.binding.transactionTimeHuman.setText(dateUtil.getHumanForDaysFromNow(
        stockLogEntry.getRowCreatedTimestamp()
    ));

    String transactionType;
    switch (stockLogEntry.getTransactionType()) {
      case "purchase":
        transactionType = context.getString(R.string.title_purchase);
        break;
      case "consume":
        transactionType = context.getString(R.string.title_consume);
        break;
      case "product-opened":
        transactionType = context.getString(R.string.action_product_opened);
        break;
      case "transfer_from":
        transactionType = context.getString(R.string.action_transfer_from);
        break;
      case "transfer_to":
        transactionType = context.getString(R.string.action_transfer_to);
        break;
      default:
        transactionType = stockLogEntry.getTransactionType();
    }
    stockLogViewHolder.binding.transactionType.setText(transactionType);

    Location location = null;
    if (NumUtil.isStringInt(stockLogEntry.getLocationId())) {
      location = locationHashMap.get(Integer.parseInt(stockLogEntry.getLocationId()));
    }
    if (location != null) {
      stockLogViewHolder.binding.location.setText(location.getName());
      stockLogViewHolder.binding.location.setVisibility(View.VISIBLE);
    } else {
      stockLogViewHolder.binding.location.setVisibility(View.GONE);
    }

    User user = null;
    if (NumUtil.isStringInt(stockLogEntry.getUserId())) {
      user = userHashMap.get(Integer.parseInt(stockLogEntry.getUserId()));
    }
    if (user != null) {
      stockLogViewHolder.binding.doneBy.setText(user.getDisplayName());
      stockLogViewHolder.binding.doneBy.setVisibility(View.VISIBLE);
    } else {
      stockLogViewHolder.binding.doneBy.setVisibility(View.GONE);
    }

    if (stockLogEntry.getNote() != null && !stockLogEntry.getNote().isEmpty()) {
      stockLogViewHolder.binding.note.setText(stockLogEntry.getNote());
      stockLogViewHolder.binding.note.setVisibility(View.VISIBLE);
    } else {
      stockLogViewHolder.binding.note.setVisibility(View.GONE);
    }

    stockLogViewHolder.binding.container.setOnClickListener(
        v -> listener.onItemRowClicked(stockLogEntry)
    );
  }

  public void add(StockLogEntry stockLogEntry) {
    stockLogEntries.add(stockLogEntry);
    notifyItemInserted(stockLogEntries.size() - 1);
  }

  public void addAll(List<StockLogEntry> results) {
    for (StockLogEntry result : results) {
      add(result);
    }
  }

  public ArrayList<StockLogEntry> getStockLogEntries() {
    return stockLogEntries;
  }

  public interface StockLogEntryAdapterListener {

    void onItemRowClicked(StockLogEntry entry);
  }

  public abstract static class PaginationScrollListener extends RecyclerView.OnScrollListener {

    private final LinearLayoutManager layoutManager;

    public PaginationScrollListener(LinearLayoutManager layoutManager) {
      this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
      super.onScrolled(recyclerView, dx, dy);

      int visibleItemCount = layoutManager.getChildCount();
      int totalItemCount = layoutManager.getItemCount();
      int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

      if (!isLoading() && !isLastPage()) {
        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
            && firstVisibleItemPosition >= 0) {
          loadMoreItems();
        }
      }
    }

    protected abstract void loadMoreItems();

    public abstract boolean isLastPage();

    public abstract boolean isLoading();

  }
}
