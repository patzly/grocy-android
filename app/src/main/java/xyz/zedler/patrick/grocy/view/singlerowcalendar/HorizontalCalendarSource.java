package xyz.zedler.patrick.grocy.view.singlerowcalendar;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

public class HorizontalCalendarSource extends PageKeyedDataSource<Long, LocalDate> {

  private Instant now;
  private LocalDate today;

  public HorizontalCalendarSource(Instant now) {
    this.now = now;
    this.today = now.atZone(ZoneId.systemDefault()).toLocalDate();
  }

  @Override
  public void loadAfter(@NonNull LoadParams<Long> loadParams,
      @NonNull LoadCallback<Long, LocalDate> loadCallback) {
    LocalDate previousDay = today.plusDays(loadParams.key);
    ArrayList<LocalDate> list = new ArrayList<>();
    list.add(previousDay);
    loadCallback.onResult(list, loadParams.key+1);
  }

  @Override
  public void loadBefore(@NonNull LoadParams<Long> loadParams,
      @NonNull LoadCallback<Long, LocalDate> loadCallback) {
    LocalDate previousDay = today.plusDays(loadParams.key);
    ArrayList<LocalDate> list = new ArrayList<>();
    list.add(previousDay);
    loadCallback.onResult(list, loadParams.key-1);
  }

  @Override
  public void loadInitial(@NonNull LoadInitialParams<Long> loadInitialParams,
      @NonNull LoadInitialCallback<Long, LocalDate> loadInitialCallback) {
    ArrayList<LocalDate> list = new ArrayList<>();
    list.add(today);
    loadInitialCallback.onResult(list, -1L, 1L);
  }
}
