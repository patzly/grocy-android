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
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.zedler.patrick.grocy.R;

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
                        //Pattern pattern = Pattern.compile("(?i)" + match);
                        Pattern pattern = Pattern.compile("[a-z]+");
                        Matcher matcher = pattern.matcher(item);
                        String color = String.format(
                                "#%06X",
                                0xFFFFFF & ContextCompat.getColor(
                                        getContext(),
                                        R.color.retro_red_dark
                                )
                        );
                        suggestions.add(
                                item.replaceAll(
                                        "(?i)" + match,
                                        "<font color='" + color + "'>"
                                                + matcher.group(0) + "</font>"
                                )
                        );
                        Log.i("hallo", "performFiltering: " + matcher.group(0));
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
