/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zedler.patrick.grocy.bottomappbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleableRes;
import androidx.appcompat.content.res.AppCompatResources;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Utility methods to resolve resources for components.
 */
@RestrictTo(LIBRARY_GROUP)
public class MaterialResources {

  private MaterialResources() {}

  /**
   * Returns the {@link ColorStateList} from the given {@link TypedArray} attributes. The resource
   * can include themeable attributes, regardless of API level.
   */
  @Nullable
  public static ColorStateList getColorStateList(
      @NonNull Context context, @NonNull TypedArray attributes, @StyleableRes int index) {
    if (attributes.hasValue(index)) {
      int resourceId = attributes.getResourceId(index, 0);
      if (resourceId != 0) {
        ColorStateList value = AppCompatResources.getColorStateList(context, resourceId);
        if (value != null) {
          return value;
        }
      }
    }

    // Reading a single color with getColorStateList() on API 15 and below doesn't always correctly
    // read the value. Instead we'll first try to read the color directly here.
    return attributes.getColorStateList(index);
  }

  /**
   * Returns the drawable object from the given attributes.
   *
   * <p>This method supports inflation of {@code <vector>} and {@code <animated-vector>} resources
   * on devices where platform support is not available.
   */
  @Nullable
  public static Drawable getDrawable(
      @NonNull Context context, @NonNull TypedArray attributes, @StyleableRes int index) {
    if (attributes.hasValue(index)) {
      int resourceId = attributes.getResourceId(index, 0);
      if (resourceId != 0) {
        Drawable value = AppCompatResources.getDrawable(context, resourceId);
        if (value != null) {
          return value;
        }
      }
    }
    return attributes.getDrawable(index);
  }
}
