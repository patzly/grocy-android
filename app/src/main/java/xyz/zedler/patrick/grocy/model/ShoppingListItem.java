package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class ShoppingListItem extends GroupedListItem implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("note")
    private String note;

    @SerializedName("amount")
    private double amount;

    @SerializedName("shopping_list_id")
    private int shoppingListId;

    @SerializedName("done")
    private int done;

    private Product product = null;

    private int isMissing;

    private ShoppingListItem(Parcel parcel) {
        id = parcel.readInt();
        productId = parcel.readInt();
        note = parcel.readString();
        amount = parcel.readDouble();
        shoppingListId = parcel.readInt();
        done = parcel.readInt();
        //product = parcel.readParcelable(Product.class.getClassLoader());
        isMissing = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(productId);
        dest.writeString(note);
        dest.writeDouble(amount);
        dest.writeInt(shoppingListId);
        dest.writeInt(done);
        //dest.writeParcelable();
        dest.writeInt(isMissing);
    }

    public static final Creator<ShoppingListItem> CREATOR = new Creator<ShoppingListItem>() {

        @Override
        public ShoppingListItem createFromParcel(Parcel in) {
            return new ShoppingListItem(in);
        }

        @Override
        public ShoppingListItem[] newArray(int size) {
            return new ShoppingListItem[size];
        }
    };

    public int getId() {
        return id;
    }

    public int getProductId() {
        return productId;
    }

    public String getNote() {
        return note;
    }

    public double getAmount() {
        return amount;
    }

    public int getShoppingListId() {
        return shoppingListId;
    }

    public boolean isUndone() {
        return done != 1;
    }

    public void setDone(boolean isDone) {
        this.done = isDone ? 1 : 0;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean isMissing() {
        return isMissing == 1;
    }

    public void setIsMissing(boolean isMissing) {
        this.isMissing = isMissing ? 1 : 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int getType() {
        return TYPE_ENTRY;
    }

    @NonNull
    @Override
    public String toString() {
        return "ShoppingListItem(" + id + ")";
    }
}
