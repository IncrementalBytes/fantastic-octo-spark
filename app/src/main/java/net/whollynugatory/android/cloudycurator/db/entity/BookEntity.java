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
package net.whollynugatory.android.cloudycurator.db.entity;

import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.Serializable;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
  tableName = "books_table",
  foreignKeys = {
    @ForeignKey(entity = AuthorEntity.class, parentColumns = "id", childColumns = "author_id")
  },
  indices = {@Index(value = {"author_id"}, unique = true)}
)
public class BookEntity implements Serializable {

  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "volume_id")
  public String VolumeId;

  @ColumnInfo(name = "author_id")
  public int AuthorId;

  @ColumnInfo(name = "category_id")
  public int CategoryId;

  @ColumnInfo(name = "publisher_id")
  public int PublisherId;

  @ColumnInfo(name = "title")
  public String Title;

  @ColumnInfo(name = "isbn_8")
  public String ISBN_8;

  @ColumnInfo(name = "isbn_13")
  public String ISBN_13;

  @ColumnInfo(name = "lccn")
  public String LCCN;

  @ColumnInfo(name = "published_date")
  public String PublishedDate;

  @ColumnInfo(name = "is_owned")
  public boolean IsOwned;

  @ColumnInfo(name = "has_read")
  public boolean HasRead;

  @ColumnInfo(name = "added_date")
  public long AddedDate;

  @ColumnInfo(name = "updated_date")
  public long UpdatedDate;

  public BookEntity() {

    AddedDate = 0;
    AuthorId = -1;
    CategoryId = -1;
    HasRead = false;
    ISBN_8 = BaseActivity.DEFAULT_ISBN_8;
    ISBN_13 = BaseActivity.DEFAULT_ISBN_13;
    IsOwned = false;
    LCCN = BaseActivity.DEFAULT_LCCN;
    PublishedDate = "";
    PublisherId = -1;
    Title = "";
    UpdatedDate = 0;
    VolumeId = BaseActivity.DEFAULT_VOLUME_ID;
  }

  public BookEntity(BookEntity bookEntity) {

    AddedDate = bookEntity.AddedDate;
    AuthorId = bookEntity.AuthorId;
    CategoryId = bookEntity.CategoryId;
    HasRead = bookEntity.HasRead;
    ISBN_8 = bookEntity.ISBN_8;
    ISBN_13 = bookEntity.ISBN_13;
    IsOwned = bookEntity.IsOwned;
    LCCN = bookEntity.LCCN;
    PublishedDate = bookEntity.PublishedDate;
    PublisherId = bookEntity.PublisherId;
    Title = bookEntity.Title;
    UpdatedDate = bookEntity.UpdatedDate;
    VolumeId = bookEntity.VolumeId;
  }

  @Override
  public String toString() {

    return String.format(
      Locale.US,
      "BookEntity {VolumeId=%s, Title='%s', ISBN=[%s, %s], HasRead='%s', IsOwned='%s'}",
      VolumeId,
      Title,
      ISBN_8,
      ISBN_13,
      String.valueOf(HasRead),
      String.valueOf(IsOwned));
  }
}
