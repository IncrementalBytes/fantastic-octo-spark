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

import net.whollynugatory.android.cloudycurator.db.CuratorRepository;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.ui.MainActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class GetDataTask extends AsyncTask<Void, Void, ArrayList<BookDetail>> {

  private static final String TAG = BaseActivity.BASE_TAG + "GetDataTask";

  private WeakReference<MainActivity> mActivityWeakReference;
  private CuratorRepository mRepository;

  public GetDataTask(MainActivity activityContext, CuratorRepository repository) {

    Log.d(TAG, "++GetDataTask(MainActivity, CuratorRepository)");
    mActivityWeakReference = new WeakReference<>(activityContext);
    mRepository = repository;
  }

  protected ArrayList<BookDetail> doInBackground(Void... params) {

    Log.d(TAG, "++doInBackground(Void...)");
    return new ArrayList<>(mRepository.getAllBooks());
  }

  protected void onPostExecute(ArrayList<BookDetail> bookDetailList) {

    Log.d(TAG, "++onPostExecute(ArrayList<BookDetail>)");
    MainActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.getDataComplete(bookDetailList);
  }
}
