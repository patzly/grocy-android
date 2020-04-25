package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ProductGroups implements Parcelable {

    private List<ProductGroup> productGroups;

    public ProductGroups(List<ProductGroup> productGroups) {
        this.productGroups = productGroups;
    }

    public ProductGroups(Parcel parcel) {
        productGroups = new ArrayList<>();
        parcel.readList(productGroups, ProductGroup.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(productGroups);
    }

    public static final Creator<ProductGroups> CREATOR = new Creator<ProductGroups>() {

        @Override
        public ProductGroups createFromParcel(Parcel in) {
            return new ProductGroups(in);
        }

        @Override
        public ProductGroups[] newArray(int size) {
            return new ProductGroups[size];
        }
    };

    public List<ProductGroup> getProductGroups() {
        return productGroups;
    }

    public ProductGroup get(int index) {
        return productGroups.get(index);
    }

    public int size() {
        return productGroups.size();
    }

    public void add(int index, ProductGroup element) {
        productGroups.add(index, element);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
