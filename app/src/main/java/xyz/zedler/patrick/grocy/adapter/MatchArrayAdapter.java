package xyz.zedler.patrick.grocy.adapter;

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

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MatchArrayAdapter extends ArrayAdapter<String> {

    private final List<String> items;
    private final List<String> itemsAll;
    private final List<String> suggestions;

    public MatchArrayAdapter(@NonNull Context context, List<String> items) {
        super(context, android.R.layout.simple_list_item_1, items);

        this.items = items;
        this.itemsAll = new ArrayList<>(items);
        this.suggestions = new ArrayList<>();
    }

    @NonNull
    public View getView(int position, View v, @NonNull ViewGroup parent) {
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
            );
            assert inflater != null;
            v = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }
        String item = items.get(position);
        if (item != null) {
            TextView textViewItem = v.findViewById(android.R.id.text1);
            if (textViewItem != null) {
                textViewItem.setText(Html.fromHtml(item));
            }
        }
        return v;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return createFilter();
    }

    private Filter createFilter() {
        return new Filter() {
            public String convertResultToString(Object resultValue) {
                return (String) resultValue;
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint != null) {
                    suggestions.clear();
                    for (String item : itemsAll) {
                        String match = constraint.toString().toLowerCase();
                        if (item.toLowerCase().contains(match)) {
                            suggestions.add(item);
                        }
                    }
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
                @SuppressWarnings("unchecked")
                ArrayList<String> filteredList = (ArrayList<String>) results.values;
                if (results.count > 0) {
                    clear();
                    for (String item : filteredList) {
                        add(item);
                    }
                    notifyDataSetChanged();
                }
            }
        };
    }
}
