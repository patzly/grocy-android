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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.SchedulerSupport;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Function;
import java.util.Objects;

public class RxJavaUtil {

  @CheckReturnValue
  @NonNull
  @SchedulerSupport(SchedulerSupport.NONE)
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Single<R> zip(
      @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
      @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
      @NonNull SingleSource<? extends T5> source5, @NonNull SingleSource<? extends T6> source6,
      @NonNull SingleSource<? extends T7> source7, @NonNull SingleSource<? extends T8> source8,
      @NonNull SingleSource<? extends T9> source9, @NonNull SingleSource<? extends T10> source10,
      @NonNull Function10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? extends R> zipper
  ) {
    Objects.requireNonNull(source1, "source1 is null");
    Objects.requireNonNull(source2, "source2 is null");
    Objects.requireNonNull(source3, "source3 is null");
    Objects.requireNonNull(source4, "source4 is null");
    Objects.requireNonNull(source5, "source5 is null");
    Objects.requireNonNull(source6, "source6 is null");
    Objects.requireNonNull(source7, "source7 is null");
    Objects.requireNonNull(source8, "source8 is null");
    Objects.requireNonNull(source9, "source9 is null");
    Objects.requireNonNull(source10, "source10 is null");
    Objects.requireNonNull(zipper, "zipper is null");
    return Single.zipArray(toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8, source9, source10);
  }

  public interface Function10<@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5, @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull T10, @NonNull R> {
    /**
     * Calculate a value based on the input values.
     * @param t1 the first value
     * @param t2 the second value
     * @param t3 the third value
     * @param t4 the fourth value
     * @param t5 the fifth value
     * @param t6 the sixth value
     * @param t7 the seventh value
     * @param t8 the eighth value
     * @param t9 the ninth value
     * @param t10 the tenth value
     * @return the result value
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10) throws Throwable;
  }

  @NonNull
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Function<Object[], R> toFunction(
      @NonNull Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> f) {
    return new Array10Func<>(f);
  }

  static final class Array10Func<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> implements Function<Object[], R> {
    final Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> f;

    Array10Func(Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> f) {
      this.f = f;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R apply(Object[] a) throws Throwable {
      if (a.length != 10) {
        throw new IllegalArgumentException("Array of size 10 expected but got " + a.length);
      }
      return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7], (T9)a[8], (T10)a[9]);
    }
  }


  @CheckReturnValue
  @NonNull
  @SchedulerSupport(SchedulerSupport.NONE)
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Single<R> zip(
      @NonNull SingleSource<? extends T1> source1, @NonNull SingleSource<? extends T2> source2,
      @NonNull SingleSource<? extends T3> source3, @NonNull SingleSource<? extends T4> source4,
      @NonNull SingleSource<? extends T5> source5, @NonNull SingleSource<? extends T6> source6,
      @NonNull SingleSource<? extends T7> source7, @NonNull SingleSource<? extends T8> source8,
      @NonNull SingleSource<? extends T9> source9, @NonNull SingleSource<? extends T10> source10,
      @NonNull SingleSource<? extends T11> source11, @NonNull SingleSource<? extends T12> source12,
      @NonNull Function12<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6,
                ? super T7, ? super T8, ? super T9, ? super T10, ? super T11, ? super T12, ? extends R> zipper
  ) {
    Objects.requireNonNull(source1, "source1 is null");
    Objects.requireNonNull(source2, "source2 is null");
    Objects.requireNonNull(source3, "source3 is null");
    Objects.requireNonNull(source4, "source4 is null");
    Objects.requireNonNull(source5, "source5 is null");
    Objects.requireNonNull(source6, "source6 is null");
    Objects.requireNonNull(source7, "source7 is null");
    Objects.requireNonNull(source8, "source8 is null");
    Objects.requireNonNull(source9, "source9 is null");
    Objects.requireNonNull(source10, "source10 is null");
    Objects.requireNonNull(source11, "source11 is null");
    Objects.requireNonNull(source12, "source12 is null");
    Objects.requireNonNull(zipper, "zipper is null");
    return Single.zipArray(toFunction(zipper), source1, source2, source3, source4, source5, source6, source7, source8, source9, source10, source11, source12);
  }

  public interface Function12<@NonNull T1, @NonNull T2, @NonNull T3, @NonNull T4, @NonNull T5,
      @NonNull T6, @NonNull T7, @NonNull T8, @NonNull T9, @NonNull T10, @NonNull T11, @NonNull T12,
      @NonNull R> {
    /**
     * Calculate a value based on the input values.
     * @param t1 the first value
     * @param t2 the second value
     * @param t3 the third value
     * @param t4 the fourth value
     * @param t5 the fifth value
     * @param t6 the sixth value
     * @param t7 the seventh value
     * @param t8 the eighth value
     * @param t9 the ninth value
     * @param t10 the tenth value
     * @param t11 the tenth value
     * @param t12 the tenth value
     * @return the result value
     * @throws Throwable if the implementation wishes to throw any type of exception
     */
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11,
        T12 t12) throws Throwable;
  }

  @NonNull
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Function<Object[], R> toFunction(
      @NonNull Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> f) {
    return new Array12Func<>(f);
  }

  static final class Array12Func<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> implements Function<Object[], R> {
    final Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> f;

    Array12Func(Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> f) {
      this.f = f;
    }

    @SuppressWarnings("unchecked")
    @Override
    public R apply(Object[] a) throws Throwable {
      if (a.length != 12) {
        throw new IllegalArgumentException("Array of size 12 expected but got " + a.length);
      }
      return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7], (T9)a[8], (T10)a[9], (T11)a[10], (T12)a[11]);
    }
  }
}
