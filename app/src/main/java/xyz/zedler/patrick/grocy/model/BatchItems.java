package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class BatchItems implements Parcelable {

    private List<BatchItem> batchItems;

    public BatchItems(Parcel parcel) {
        batchItems = new ArrayList<>();
        parcel.readList(batchItems, BatchItem.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(batchItems);
    }

    public static final Creator<BatchItems> CREATOR = new Creator<BatchItems>() {

        @Override
        public BatchItems createFromParcel(Parcel in) {
            return new BatchItems(in);
        }

        @Override
        public BatchItems[] newArray(int size) {
            return new BatchItems[size];
        }
    };

    public List<BatchItem> getBatchItems() {
        return batchItems;
    }

    public BatchItem get(int index) {
        return batchItems.get(index);
    }

    public int size() {
        return batchItems.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
