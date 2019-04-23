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

package net.frostedbytes.android.cloudycurator;

import android.content.Context;
import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;

import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtil;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class WriteToLocalLibraryTask extends AsyncTask<Void, Void, ArrayList<CloudyBook>> {

    private static final String TAG = BASE_TAG + WriteToLocalLibraryTask.class.getSimpleName();

    private WeakReference<MainActivity> mFragmentWeakReference;
    private ArrayList<CloudyBook> mCloudyBooks;

    WriteToLocalLibraryTask(MainActivity context, ArrayList<CloudyBook> cloudyBookList) {

        mFragmentWeakReference = new WeakReference<>(context);
        mCloudyBooks = cloudyBookList;
    }

    protected ArrayList<CloudyBook> doInBackground(Void... params) {

        ArrayList<CloudyBook> booksWritten = new ArrayList<>();
        FileOutputStream outputStream;
        try {
            outputStream = mFragmentWeakReference.get().getApplicationContext().openFileOutput(
                BaseActivity.DEFAULT_LIBRARY_FILE,
                Context.MODE_PRIVATE);
            for (CloudyBook cloudyBook : mCloudyBooks) {
                String lineContents = String.format(
                    Locale.US,
                    "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\r\n",
                    cloudyBook.VolumeId,
                    cloudyBook.ISBN_8,
                    cloudyBook.ISBN_13,
                    cloudyBook.LCCN,
                    cloudyBook.Title,
                    cloudyBook.getAuthorsDelimited(),
                    cloudyBook.getCategoriesDelimited(),
                    String.valueOf(cloudyBook.AddedDate),
                    String.valueOf(cloudyBook.HasRead),
                    String.valueOf(cloudyBook.IsOwned),
                    cloudyBook.PublishedDate,
                    cloudyBook.Publisher,
                    cloudyBook.UpdatedDate);
                outputStream.write(lineContents.getBytes());
                booksWritten.add(cloudyBook);
            }
        } catch (Exception e) {
            LogUtil.warn(TAG, "Exception when writing local library.");
            Crashlytics.logException(e);
        }

        return booksWritten;
    }

    protected void onPostExecute(ArrayList<CloudyBook> cloudyBookList) {

        LogUtil.debug(TAG, "++onPostExecute(%d)", cloudyBookList.size());
        MainActivity activity = mFragmentWeakReference.get();
        if (activity == null) {
            LogUtil.error(TAG, "Activity is null.");
            return;
        }

        activity.writeComplete(cloudyBookList);
    }
}
