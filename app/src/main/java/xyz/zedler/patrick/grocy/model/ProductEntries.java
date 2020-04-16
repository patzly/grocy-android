package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ProductEntries implements Parcelable {

    private List<ProductEntry> productEntries;

    public ProductEntries(List<ProductEntry> productEntries) {
        this.productEntries = productEntries;
    }

    public ProductEntries(Parcel parcel) {
        productEntries = new ArrayList<>();
        parcel.readList(productEntries, ProductEntry.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(productEntries);
    }

    public static final Creator<ProductEntries> CREATOR = new Creator<ProductEntries>() {

        @Override
        public ProductEntries createFromParcel(Parcel in) {
            return new ProductEntries(in);
        }

        @Override
        public ProductEntries[] newArray(int size) {
            return new ProductEntries[size];
        }
    };

    public List<ProductEntry> getProductEntries() {
        return productEntries;
    }

    public ProductEntry get(int index) {
        return productEntries.get(index);
    }

    public int size() {
        return productEntries.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
