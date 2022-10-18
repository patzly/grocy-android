package xyz.zedler.patrick.grocy.view.singlerowcalendar;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import java.time.Instant;

public class HorizontalCalendarFactory extends DataSource.Factory<Long, Week> {

  private final Instant now;
  private final int firstDayOfWeek;

  public HorizontalCalendarFactory(Instant now, int firstDayOfWeek) {
    this.now = now;
    this.firstDayOfWeek = firstDayOfWeek;
  }

  @NonNull
  @Override
  public DataSource<Long, Week> create() {
    return new HorizontalCalendarSource(now, firstDayOfWeek);
  }
}
