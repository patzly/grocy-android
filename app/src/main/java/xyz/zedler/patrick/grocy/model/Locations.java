package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Locations implements Parcelable {

    private List<Location> locations;

    public Locations(List<Location> locations) {
        this.locations = locations;
    }

    public Locations(Parcel parcel) {
        locations = new ArrayList<>();
        parcel.readList(locations, Location.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(locations);
    }

    public static final Creator<Locations> CREATOR = new Creator<Locations>() {

        @Override
        public Locations createFromParcel(Parcel in) {
            return new Locations(in);
        }

        @Override
        public Locations[] newArray(int size) {
            return new Locations[size];
        }
    };

    public List<Location> getLocations() {
        return locations;
    }

    public Location get(int index) {
        return locations.get(index);
    }

    public int size() {
        return locations.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
