/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

@Entity(tableName = "server_table")
public class Server implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "alias")
  @SerializedName("alias")
  private String alias;

  @ColumnInfo(name = "grocy_server_url")
  @SerializedName("grocy_server_url")
  private String grocyServerUrl;

  @ColumnInfo(name = "grocy_api_key")
  @SerializedName("grocy_api_key")
  private String grocyApiKey;

  @ColumnInfo(name = "home_assistant_server_url")
  @SerializedName("home_assistant_server_url")
  private String homeAssistantServerUrl;

  @ColumnInfo(name = "home_assistant_token")
  @SerializedName("home_assistant_token")
  private String homeAssistantToken;

  public Server() {
  }  // for Room

  @Ignore
  public Server(Parcel parcel) {
    id = parcel.readInt();
    alias = parcel.readString();
    grocyServerUrl = parcel.readString();
    grocyApiKey = parcel.readString();
    homeAssistantServerUrl = parcel.readString();
    homeAssistantToken = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(alias);
    dest.writeString(grocyServerUrl);
    dest.writeString(grocyApiKey);
    dest.writeString(homeAssistantServerUrl);
    dest.writeString(homeAssistantToken);
  }

  public static final Creator<Server> CREATOR = new Creator<Server>() {

    @Override
    public Server createFromParcel(Parcel in) {
      return new Server(in);
    }

    @Override
    public Server[] newArray(int size) {
      return new Server[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getGrocyServerUrl() {
    return grocyServerUrl;
  }

  public void setGrocyServerUrl(String grocyServerUrl) {
    this.grocyServerUrl = grocyServerUrl;
  }

  public String getGrocyApiKey() {
    return grocyApiKey;
  }

  public void setGrocyApiKey(String grocyApiKey) {
    this.grocyApiKey = grocyApiKey;
  }

  public String getHomeAssistantServerUrl() {
    return homeAssistantServerUrl;
  }

  public void setHomeAssistantServerUrl(String homeAssistantServerUrl) {
    this.homeAssistantServerUrl = homeAssistantServerUrl;
  }

  public String getHomeAssistantToken() {
    return homeAssistantToken;
  }

  public void setHomeAssistantToken(String homeAssistantToken) {
    this.homeAssistantToken = homeAssistantToken;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Server server = (Server) o;
    return id == server.id && Objects.equals(alias, server.alias) && Objects
        .equals(grocyServerUrl, server.grocyServerUrl) && Objects
        .equals(grocyApiKey, server.grocyApiKey) && Objects
        .equals(homeAssistantServerUrl, server.homeAssistantServerUrl) && Objects
        .equals(homeAssistantToken, server.homeAssistantToken);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, alias, grocyServerUrl, grocyApiKey, homeAssistantServerUrl, homeAssistantToken);
  }

  @NonNull
  @Override
  public String toString() {
    return "Server(" + grocyServerUrl + ")";
  }
}
