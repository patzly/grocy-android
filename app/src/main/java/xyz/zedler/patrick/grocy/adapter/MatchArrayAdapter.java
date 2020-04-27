package xyz.zedler.patrick.grocy.adapter;

import android.content.Context;
import android.text.Html;
import android.util.Log;
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

    private List<String> items;
    private List<String> itemsAll;
    private List<String> suggestions;

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
        return stringFilter;
    }

    Filter stringFilter = new Filter() {
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
                        Log.i("hallo", "performFiltering: " + item);
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
