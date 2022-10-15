package xyz.zedler.patrick.grocy.view.singlerowcalendar;

public class DayInfo {

  private final String dayOfWeek;
  private final String date;

  public DayInfo(String dayOfWeek, String date) {
    this.dayOfWeek = dayOfWeek;
    this.date = date;
  }

  public String getDayOfWeek() {
    return dayOfWeek;
  }

  public String getDate() {
    return date;
  }
}
