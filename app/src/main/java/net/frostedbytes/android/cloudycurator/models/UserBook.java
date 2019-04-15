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

import java.util.ArrayList;
import java.util.Objects;

public class UserBook extends Book implements Parcelable {

    @Exclude
    public static final String ROOT = "UserBooks";

    public long AddedDate;

    public boolean HasRead;

    public boolean IsOwned;

    public long UpdatedDate;

    public UserBook() {

        AddedDate = 0;
        HasRead = false;
        IsOwned = false;
        UpdatedDate = 0;
    }

    public UserBook(UserBook userBook) {

        AddedDate = userBook.AddedDate;
        Authors = new ArrayList<>();
        Authors.addAll(userBook.Authors);
        HasRead = userBook.HasRead;
        ISBN_8 = userBook.ISBN_8;
        ISBN_13 = userBook.ISBN_13;
        IsOwned = userBook.IsOwned;
        LCCN = userBook.LCCN;
        Title = userBook.Title;
        UpdatedDate = userBook.UpdatedDate;
        VolumeId = userBook.VolumeId;
    }

    protected UserBook(Parcel in) {

        AddedDate = in.readLong();
        Authors = new ArrayList<>();
        in.readList(Authors, String.class.getClassLoader());
        HasRead = in.readByte() != 0;
        ISBN_8 = in.readString();
        ISBN_13 = in.readString();
        IsOwned = in.readByte() != 0;
        LCCN = in.readString();
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

        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static final Creator<UserBook> CREATOR = new Creator<UserBook>() {

        @Override
        public UserBook createFromParcel(Parcel in) { return new UserBook(in); }

        @Override
        public UserBook[] newArray(int size) { return new UserBook[size]; }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        UserBook userBook = (UserBook) o;
        return VolumeId.equals(userBook.VolumeId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), VolumeId);
    }

    @Override
    public String toString() {

        return "UserBook{" +
            "HasRead=" + HasRead +
            ", IsOwned=" + IsOwned +
            ", Title='" + Title + '\'' +
            '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeList(Authors);
        dest.writeByte((byte) (HasRead ? 1 : 0));
        dest.writeString(ISBN_8);
        dest.writeString(ISBN_13);
        dest.writeByte((byte) (IsOwned ? 1 : 0));
        dest.writeString(LCCN);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeString(VolumeId);
    }
}
