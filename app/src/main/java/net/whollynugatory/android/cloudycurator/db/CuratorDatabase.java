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

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.whollynugatory.android.cloudycurator.db.dao.BookDao;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.views.AuthorSummaryView;
import net.whollynugatory.android.cloudycurator.db.views.CategorySummaryView;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
  entities = {BookEntity.class},
  views = {AuthorSummaryView.class, CategorySummaryView.class},
  version = 1,
  exportSchema = false
)
public abstract class CuratorDatabase extends RoomDatabase {

  private static final String TAG = BaseActivity.BASE_TAG + "CuratorDatabase";

  public abstract BookDao bookDao();

  private static volatile CuratorDatabase INSTANCE;
  private static final int NUMBER_OF_THREADS = 4;

  public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

  public static CuratorDatabase getInstance(final Context context) {

    Log.d(TAG, "++getDatabase(Context)");
    if (INSTANCE == null) {
      synchronized (CuratorDatabase.class) {
        if (INSTANCE == null) {
          INSTANCE = Room.databaseBuilder(
            context.getApplicationContext(),
            CuratorDatabase.class,
            BaseActivity.DATABASE_NAME)
            .addCallback(CuratorDatabaseCallback)
            .build();
        }
      }
    }

    return INSTANCE;
  }

  private static CuratorDatabase.Callback CuratorDatabaseCallback = new RoomDatabase.Callback() {

    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
      super.onCreate(db);

      Log.d(TAG, "++onCreate(SupportSQLiteDatabase)");
    }

    @Override
    public void onOpen(@NonNull SupportSQLiteDatabase db) {
      super.onOpen(db);

      Log.d(TAG, "++onOpen(SupportSQLiteDatabase)");
    }
  };
}
