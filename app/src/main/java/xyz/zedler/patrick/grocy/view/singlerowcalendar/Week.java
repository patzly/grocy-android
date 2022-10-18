package xyz.zedler.patrick.grocy.view.singlerowcalendar;

import java.time.LocalDate;

public class Week {
  private int selectedDayOfWeek; // int which is 0-6 if weekday in this week is selected, or -1 if not
  private LocalDate startDate;

  public Week(int selectedDayOfWeek, LocalDate startDate) {
    this.selectedDayOfWeek = selectedDayOfWeek;
    this.startDate = startDate;
  }

  public int getSelectedDayOfWeek() {
    return selectedDayOfWeek;
  }

  public void setSelectedDayOfWeek(int selectedDayOfWeek) {
    this.selectedDayOfWeek = selectedDayOfWeek;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Week week = (Week) o;
    return selectedDayOfWeek == week.selectedDayOfWeek && startDate.isEqual(week.startDate);
  }

  @Override
  public int hashCode() {
    return startDate.hashCode();
  }
}
