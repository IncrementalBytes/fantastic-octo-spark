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
import java.util.Objects;

public class UserBook implements Parcelable {

    @Exclude
    public static final String ROOT = "UserBooks";

    public long AddedDate;

    public List<String> Authors;

    public boolean HasRead;

    @Exclude
    public String ISBN;

    public boolean IsOwned;

    public String Title;

    public long UpdatedDate;

    public UserBook() {

        AddedDate = 0;
        Authors = new ArrayList<>();
        HasRead = false;
        ISBN = BaseActivity.DEFAULT_ISBN;
        IsOwned = false;
        Title = "";
        UpdatedDate = 0;
    }

    protected UserBook(Parcel in) {

        AddedDate = in.readLong();
        Authors = new ArrayList<>();
        in.readList(Authors, String.class.getClassLoader());
        HasRead = in.readByte() != 0;
        ISBN = in.readString();
        IsOwned = in.readByte() != 0;
        Title = in.readString();
        UpdatedDate = in.readLong();
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
        return ISBN.equals(userBook.ISBN);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), HasRead, IsOwned);
    }

    @Override
    public String toString() {

        return "UserBook{" +
            "HasRead=" + HasRead +
            ", ISBN='" + ISBN + '\'' +
            ", IsOwned=" + IsOwned +
            ", Title='" + Title + '\'' +
            '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeList(Authors);
        dest.writeByte((byte) (HasRead ? 1 : 0));
        dest.writeString(ISBN);
        dest.writeByte((byte) (IsOwned ? 1 : 0));
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
    }
}
