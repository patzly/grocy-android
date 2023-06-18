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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowQuantityUnitConversionBinding;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class QuantityUnitConversionAdapter extends RecyclerView.Adapter<QuantityUnitConversionAdapter.ViewHolder> {

  private final static String TAG = QuantityUnitConversionAdapter.class.getSimpleName();

  private final PluralUtil pluralUtil;
  private final ArrayList<QuantityUnitConversion> quantityUnitConversions;
  private final QuantityUnitConversionAdapterListener listener;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final int maxDecimalPlacesAmount;

  public QuantityUnitConversionAdapter(
      Context context,
      ArrayList<QuantityUnitConversion> quantityUnitConversions,
      QuantityUnitConversionAdapterListener listener,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap
  ) {
    this.pluralUtil = new PluralUtil(context);
    this.quantityUnitConversions = new ArrayList<>(quantityUnitConversions);
    this.listener = listener;
    this.quantityUnitHashMap = quantityUnitHashMap;
    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(context).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowQuantityUnitConversionBinding binding;

    public ViewHolder(RowQuantityUnitConversionBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public QuantityUnitConversionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
      int viewType) {
    RowQuantityUnitConversionBinding binding = RowQuantityUnitConversionBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    );
    return new ViewHolder(binding);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final QuantityUnitConversionAdapter.ViewHolder holder,
      int position
  ) {
    QuantityUnitConversion conversion = quantityUnitConversions.get(holder.getAdapterPosition());
    Context context = holder.binding.getRoot().getContext();
    holder.binding.fromAmountUnit.setText(context.getString(
        R.string.subtitle_amount,
        String.valueOf(1),
        pluralUtil.getQuantityUnitPlural(quantityUnitHashMap, conversion.getFromQuId(), 1)
    ));
    holder.binding.toAmountUnit.setText(context.getString(
        R.string.subtitle_amount,
        NumUtil.trimAmount(conversion.getFactor(), maxDecimalPlacesAmount),
        pluralUtil.getQuantityUnitPlural(quantityUnitHashMap, conversion.getToQuId(), conversion.getFactor())
    ));

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked(conversion)
    );
  }

  @Override
  public int getItemCount() {
    return quantityUnitConversions.size();
  }

  public void updateData(
      ArrayList<QuantityUnitConversion> quantityUnitConversionsNew,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap
  ) {
    DiffCallback diffCallback = new DiffCallback(
        this.quantityUnitConversions,
        quantityUnitConversionsNew,
        this.quantityUnitHashMap,
        quantityUnitHashMap
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.quantityUnitConversions.clear();
    this.quantityUnitConversions.addAll(quantityUnitConversionsNew);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<QuantityUnitConversion> oldItems;
    ArrayList<QuantityUnitConversion> newItems;
    HashMap<Integer, QuantityUnit> oldQuantityUnitHashMap;
    HashMap<Integer, QuantityUnit> newQuantityUnitHashMap;

    public DiffCallback(
        ArrayList<QuantityUnitConversion> oldItems,
        ArrayList<QuantityUnitConversion> newItems,
        HashMap<Integer, QuantityUnit> oldQuantityUnitHashMap,
        HashMap<Integer, QuantityUnit> newQuantityUnitHashMap
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.oldQuantityUnitHashMap = oldQuantityUnitHashMap;
      this.newQuantityUnitHashMap = newQuantityUnitHashMap;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      QuantityUnitConversion newItem = newItems.get(newItemPos);
      QuantityUnitConversion oldItem = oldItems.get(oldItemPos);
      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }
      if (oldQuantityUnitHashMap.size() != newQuantityUnitHashMap.size()) {
        return false;
      }

      return newItem.equals(oldItem);
    }
  }

  public interface QuantityUnitConversionAdapterListener {

    void onItemRowClicked(QuantityUnitConversion quantityUnitConversion);
  }
}
