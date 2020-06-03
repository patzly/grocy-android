package xyz.zedler.patrick.grocy.model;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "shopping_list_item_table")
public class ShoppingListItem extends GroupedListItem implements Parcelable {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    private int id;

    @ColumnInfo(name = "note")
    @SerializedName("note")
    private String note;

    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    private double amount;

    @ColumnInfo(name = "shopping_list_id")
    @SerializedName("shopping_list_id")
    private int shoppingListId;

    @ColumnInfo(name = "done")
    @SerializedName("done")
    private int done;

    @ColumnInfo(name = "product_id")
    @SerializedName("product_id")
    private String productId;

    @ColumnInfo(name = "product_name")
    private String productName;

    @ColumnInfo(name = "product_description")
    private String productDescription;

    @ColumnInfo(name = "product_group_id")
    private String productGroupId;

    @ColumnInfo(name = "product_qu_id_purchase")
    private int productQuIdPurchase;

    @ColumnInfo(name = "is_missing")
    private int isMissing;  // needs to be integer because of min. API level

    public ShoppingListItem() {}

    private ShoppingListItem(Parcel parcel) {
        id = parcel.readInt();
        productId = parcel.readString();
        note = parcel.readString();
        amount = parcel.readDouble();
        shoppingListId = parcel.readInt();
        done = parcel.readInt();
        productName = parcel.readString();
        productDescription = parcel.readString();
        productGroupId = parcel.readString();
        productQuIdPurchase = parcel.readInt();
        isMissing = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(productId);
        dest.writeString(note);
        dest.writeDouble(amount);
        dest.writeInt(shoppingListId);
        dest.writeInt(done);
        dest.writeString(productName);
        dest.writeString(productDescription);
        dest.writeString(productGroupId);
        dest.writeInt(productQuIdPurchase);
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

    public void setId(int id) {
        this.id = id;
    }

    public String getProductId() {
        if(productId != null && productId.isEmpty()) return null;
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {  // getter & setter seem useless,
        this.note = note;               // but are required by Room !!!
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getShoppingListId() {
        return shoppingListId;
    }

    public void setShoppingListId(int shoppingListId) {
        this.shoppingListId = shoppingListId;
    }

    public int getDone() {
        return done;
    }

    public boolean isUndone() {
        return getDone() != 1;
    }

    public void setDone(boolean isDone) {
        setDone(isDone ? 1 : 0);
    }

    public void setDone(int done) {
        this.done = done;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public String getProductGroupId() {
        return productGroupId;
    }

    public int getProductQuIdPurchase() {
        return productQuIdPurchase;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public void setProductGroupId(String productGroupId) {
        this.productGroupId = productGroupId;
    }

    public void setProductQuIdPurchase(int productQuIdPurchase) {
        this.productQuIdPurchase = productQuIdPurchase;
    }

    public Product getProduct() {  // only required info for actions in shopping list
        if(productId == null || productId.isEmpty()) return null;
        return new Product(
                Integer.parseInt(productId),
                productName,
                productDescription,
                productQuIdPurchase,
                productGroupId
        );
    }

    public void setProduct(Product product) {
        if(product == null) {
            return;
        }
        productName = product.getName();
        productDescription = product.getDescription();
        productQuIdPurchase = product.getQuIdPurchase();
        productGroupId = product.getProductGroupId();
    }

    public int getIsMissing() {
        return isMissing;
    }

    public boolean isMissing() {
        return getIsMissing() == 1;
    }

    public void setIsMissing(boolean isMissing) {
        setIsMissing(isMissing ? 1 : 0);
    }

    public void setIsMissing(int isMissing) {
        this.isMissing = isMissing;
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
