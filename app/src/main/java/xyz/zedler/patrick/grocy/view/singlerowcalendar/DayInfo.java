package xyz.zedler.patrick.grocy.view.singlerowcalendar;

public class DayInfo {

  private String dayOfWeek;
  private String date;

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
