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

package net.whollynugatory.android.cloudycurator.db.repositories;

import android.util.Log;

import net.whollynugatory.android.cloudycurator.db.CuratorDatabase;
import net.whollynugatory.android.cloudycurator.db.dao.BookDao;
import net.whollynugatory.android.cloudycurator.db.data.BookAuthor;
import net.whollynugatory.android.cloudycurator.db.data.BookCategory;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.List;

import androidx.lifecycle.LiveData;

public class BookRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "BookRepository";

  private static volatile BookRepository INSTANCE;

  private BookDao mBookDao;

  private BookRepository(BookDao bookDao) {

    mBookDao = bookDao;
  }

  public static BookRepository getInstance(final BookDao bookDao) {

    Log.d(TAG, "++getDatabase(Context)");
    if (INSTANCE == null) {
      synchronized (BookRepository.class) {
        if (INSTANCE == null) {
          INSTANCE = new BookRepository(bookDao);
        }
      }
    }

    return INSTANCE;
  }

  public void delete(String volumeId) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mBookDao.delete(volumeId));
  }

  public LiveData<BookEntity> find(String barcodeValue) {

    return mBookDao.find(barcodeValue);
  }

  public LiveData<List<BookAuthor>> getByAuthors() {

    return mBookDao.getAllByAuthors();
  }

  public LiveData<List<BookCategory>> getByCategories() {

    return mBookDao.getAllByCategories();
  }

  public LiveData<List<BookEntity>> getRecent() {

    return mBookDao.getAllByRecent();
  }

  public void insert(BookEntity bookEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mBookDao.insert(bookEntity));
  }

  public void update(BookEntity bookEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mBookDao.update(bookEntity));
  }
}
