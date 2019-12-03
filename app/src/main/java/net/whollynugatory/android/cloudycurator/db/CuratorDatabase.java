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
 *
 *    Referencing:
 *    https://github.com/android/architecture-components-samples/blob/master/BasicSample/app/src/main/java/com/example/android/persistence/db/AppDatabase.java
 */
package net.whollynugatory.android.cloudycurator.db;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.whollynugatory.android.cloudycurator.db.dao.AuthorDao;
import net.whollynugatory.android.cloudycurator.db.dao.BookDao;
import net.whollynugatory.android.cloudycurator.db.dao.CategoryDao;
import net.whollynugatory.android.cloudycurator.db.dao.PublisherDao;
import net.whollynugatory.android.cloudycurator.db.entity.AuthorEntity;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.entity.CategoryEntity;
import net.whollynugatory.android.cloudycurator.db.entity.PublisherEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
  entities = {AuthorEntity.class, BookEntity.class, CategoryEntity.class, PublisherEntity.class},
  version = 1,
  exportSchema = false
)
public abstract class CuratorDatabase extends RoomDatabase {

  private static final String TAG = BaseActivity.BASE_TAG + "CuratorDatabase";
  private static final int NUMBER_OF_THREADS = 4;

  private static CuratorDatabase sInstance;

  public abstract AuthorDao authorDao();

  public abstract BookDao bookDao();

  public abstract CategoryDao categoryDao();

  public abstract PublisherDao publisherDao();

  static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

  public static CuratorDatabase getInstance(final Context context) {

    if (sInstance == null) {
      synchronized (CuratorDatabase.class) {
        if (sInstance == null) {
          Log.d(TAG, "Building RoomDatabase object.");
          sInstance = Room.databaseBuilder(context, CuratorDatabase.class, BaseActivity.DATABASE_NAME).build();
        }
      }
    }

    return sInstance;
  }
}
