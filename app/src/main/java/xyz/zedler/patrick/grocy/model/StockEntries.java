package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class StockEntries implements Parcelable {

    private List<StockEntry> stockEntries;

    public StockEntries(List<StockEntry> stockEntries) {
        this.stockEntries = stockEntries;
    }

    public StockEntries(Parcel parcel) {
        stockEntries = new ArrayList<>();
        parcel.readList(stockEntries, StockEntry.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(stockEntries);
    }

    public static final Creator<StockEntries> CREATOR = new Creator<StockEntries>() {

        @Override
        public StockEntries createFromParcel(Parcel in) {
            return new StockEntries(in);
        }

        @Override
        public StockEntries[] newArray(int size) {
            return new StockEntries[size];
        }
    };

    public List<StockEntry> getStockEntries() {
        return stockEntries;
    }

    public StockEntry get(int index) {
        return stockEntries.get(index);
    }

    public int size() {
        return stockEntries.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
