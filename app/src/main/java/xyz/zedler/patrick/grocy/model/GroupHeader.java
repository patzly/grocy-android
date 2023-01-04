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

package xyz.zedler.patrick.grocy.model;

import java.util.Objects;

public class GroupHeader extends GroupedListItem {

  private final String groupName;
  private int displayDivider = 0;

  public GroupHeader(String groupName) {
    this.groupName = groupName;
  }

  public String getGroupName() {
    return groupName;
  }

  public int getDisplayDivider() {
    return displayDivider;
  }

  public void setDisplayDivider(int display) {
    displayDivider = display;
  }

  public void setDisplayDivider(boolean display) {
    displayDivider = display ? 1 : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupHeader that = (GroupHeader) o;
    return displayDivider == that.displayDivider && Objects
        .equals(groupName, that.groupName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupName, displayDivider);
  }
}
