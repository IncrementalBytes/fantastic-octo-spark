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

public class CloudyBook implements Parcelable {

    @Exclude
    public static final String ROOT = "CloudyBooks";

    public List<String> Authors;

    public List<String> Categories;

    public long CreatedDate;

    @Exclude
    public String ISBN;

    public String PublishedDate;

    public String Title;

    public long UpdatedDate;

    public String VolumeId;

    public CloudyBook() {

        Authors = new ArrayList<>();
        Categories = new ArrayList<>();
        CreatedDate = 0;
        ISBN = BaseActivity.DEFAULT_ISBN;
        PublishedDate = "";
        Title = "";
        UpdatedDate = 0;
        VolumeId = "";
    }

    protected CloudyBook(Parcel in) {

        Authors = new ArrayList<>();
        in.readList(Authors, String.class.getClassLoader());
        Categories = new ArrayList<>();
        in.readList(Categories, String.class.getClassLoader());
        CreatedDate = in.readLong();
        ISBN = in.readString();
        PublishedDate = in.readString();
        Title = in.readString();
        UpdatedDate = in.readLong();
        VolumeId = in.readString();
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

        CloudyBook cloudyBook = (CloudyBook) o;
        return Objects.equals(ISBN, cloudyBook.ISBN) && Objects.equals(Title, cloudyBook.Title);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ISBN, Title);
    }

    @Override
    public String toString() {

        return "CloudyBook{" +
            "CreatedDate=" + CreatedDate +
            ", ISBN='" + ISBN + '\'' +
            ", PublishedDate='" + PublishedDate + '\'' +
            ", Title='" + Title + '\'' +
            ", UpdatedDate=" + UpdatedDate +
            ", VolumeId='" + VolumeId + '\'' +
            '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeList(Authors);
        dest.writeList(Categories);
        dest.writeLong(CreatedDate);
        dest.writeString(ISBN);
        dest.writeString(PublishedDate);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeString(VolumeId);
    }
}
