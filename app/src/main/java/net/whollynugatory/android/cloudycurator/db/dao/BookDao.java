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

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface BookDao {

  @Query("DELETE FROM books_table WHERE volume_id = :volumeId")
  void delete(String volumeId);

  @Query("SELECT * FROM books_table WHERE volume_id = :bookId OR isbn_8 = :bookId OR isbn_13 = :bookId")
  LiveData<BookEntity> find(String bookId);

  @Query("SELECT * FROM books_table LIMIT 50")
  LiveData<List<BookEntity>> getAllByRecent();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  long insert(BookEntity bookEntity);

  @Update
  void update(BookEntity bookEntity);
}
