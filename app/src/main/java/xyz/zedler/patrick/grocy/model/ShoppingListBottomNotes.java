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
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.text.Spanned;

import androidx.annotation.NonNull;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingListBottomNotes that = (ShoppingListBottomNotes) o;
        return Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notes);
    }

    @NonNull
    @Override
    public String toString() {
        return "ShoppingListBottomNotes(" + notes.toString() + ')';
    }
}
