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
package net.whollynugatory.android.cloudycurator.db;

import android.content.Context;
import android.util.Log;

import net.whollynugatory.android.cloudycurator.db.entity.AuthorEntity;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.entity.CategoryEntity;
import net.whollynugatory.android.cloudycurator.db.entity.PublisherEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.List;

import androidx.lifecycle.LiveData;

public class CuratorRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "CuratorRepository";

  private static CuratorRepository sInstance;

  private final CuratorDatabase mDatabase;

  private CuratorRepository(final Context context) {

    Log.d(TAG, "++CuratorRepository()");
    mDatabase = CuratorDatabase.getInstance(context);
  }

  public static CuratorRepository getInstance(final Context context) {

    if (sInstance == null) {
      synchronized (CuratorRepository.class) {
        if (sInstance == null) {
          sInstance = new CuratorRepository(context);
        }
      }
    }

    return sInstance;
  }

  /*
    Author methods
   */
  public int countAuthors() {

    return mDatabase.authorDao().count();
  }

  public List<AuthorEntity> getAllAuthors() {

    return mDatabase.authorDao().getAll();
  }

  public void insertAuthor(AuthorEntity authorEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mDatabase.authorDao().insert(authorEntity));
  }

  /*
    Book methods
   */
  public int countBooks() {

    return mDatabase.bookDao().count();
  }

  public LiveData<List<BookEntity>> getAllBooks() {

    return mDatabase.bookDao().getAll();
  }

  public void insertBookEntity(BookEntity bookEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mDatabase.bookDao().insert(bookEntity));
  }

  /*
  Category methods
 */
  public int countCategories() {

    return mDatabase.categoryDao().count();
  }

  public List<CategoryEntity> getAllCategories() {

    return mDatabase.categoryDao().getAll();
  }

  public void insertCategory(CategoryEntity categoryEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mDatabase.categoryDao().insert(categoryEntity));
  }

  /*
    Publisher method(s)
   */
  public int countPublishers() {

    return mDatabase.publisherDao().count();
  }

  public List<PublisherEntity> getAllPublishers() {

    return mDatabase.publisherDao().getAll();
  }

  public void insertPublisher(PublisherEntity publisherEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mDatabase.publisherDao().insert(publisherEntity));
  }
}
