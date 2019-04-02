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

import java.util.Locale;
import java.util.Objects;

public class Book implements Parcelable {

    @Exclude
    public static final String ROOT = "Books";

    public long AddedDate;

    public String Author;

    @Exclude
    public String ISBN;

    public String Title;

    public long UpdatedDate;

    public Book() {

        this.AddedDate = 0;
        this.Author = "";
        this.ISBN = BaseActivity.DEFAULT_ISBN;
        this.Title = "";
        this.UpdatedDate = 0;
    }

    protected Book(Parcel in) {

        AddedDate = in.readLong();
        Author = in.readString();
        ISBN = in.readString();
        Title = in.readString();
        UpdatedDate = in.readLong();
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
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

        Book book = (Book) o;
        return Objects.equals(Author, book.Author) &&
            Objects.equals(ISBN, book.ISBN) &&
            Objects.equals(Title, book.Title);
    }

    @Override
    public int hashCode() {

        return Objects.hash(Author, ISBN, Title);
    }

    @Override
    public String toString() {

        String formattedMessage = String.format(Locale.ENGLISH, "%s", this.Title);
        if (!this.Author.isEmpty()) {
            formattedMessage = String.format(Locale.ENGLISH, "%s (%s)", formattedMessage, this.Author);
        }

        if (!this.ISBN.isEmpty() && this.ISBN.compareToIgnoreCase(BaseActivity.DEFAULT_ISBN) != 0) {
            formattedMessage = String.format(Locale.ENGLISH, "%s - %s", formattedMessage, this.ISBN);
        }

        return formattedMessage;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(AddedDate);
        dest.writeString(Author);
        dest.writeString(ISBN);
        dest.writeString(Title);
        dest.writeLong(UpdatedDate);
    }
}
