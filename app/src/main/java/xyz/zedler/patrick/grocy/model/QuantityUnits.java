package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class QuantityUnits implements Parcelable {

    private List<QuantityUnit> quantityUnits;

    public QuantityUnits(List<QuantityUnit> quantityUnits) {
        this.quantityUnits = quantityUnits;
    }

    public QuantityUnits(Parcel parcel) {
        quantityUnits = new ArrayList<>();
        parcel.readList(quantityUnits, QuantityUnit.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(quantityUnits);
    }

    public static final Creator<QuantityUnits> CREATOR = new Creator<QuantityUnits>() {

        @Override
        public QuantityUnits createFromParcel(Parcel in) {
            return new QuantityUnits(in);
        }

        @Override
        public QuantityUnits[] newArray(int size) {
            return new QuantityUnits[size];
        }
    };

    public List<QuantityUnit> getQuantityUnits() {
        return quantityUnits;
    }

    public QuantityUnit get(int index) {
        return quantityUnits.get(index);
    }

    public int size() {
        return quantityUnits.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
