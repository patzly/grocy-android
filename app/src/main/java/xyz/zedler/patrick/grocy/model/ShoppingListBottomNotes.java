package xyz.zedler.patrick.grocy.model;

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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.text.Spanned;

import androidx.annotation.NonNull;

public class ShoppingListBottomNotes extends GroupedListItem {

    private Spanned notes;

    public ShoppingListBottomNotes(Spanned notes) {
        this.notes = notes;
    }

    public Spanned getNotes() {
        return notes;
    }

    @Override
    public int getType() {
        return TYPE_BOTTOM_NOTES;
    }

    @NonNull
    @Override
    public String toString() {
        return "ShoppingListBottomNotes(" + notes.toString() + ')';
    }
}
