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

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;

public class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.ViewHolder> {

    private final static String TAG = HelpAdapter.class.getSimpleName();

    public static class HelpSection {
        private String header;
        private String body;
        private int id;

        public HelpSection(String header, String body, int id) {
            this.header = header;
            this.body = body;
            this.id = id;
        }

        public String getHeader() {
            return header;
        }

        public String getBody() {
            return body;
        }

        public int getId() {
            return id;
        }
    }

    private ArrayList<HelpSection> helpSections;
    private int positionExpanded = -1;

    public HelpAdapter(ArrayList<HelpSection> helpSections) {
        this.helpSections = helpSections;
    }

    public void expandItem(int position) {
        positionExpanded = position;
        notifyItemChanged(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutContainer;
        private MaterialCardView expandableCardBody;
        private TextView textViewHeader, textViewBody;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(R.id.linear_help_container);
            expandableCardBody = view.findViewById(R.id.card_help_body);
            textViewHeader = view.findViewById(R.id.text_help_header);
            textViewBody = view.findViewById(R.id.text_expandable_card_expanded);
        }
    }

    @NonNull
    @Override
    public HelpAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HelpAdapter.ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_help,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull final HelpAdapter.ViewHolder holder,
            int position
    ) {
        HelpSection helpSection = helpSections.get(position);

        // NAME

        holder.textViewHeader.setText(helpSection.getHeader());

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
                view -> {
                    if(positionExpanded != position) {
                        if(positionExpanded != -1) {
                            notifyItemChanged(positionExpanded);
                        }
                        positionExpanded = position;
                    } else {
                        positionExpanded = -1;
                    }
                    notifyItemChanged(position);
                }
        );

        if(position == positionExpanded) {
            holder.expandableCardBody.setVisibility(View.VISIBLE);
            holder.textViewBody.setText(helpSection.getBody());
        } else {
            holder.expandableCardBody.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return helpSections.size();
    }
}
