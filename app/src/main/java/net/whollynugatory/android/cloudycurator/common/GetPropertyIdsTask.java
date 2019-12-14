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
package net.whollynugatory.android.cloudycurator.common;

import android.os.AsyncTask;
import android.util.Log;

import net.whollynugatory.android.cloudycurator.db.CuratorDatabase;
import net.whollynugatory.android.cloudycurator.db.entity.AuthorEntity;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.entity.CategoryEntity;
import net.whollynugatory.android.cloudycurator.db.entity.PublisherEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.ui.MainActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public class GetPropertyIdsTask extends AsyncTask<Void, Void, BookEntity> {

  private static final String TAG = BaseActivity.BASE_TAG + "GetAuthorIdTask";

  private WeakReference<MainActivity> mActivityWeakReference;
  private BookEntity mBookEntity;
  private CuratorDatabase mCuratorDatabase;

  public GetPropertyIdsTask(MainActivity activityContext, CuratorDatabase curatorDatabase, BookEntity bookEntity) {

    Log.d(TAG, "++GetPropertyIdsTask(MainActivity, CuratorDatabase, BookEntity)");
    mActivityWeakReference = new WeakReference<>(activityContext);
    mBookEntity = bookEntity;
    mCuratorDatabase = curatorDatabase;
  }

  protected BookEntity doInBackground(Void... params) {

    Log.d(TAG, "++doInBackground(Void...)");
    if (mBookEntity.AuthorId < 0) {
      List<AuthorEntity> authorEntityList = mCuratorDatabase.authorDao().getAll();
      for (AuthorEntity authorEntity : authorEntityList) {
        if (authorEntity.AuthorString.equals(mBookEntity.getAuthorsDelimited())) {
          mBookEntity.AuthorId = authorEntity.Id;
          break;
        }
      }

      if (mBookEntity.AuthorId < 0) { // author not found, insert
        AuthorEntity authorEntity = new AuthorEntity(mBookEntity.getAuthorsDelimited());
        mBookEntity.AuthorId = mCuratorDatabase.authorDao().insert(authorEntity);
      }
    }

    if (mBookEntity.CategoryId < 0) {
      List<CategoryEntity> categoryEntityList = mCuratorDatabase.categoryDao().getAll();
      for (CategoryEntity categoryEntity : categoryEntityList) {
        if (categoryEntity.CategoryString.equals(mBookEntity.getCategoriesDelimited())) {
          mBookEntity.CategoryId = categoryEntity.Id;
          break;
        }
      }

      if (mBookEntity.CategoryId < 0) { // category not found, insert
        CategoryEntity categoryEntity = new CategoryEntity(mBookEntity.getCategoriesDelimited());
        mBookEntity.CategoryId = mCuratorDatabase.categoryDao().insert(categoryEntity);
      }
    }

    if (mBookEntity.PublisherId < 0) {
      List<PublisherEntity> publisherEntityList = mCuratorDatabase.publisherDao().getAll();
      for (PublisherEntity publisherEntity : publisherEntityList) {
        if (publisherEntity.PublisherString.equals(mBookEntity.Publisher)) {
          mBookEntity.PublisherId = publisherEntity.Id;
          break;
        }
      }

      if (mBookEntity.PublisherId < 0) { // publisher not found, insert
        PublisherEntity publisherEntity = new PublisherEntity(mBookEntity.Publisher);
        mBookEntity.PublisherId = mCuratorDatabase.publisherDao().insert(publisherEntity);
      }
    }

    return mBookEntity;
  }

  protected void onPostExecute(BookEntity bookEntity) {

    Log.d(TAG, "++onPostExecute(BookEntity)");
    MainActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.getPropertyIdComplete(bookEntity);
  }
}
