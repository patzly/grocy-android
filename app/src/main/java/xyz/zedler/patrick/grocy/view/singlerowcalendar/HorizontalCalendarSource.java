package xyz.zedler.patrick.grocy.view.singlerowcalendar;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

public class HorizontalCalendarSource extends PageKeyedDataSource<Long, Week> {

  private Instant now;
  private final LocalDate today;
  private final int offsetToStart;

  public HorizontalCalendarSource(Instant now, int firstDayOfWeek) {
    // firstDayOfWeek: Sunday = 0, Monday = 1 and so forth
    this.now = now;
    this.today = now.atZone(ZoneId.systemDefault()).toLocalDate();
    this.offsetToStart = today.getDayOfWeek().ordinal()-(firstDayOfWeek-1);
  }

  @Override
  public void loadInitial(@NonNull LoadInitialParams<Long> loadInitialParams,
      @NonNull LoadInitialCallback<Long, Week> loadInitialCallback) {
    Week week = new Week(offsetToStart, today.minusDays(offsetToStart));
    ArrayList<Week> list = new ArrayList<>();
    list.add(week);
    loadInitialCallback.onResult(list, -7L, 7L);
  }

  @Override
  public void loadBefore(@NonNull LoadParams<Long> loadParams,
      @NonNull LoadCallback<Long, Week> loadCallback) {
    LocalDate previousDay = today.plusDays(loadParams.key);
    Week week = new Week(-1, previousDay.minusDays(offsetToStart));
    ArrayList<Week> list = new ArrayList<>();
    list.add(week);
    loadCallback.onResult(list, loadParams.key-7);
  }

  @Override
  public void loadAfter(@NonNull LoadParams<Long> loadParams,
      @NonNull LoadCallback<Long, Week> loadCallback) {
    LocalDate nextDay = today.plusDays(loadParams.key);
    Week week = new Week(-1, nextDay.minusDays(offsetToStart));
    ArrayList<Week> list = new ArrayList<>();
    list.add(week);
    loadCallback.onResult(list, loadParams.key+7);
  }
}
