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
package net.whollynugatory.android.cloudycurator.db.dao;

import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface BookDao {

  @Query("SELECT COUNT(*) FROM books_table")
  int count();

  @Query("DELETE FROM books_table WHERE volume_id = :volumeId")
  int delete(String volumeId);

  @Query("SELECT * FROM bookdetail WHERE id = :bookId OR isbn_8 = :bookId OR isbn_13 = :bookId")
  BookDetail get(String bookId);

  @Query("SELECT * FROM bookdetail")
  List<BookDetail> getAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insert(BookEntity bookEntity);
}
