package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class StockLocations implements Parcelable {

    private List<StockLocation> stockLocations;

    public StockLocations(List<StockLocation> stockLocations) {
        this.stockLocations = stockLocations;
    }

    public StockLocations(Parcel parcel) {
        stockLocations = new ArrayList<>();
        parcel.readList(stockLocations, StockLocation.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(stockLocations);
    }

    public static final Creator<StockLocations> CREATOR = new Creator<StockLocations>() {

        @Override
        public StockLocations createFromParcel(Parcel in) {
            return new StockLocations(in);
        }

        @Override
        public StockLocations[] newArray(int size) {
            return new StockLocations[size];
        }
    };

    public List<StockLocation> getStockLocations() {
        return stockLocations;
    }

    public StockLocation get(int index) {
        return stockLocations.get(index);
    }

    public int size() {
        return stockLocations.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
