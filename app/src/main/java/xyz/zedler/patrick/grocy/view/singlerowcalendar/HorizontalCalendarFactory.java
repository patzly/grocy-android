package xyz.zedler.patrick.grocy.view.singlerowcalendar;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import java.time.Instant;
import java.time.LocalDate;

public class HorizontalCalendarFactory extends DataSource.Factory<Long, LocalDate> {

  private final Instant now;

  public HorizontalCalendarFactory(Instant now) {
    this.now = now;
  }

  @NonNull
  @Override
  public DataSource<Long, LocalDate> create() {
    return new HorizontalCalendarSource(now);
  }
}
