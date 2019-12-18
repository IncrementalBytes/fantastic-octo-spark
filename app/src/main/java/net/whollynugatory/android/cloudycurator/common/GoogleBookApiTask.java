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

import com.crashlytics.android.Crashlytics;

import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.LiveBarcodeScanningActivity;
import net.whollynugatory.android.cloudycurator.ui.MainActivity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

/*
    Retrieve data task; querying URLs for data
 */
public class GoogleBookApiTask extends AsyncTask<Void, Void, ArrayList<BookEntity>> {

  private static final String TAG = BaseActivity.BASE_TAG + "GoogleBookAPITask";

  private WeakReference<LiveBarcodeScanningActivity> mActivityWeakReference;
  private BookDetail mQueryForBook;

  public GoogleBookApiTask(LiveBarcodeScanningActivity activityContext, BookDetail queryForBook) {

    Log.d(TAG, "++GoogleBookApiTask(MainActivity, BookEntity)");
    mActivityWeakReference = new WeakReference<>(activityContext);
    mQueryForBook = queryForBook;
  }

  protected ArrayList<BookEntity> doInBackground(Void... params) {

    Log.d(TAG, "++doInBackground(Void...)");
    ArrayList<BookEntity> bookEntities = new ArrayList<>();
    String searchParam = null;
    if (!mQueryForBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) {
      searchParam = String.format(Locale.US, "isbn:%s", mQueryForBook.ISBN_8);
    } else if (!mQueryForBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
      searchParam = String.format(Locale.US, "isbn:%s", mQueryForBook.ISBN_13);
    } else if (!mQueryForBook.Title.isEmpty()) {
      searchParam = "intitle:" + "\"" + mQueryForBook.Title + "\"";
    } else if (!mQueryForBook.LCCN.equals(BaseActivity.DEFAULT_LCCN)) {
      searchParam = String.format(Locale.US, "lccn:%s", mQueryForBook.LCCN);
    }

    if (searchParam == null) {
      Log.e(TAG, "Missing search parameter; cannot continue.");
      return bookEntities;
    }

    String urlString = String.format(
      Locale.US,
      "https://www.googleapis.com/books/v1/volumes?q=%s&printType=books",
      searchParam);

    // add fields to urlString
    urlString = String.format(
      Locale.US,
      "%s&fields=items(id,volumeInfo/title,volumeInfo/authors,volumeInfo/publisher,volumeInfo/publishedDate,volumeInfo/industryIdentifiers,volumeInfo/categories)",
      urlString);

    Log.d(TAG, "Query: " + urlString);
    HttpURLConnection connection = null;
    StringBuilder builder = new StringBuilder();
    try {
      URL url = new URL(urlString);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setReadTimeout(20000); // 5 seconds
      connection.setConnectTimeout(20000); // 5 seconds

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        Log.e(TAG, "GoogleBooksAPI request failed. Response Code: " + responseCode);
        connection.disconnect();
        return bookEntities;
      }

      BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = responseReader.readLine();
      while (line != null) {
        builder.append(line);
        line = responseReader.readLine();
      }
    } catch (IOException e) {
      if (connection != null) {
        connection.disconnect();
      }

      return bookEntities;
    }

    JSONArray items;
    try {
      JSONObject responseJson = new JSONObject(builder.toString());
      items = (JSONArray) responseJson.get("items");
    } catch (JSONException e) {
      connection.disconnect();
      return bookEntities;
    }

    if (items != null) {
      for (int index = 0; index < items.length(); index++) {
        try { // errors parsing items should not prevent further parsing
          JSONObject item = (JSONObject) items.get(index);
          JSONObject volumeInfo = item.getJSONObject("volumeInfo");

          BookEntity bookEntity = new BookEntity();
          if (volumeInfo.has("authors")) {
            JSONArray infoArray = volumeInfo.getJSONArray("authors");
            for (int subIndex = 0; subIndex < infoArray.length(); subIndex++) {
              bookEntity.Authors.add((String) infoArray.get(subIndex));
            }
          }

          if (volumeInfo.has("categories")) {
            JSONArray infoArray = volumeInfo.getJSONArray("categories");
            for (int subIndex = 0; subIndex < infoArray.length(); subIndex++) {
              bookEntity.Categories.add((String) infoArray.get(subIndex));
            }
          }

          if (volumeInfo.has("industryIdentifiers")) {
            JSONArray infoArray = volumeInfo.getJSONArray("industryIdentifiers");
            for (int subIndex = 0; subIndex < infoArray.length(); subIndex++) {
              JSONObject identifiers = infoArray.getJSONObject(subIndex);
              if (identifiers.getString("type").equals("ISBN_13")) {
                bookEntity.ISBN_13 = identifiers.getString("identifier");
              } else if (identifiers.getString("type").equals("ISBN_10")) {
                bookEntity.ISBN_8 = identifiers.getString("identifier");
              }
            }
          }

          // TODO: check for identifiers, if not found what next?

          if (volumeInfo.has("publishedDate")) {
            bookEntity.PublishedDate = volumeInfo.getString("publishedDate");
          }

          if (volumeInfo.has("publisher")) {
            bookEntity.Publisher = volumeInfo.getString("publisher");
          }

          // if title or id are missing, allow exception to be thrown to skip
          bookEntity.Title = volumeInfo.getString("title");
          bookEntity.VolumeId = item.getString("id");

          // TODO: should we validate before adding?

          bookEntities.add(bookEntity);
        } catch (JSONException e) {
          Log.d(TAG, "Failed to parse JSON object.");
          Crashlytics.logException(e);
        }
      }
    } else {
      Log.w(TAG, "No expected items where found in response.");
    }

    connection.disconnect();
    return bookEntities;
  }

  protected void onPostExecute(ArrayList<BookEntity> bookEntities) {

    Log.d(TAG, "++onPostExecute(ArrayList<BookEntity>)");
    LiveBarcodeScanningActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.retrieveBooksComplete(bookEntities);
  }
}
