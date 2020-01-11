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

package net.whollynugatory.android.cloudycurator.db.views;

import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.Serializable;
import java.util.Locale;

import androidx.room.DatabaseView;
import androidx.room.Ignore;

@DatabaseView(
  "SELECT BookTable.volume_id AS Id, " +
    "BookTable.isbn_8 AS ISBN_8, " +
    "BookTable.isbn_13 AS ISBN_13," +
    "BookTable.lccn AS LCCN, " +
    "BookTable.title AS Title, " +
    "BookTable.author_id AS AuthorId, " +
    "AuthorTable.Authors AS Authors, " +
    "BookTable.published_date AS Published, " +
    "BookTable.is_owned AS IsOwned, " +
    "BookTable.has_read AS HasRead, " +
    "BookTable.added_date AS DateAdded " +
    "FROM books_table AS BookTable " +
    "INNER JOIN authors_table AS AuthorTable ON AuthorTable.AuthorId = BookTable.author_id")
public class BookDetail implements Serializable {

  public String Id;
  public String ISBN_8;
  public String ISBN_13;
  public String LCCN;
  public String Title;
  public String Authors;
  public String AuthorId;
  public String Published;
  public boolean HasRead;
  public boolean IsOwned;
  public long DateAdded;

  public BookDetail() {

    Id = BaseActivity.DEFAULT_ID;
    ISBN_8 = BaseActivity.DEFAULT_ISBN_8;
    ISBN_13= BaseActivity.DEFAULT_ISBN_13;
    LCCN = BaseActivity.DEFAULT_LCCN;
    Title = "";
    Authors = "";
    AuthorId = BaseActivity.DEFAULT_ID;
    Published = "";
    HasRead = false;
    IsOwned = false;
    DateAdded = 0;
  }

  public BookDetail(String volumeId) {
    this();

    Id = volumeId;
  }

  public BookDetail(BookDetail bookDetails) {

    Id = bookDetails.Id;
    ISBN_8 = bookDetails.ISBN_8;
    ISBN_13 = bookDetails.ISBN_13;
    LCCN = bookDetails.LCCN;
    Title = bookDetails.Title;
    AuthorId = bookDetails.AuthorId;
    Published = bookDetails.Published;
    HasRead = bookDetails.HasRead;
    IsOwned = bookDetails.IsOwned;
    DateAdded = bookDetails.DateAdded;

    Authors = bookDetails.Authors;
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "Book { Title=%s, Author(s)=%s, %s, %s}",
      Title,
      Authors,
      HasRead ? "Read" : "Unread",
      IsOwned ? "Owned" : "Not Owned");
  }

  @Ignore
  public boolean hasBarcode() {

    return !ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) ||
      !ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ||
      !LCCN.equals(BaseActivity.DEFAULT_LCCN) ||
      !Title.isEmpty();
  }

  @Ignore
  public boolean isValid() {

    return !Id.isEmpty() && !Id.equals(BaseActivity.DEFAULT_VOLUME_ID) &&
      !AuthorId.isEmpty() && !AuthorId.equals(BaseActivity.DEFAULT_ID) &&
      !Title.isEmpty();
  }

  @Ignore
  public static BookDetail fromBookEntity(BookEntity bookEntity) {

    BookDetail bookDetail = new BookDetail();
    bookDetail.Id = bookEntity.VolumeId;
    bookDetail.ISBN_8 = bookEntity.ISBN_8;
    bookDetail.ISBN_13 = bookEntity.ISBN_13;
    bookDetail.LCCN = bookEntity.LCCN;
    bookDetail.Title = bookEntity.Title;
    bookDetail.Published = bookEntity.PublishedDate;
    bookDetail.AuthorId = bookEntity.AuthorId;
    bookDetail.HasRead = bookEntity.HasRead;
    bookDetail.IsOwned = bookEntity.IsOwned;
    bookDetail.DateAdded = bookEntity.AddedDate;
    return bookDetail;
  }
}
