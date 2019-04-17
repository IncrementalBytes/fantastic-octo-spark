/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.frostedbytes.android.cloudycurator.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.cloudycurator.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CloudyBook implements Parcelable {

    @Exclude
    public static final String ROOT = "CloudyBooks";

    public long AddedDate;

    public List<String> Authors;

    public List<String> Categories;

    public boolean HasRead;

    public String ISBN_8;

    public String ISBN_13;

    public boolean IsOwned;

    public String LCCN;

    public String PublishedDate;

    public String Publisher;

    public String Title;

    public long UpdatedDate;

    @Exclude
    public String VolumeId;

    public CloudyBook() {

        AddedDate = 0;
        Authors = new ArrayList<>();
        Categories = new ArrayList<>();
        HasRead = false;
        ISBN_8 = BaseActivity.DEFAULT_ISBN_8;
        ISBN_13 = BaseActivity.DEFAULT_ISBN_13;
        IsOwned = false;
        LCCN = BaseActivity.DEFAULT_LCCN;
        PublishedDate = "";
        Publisher = "";
        Title = "";
        UpdatedDate = 0;
        VolumeId = BaseActivity.DEFAULT_VOLUME_ID;
    }

    public CloudyBook(CloudyBook cloudyBook) {

        AddedDate = cloudyBook.AddedDate;
        Authors = new ArrayList<>();
        Authors.addAll(cloudyBook.Authors);
        Categories = new ArrayList<>();
        Categories.addAll(cloudyBook.Categories);
        HasRead = cloudyBook.HasRead;
        ISBN_8 = cloudyBook.ISBN_8;
        ISBN_13 = cloudyBook.ISBN_13;
        IsOwned = cloudyBook.IsOwned;
        LCCN = cloudyBook.LCCN;
        PublishedDate = cloudyBook.PublishedDate;
        Publisher = cloudyBook.Publisher;
        Title = cloudyBook.Title;
        UpdatedDate = cloudyBook.UpdatedDate;
        VolumeId = cloudyBook.VolumeId;
    }

    protected CloudyBook(Parcel in) {

        AddedDate = in.readLong();
        Authors = new ArrayList<>();
        in.readList(Authors, String.class.getClassLoader());
        Categories = new ArrayList<>();
        in.readList(Categories, String.class.getClassLoader());
        HasRead = in.readInt() != 0;
        ISBN_8 = in.readString();
        ISBN_13 = in.readString();
        IsOwned = in.readInt() != 0;
        LCCN = in.readString();
        PublishedDate = in.readString();
        Publisher = in.readString();
        Title = in.readString();
        UpdatedDate = in.readLong();
        VolumeId = in.readString();
    }

    @Exclude
    public String getAuthorsDelimited() {

        StringBuilder builder = new StringBuilder();
        for (String author : Authors) {
            builder.append(author);
            builder.append(",");
        }

        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    @Exclude
    public String getCategoriesDelimited() {

        StringBuilder builder = new StringBuilder();
        for (String category : Categories) {
            builder.append(category);
            builder.append(",");
        }

        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    @Exclude
    public boolean isPartiallyEqual(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudyBook that = (CloudyBook) o;
        return (!Objects.equals(ISBN_8, BaseActivity.DEFAULT_ISBN_8) && Objects.equals(ISBN_8, that.ISBN_8)) ||
            (!Objects.equals(ISBN_13, BaseActivity.DEFAULT_ISBN_13) && Objects.equals(ISBN_13, that.ISBN_13)) ||
            (!Objects.equals(LCCN, BaseActivity.DEFAULT_LCCN) && Objects.equals(LCCN, that.LCCN)) ||
            (!Title.isEmpty() && Objects.equals(Title, that.Title)) ||
            (!Objects.equals(VolumeId, BaseActivity.DEFAULT_VOLUME_ID) && Objects.equals(VolumeId, that.VolumeId));
    }

    public static final Creator<CloudyBook> CREATOR = new Creator<CloudyBook>() {

        @Override
        public CloudyBook createFromParcel(Parcel in) {
            return new CloudyBook(in);
        }

        @Override
        public CloudyBook[] newArray(int size) {
            return new CloudyBook[size];
        }
    };

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

        CloudyBook that = (CloudyBook) o;
        return Objects.equals(ISBN_8, that.ISBN_8) &&
            Objects.equals(ISBN_13, that.ISBN_13) &&
            Objects.equals(LCCN, that.LCCN) &&
            Objects.equals(Title, that.Title) &&
            Objects.equals(VolumeId, that.VolumeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ISBN_8, ISBN_13, LCCN, Title, VolumeId);
    }

    @Override
    public String toString() {

        return String.format(
            Locale.US,
            "CloudyBook{VolumeId=%s, Title='%s', ISBN=[%s, %s], HasRead='%s', IsOwned='%s'}",
            VolumeId,
            Title,
            ISBN_8,
            ISBN_13,
            String.valueOf(HasRead),
            String.valueOf(IsOwned));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeList(Authors);
        dest.writeList(Categories);
        dest.writeInt(HasRead ? 1 : 0);
        dest.writeString(ISBN_8);
        dest.writeString(ISBN_13);
        dest.writeInt(IsOwned ? 1 : 0);
        dest.writeString(LCCN);
        dest.writeString(PublishedDate);
        dest.writeString(Publisher);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeString(VolumeId);
    }
}
