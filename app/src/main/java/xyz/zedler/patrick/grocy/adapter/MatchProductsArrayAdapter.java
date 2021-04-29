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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import xyz.zedler.patrick.grocy.model.Product;

public class MatchProductsArrayAdapter extends ArrayAdapter<Product> {

  Context context;
  int resource;
  List<Product> items, suggestions;
  HashMap<String, Product> tempItems;

  public MatchProductsArrayAdapter(Context context, int resource, List<Product> items) {
    super(context, resource, items);
    this.context = context;
    this.resource = resource;
    this.items = items;
    suggestions = new ArrayList<>();
    tempItems = new HashMap<>(); // this makes the difference.
    for (Product product : items) {
      tempItems.put(product.getName().toLowerCase(), product);
    }
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    View view = convertView;
    if (convertView == null) {
      LayoutInflater inflater = (LayoutInflater) context
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      view = inflater.inflate(resource, parent, false);
    }
    Product product = items.get(position);
    if (product != null) {
      TextView name = view.findViewById(android.R.id.text1);
      if (name != null) {
        name.setText(product.getName());
      }
    }
    return view;
  }

  @NonNull
  @Override
  public Filter getFilter() {
    return nameFilter;
  }

  /**
   * Custom Filter implementation for custom suggestions we provide.
   */
  Filter nameFilter = new Filter() {
    @Override
    public CharSequence convertResultToString(Object resultValue) {
      return ((Product) resultValue).getName();
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      if (constraint != null) {
        suggestions.clear();
        List<ExtractedResult> results = FuzzySearch.extractSorted(
            constraint.toString().toLowerCase(),
            tempItems.keySet(),
            50
        );
        for (ExtractedResult result : results) {
          suggestions.add(tempItems.get(result.getString()));
        }

        //alternative without fuzzy
                /*for (Product product : tempItems) {
                    if (product.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        suggestions.add(product);
                    }
                }*/

        FilterResults filterResults = new FilterResults();
        filterResults.values = suggestions;
        filterResults.count = suggestions.size();
        return filterResults;
      } else {
        return new FilterResults();
      }
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      List<Product> filterList = (ArrayList<Product>) results.values;
      if (results.count > 0) {
        clear();
        for (Product product : filterList) {
          add(product);
          notifyDataSetChanged();
        }
      }
    }
  };
}
