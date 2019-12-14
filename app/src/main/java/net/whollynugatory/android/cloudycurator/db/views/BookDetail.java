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
    "AuthorTable.author_string AS Authors, " +
    "BookTable.category_id AS CategoryId, " +
    "CategorieTable.category_string AS Categories, " +
    "BookTable.publisher_id AS PublisherId, " +
    "PublisherTable.publisher_string AS Publishers, " +
    "BookTable.published_date AS Published, " +
    "BookTable.is_owned AS IsOwned, " +
    "BookTable.has_read AS HasRead " +
    "FROM books_table AS BookTable " +
    "INNER JOIN authors_table AS AuthorTable ON AuthorTable.id = BookTable.author_id " +
    "INNER JOIN publishers_table AS PublisherTable ON PublisherTable.id = BookTable.publisher_id " +
    "INNER JOIN categories_table AS CategorieTable ON CategorieTable.id = BookTable.category_id")
public class BookDetail implements Serializable {

  public String Id;
  public String ISBN_8;
  public String ISBN_13;
  public String LCCN;
  public String Title;
  public String Authors;
  public long AuthorId;
  public String Publishers;
  public long PublisherId;
  public String Categories;
  public long CategoryId;
  public String Published;
  public boolean HasRead;
  public boolean IsOwned;

  public BookDetail() {

    Id = BaseActivity.DEFAULT_ID;
    ISBN_8 = BaseActivity.DEFAULT_ISBN_8;
    ISBN_13= BaseActivity.DEFAULT_ISBN_13;
    LCCN = BaseActivity.DEFAULT_LCCN;
    Title = "";
    Authors = "";
    AuthorId = -1;
    Publishers = "";
    PublisherId = -1;
    Categories = "";
    CategoryId = -1;
    Published = "";
    HasRead = false;
    IsOwned = false;
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
    PublisherId = bookDetails.PublisherId;
    CategoryId = bookDetails.CategoryId;
    Published = bookDetails.Published;
    HasRead = bookDetails.HasRead;
    IsOwned = bookDetails.IsOwned;

    Authors = bookDetails.Authors;
    Categories = bookDetails.Categories;
    Publishers = bookDetails.Publishers;
  }

  /*
    Object Override(s)
   */
  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "Book { Title=%s, Author(s)=%s, Categories=%s, Publisher(s)=%s, %s, %s}",
      Title,
      Authors,
      Categories,
      Publishers,
      HasRead ? "Read" : "Unread",
      IsOwned ? "Owned" : "Not Owned");
  }

  @Ignore
  public boolean isValid() {

    return !Id.isEmpty() && !Id.equals(BaseActivity.DEFAULT_VOLUME_ID) &&
      AuthorId >= 0 &&
      CategoryId >= 0 &&
      PublisherId >= 0 &&
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
    bookDetail.CategoryId = bookEntity.CategoryId;
    bookDetail.PublisherId = bookEntity.PublisherId;
    bookDetail.HasRead = bookEntity.HasRead;
    bookDetail.IsOwned = bookEntity.IsOwned;
    return bookDetail;
  }
}
