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
import java.util.List;

public class CloudyBook extends Book implements Parcelable {

    @Exclude
    public static final String ROOT = "CloudyBooks";

    public List<String> Categories;

    public long CreatedDate;

    public String PublishedDate;

    public long UpdatedDate;

    public CloudyBook() {

        Categories = new ArrayList<>();
        CreatedDate = 0;
        PublishedDate = "";
        UpdatedDate = 0;
    }

    protected CloudyBook(Parcel in) {

        Authors = new ArrayList<>();
        in.readList(Authors, String.class.getClassLoader());
        Categories = new ArrayList<>();
        in.readList(Categories, String.class.getClassLoader());
        CreatedDate = in.readLong();
        ISBN_8 = in.readString();
        ISBN_13 = in.readString();
        LCCN = in.readString();
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
    public String toString() {

        return "CloudyBook{" +
            "VolumeId=" + VolumeId +
            ", Title='" + Title + '\'' +
            ", PublishedDate='" + PublishedDate + '\'' +
            ", ISBN={" + ISBN_8 + ", " + ISBN_13 + "}\'" +
            ", LCCN=" + LCCN + "\'" +
            ", UpdatedDate=" + UpdatedDate +
            '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeList(Authors);
        dest.writeList(Categories);
        dest.writeLong(CreatedDate);
        dest.writeString(ISBN_8);
        dest.writeString(ISBN_13);
        dest.writeString(LCCN);
        dest.writeString(PublishedDate);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
        dest.writeString(VolumeId);
    }
}
