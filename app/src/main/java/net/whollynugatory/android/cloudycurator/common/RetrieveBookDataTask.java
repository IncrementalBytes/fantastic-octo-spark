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

import com.crashlytics.android.Crashlytics;

import net.whollynugatory.android.cloudycurator.BaseActivity;
import net.whollynugatory.android.cloudycurator.MainActivity;
import net.whollynugatory.android.cloudycurator.models.CloudyBook;

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
public class RetrieveBookDataTask extends AsyncTask<Void, Void, ArrayList<CloudyBook>> {

    private static final String TAG = BaseActivity.BASE_TAG + RetrieveBookDataTask.class.getSimpleName();

    private WeakReference<MainActivity> mActivityWeakReference;
    private CloudyBook mQueryForBook;

    public RetrieveBookDataTask(MainActivity context, CloudyBook queryForBook) {

        mActivityWeakReference = new WeakReference<>(context);
        mQueryForBook = queryForBook;
    }

    protected ArrayList<CloudyBook> doInBackground(Void... params) {

        ArrayList<CloudyBook> cloudyBooks = new ArrayList<>();
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
            LogUtils.error(TAG, "Missing search parameter; cannot continue.");
            return cloudyBooks;
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

        LogUtils.debug(TAG, "Query: %s", urlString);
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
                LogUtils.error(TAG, "GoogleBooksAPI request failed. Response Code: " + responseCode);
                connection.disconnect();
                return cloudyBooks;
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

            return cloudyBooks;
        }

        JSONArray items;
        try {
            JSONObject responseJson = new JSONObject(builder.toString());
            items = (JSONArray) responseJson.get("items");
        } catch (JSONException e) {
            connection.disconnect();
            return cloudyBooks;
        }

        if (items != null) {
            for (int index = 0; index < items.length(); index++) {
                try { // errors parsing items should not prevent further parsing
                    JSONObject item = (JSONObject) items.get(index);
                    JSONObject volumeInfo = item.getJSONObject("volumeInfo");

                    CloudyBook cloudyBook = new CloudyBook();
                    if (volumeInfo.has("authors")) {
                        JSONArray infoArray = volumeInfo.getJSONArray("authors");
                        for (int subIndex = 0; subIndex < infoArray.length(); subIndex++) {
                            cloudyBook.Authors.add((String) infoArray.get(subIndex));
                        }
                    }

                    if (volumeInfo.has("categories")) {
                        JSONArray infoArray = volumeInfo.getJSONArray("categories");
                        for (int subIndex = 0; subIndex < infoArray.length(); subIndex++) {
                            cloudyBook.Categories.add((String) infoArray.get(subIndex));
                        }
                    }

                    if (volumeInfo.has("industryIdentifiers")) {
                        JSONArray infoArray = volumeInfo.getJSONArray("industryIdentifiers");
                        for (int subIndex = 0; subIndex < infoArray.length(); subIndex++) {
                            JSONObject identifiers = infoArray.getJSONObject(subIndex);
                            if (identifiers.getString("type").equals("ISBN_13")) {
                                cloudyBook.ISBN_13 = identifiers.getString("identifier");
                            } else if (identifiers.getString("type").equals("ISBN_10")) {
                                cloudyBook.ISBN_8 = identifiers.getString("identifier");
                            }
                        }
                    }

                    if (volumeInfo.has("publishedDate")) {
                        cloudyBook.PublishedDate = volumeInfo.getString("publishedDate");
                    }

                    if (volumeInfo.has("publisher")) {
                        cloudyBook.Publisher = volumeInfo.getString("publisher");
                    }

                    // if title or id are missing, allow exception to be thrown to skip
                    cloudyBook.Title = volumeInfo.getString("title");
                    cloudyBook.VolumeId = item.getString("id");

                    // TODO: should we validate before adding?
                    cloudyBooks.add(cloudyBook);
                } catch (JSONException e) {
                    LogUtils.debug(TAG, "Failed to parse JSON object.");
                    Crashlytics.logException(e);
                }
            }
        } else {
            LogUtils.warn(TAG, "No expected items where found in response.");
        }

        connection.disconnect();
        return cloudyBooks;
    }

    protected void onPostExecute(ArrayList<CloudyBook> cloudyBooks) {

        LogUtils.debug(TAG, "++onPostExecute(%d)", cloudyBooks.size());
        MainActivity activity = mActivityWeakReference.get();
        if (activity == null) {
            LogUtils.error(TAG, "MainActivity is null or detached.");
            return;
        }

        activity.retrieveBooksComplete(cloudyBooks);
    }
}
