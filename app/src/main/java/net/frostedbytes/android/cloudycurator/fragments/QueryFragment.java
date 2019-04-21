package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.BuildConfig;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class QueryFragment extends Fragment {

    private static final String TAG = BASE_TAG + QueryFragment.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public interface OnQueryListener {

        void onQueryActionComplete(String message);

        void onQueryFoundMultipleBooks(ArrayList<CloudyBook> cloudyBooks);

        void onQueryFoundBook(CloudyBook cloudyBook);

        void onQueryNoBarCodesDetected(Bitmap bitmapData);

        void onQueryStarted();

        void onQueryTakePicture();

        void onQueryTextResultsFound(ArrayList<String> textResults);
    }

    private OnQueryListener mCallback;

    private ArrayList<CloudyBook> mCloudyBookList;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private int mScanType;

    public static QueryFragment newInstance(ArrayList<CloudyBook> cloudyBookList) {

        LogUtils.debug(TAG, "++newInstance()");
        QueryFragment fragment = new QueryFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(BaseActivity.ARG_CLOUDY_BOOK_LIST, cloudyBookList);
        fragment.setArguments(args);
        return fragment;
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        LogUtils.debug(TAG, "++onAttach(Context)");
        try {
            mCallback = (OnQueryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mCloudyBookList = arguments.getParcelableArrayList(BaseActivity.ARG_CLOUDY_BOOK_LIST);
        } else {
            String message = "Arguments were null.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        View view = inflater.inflate(R.layout.fragment_book_query, container, false);

        CardView scanISBNCard = view.findViewById(R.id.query_card_isbn);
        CardView scanTitleCard = view.findViewById(R.id.query_card_text);
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                scanISBNCard.setOnClickListener(v -> {
                    mScanType = BaseActivity.SCAN_ISBN;
                    mCallback.onQueryTakePicture();
                });

                scanTitleCard.setOnClickListener(v -> {
                    mScanType = BaseActivity.SCAN_TEXT;
                    mCallback.onQueryTakePicture();
                });
            } else {
                String message = "Camera feature is not available; disabling camera.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
                scanISBNCard.setEnabled(false);
                scanTitleCard.setEnabled(false);
            }
        } else {
            String message = "Camera not detected.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
            scanISBNCard.setEnabled(false);
            scanTitleCard.setEnabled(false);
        }

        CardView manualCard = view.findViewById(R.id.query_card_manual);
        manualCard.setOnClickListener(v -> showManualDialog());

        mCallback.onQueryActionComplete("");
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mCloudyBookList = null;
        if (mCurrentPhotoPath != null) {
            File file = new File(mCurrentPhotoPath);
            if (file.exists()) {
                if (file.delete()) {
                    LogUtils.debug(TAG, "Removed processed image: %s", mCurrentPhotoPath);
                } else {
                    LogUtils.warn(TAG, "Unable to remove processed image: %s", mCurrentPhotoPath);
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (BuildConfig.DEBUG) {
                // File f = new File(getString(R.string.debug_path), "20190413_094436.jpg");
                File f = new File(getString(R.string.debug_path), "20190413_094436.jpg");
                try {
                    mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                File f = new File(mCurrentPhotoPath);
                try {
                    mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if (mImageBitmap != null) {
                Bitmap emptyBitmap = Bitmap.createBitmap(
                    mImageBitmap.getWidth(),
                    mImageBitmap.getHeight(),
                    mImageBitmap.getConfig());
                if (!mImageBitmap.sameAs(emptyBitmap)) {
                    if (mScanType == BaseActivity.SCAN_ISBN) {
                        scanImageForISBN();
                    } else if (mScanType == BaseActivity.SCAN_TEXT){
                        scanImageForText();
                    } else {
                        String message = String.format(Locale.US, "Unknown scan type: %s", mScanType);
                        LogUtils.warn(TAG, message);
                        mCallback.onQueryActionComplete(message);
                    }
                } else {
                    String message = "Image was empty.";
                    LogUtils.warn(TAG, message);
                    mCallback.onQueryActionComplete(message);
                }
            } else {
                String message = "Image does not exist.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
            }
        } else {
            String message = "Unexpected data from camera.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        }
    }

    /*
        Public method(s)
     */
    /**
     * Queries the user's current book list for book.
     * @param cloudyBook Book to search for in user's current book list.
     */
    public void queryInUserBooks(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++queryInUserBooks(%s)", cloudyBook.toString());
        CloudyBook foundBook = null;
        if (mCloudyBookList != null) {
            for (CloudyBook book : mCloudyBookList) {
                if (book.isPartiallyEqual(cloudyBook)) {
                    foundBook = book;
                    break;
                }
            }
        }

        if (foundBook != null) {
            mCallback.onQueryFoundBook(foundBook);
        } else {
            LogUtils.debug(TAG, "Did not find %s in user's book list.", cloudyBook.toString());
            queryGoogleBookService(cloudyBook);
        }
    }

    /**
     * Starts the common ACTION_IMAGE_CAPTURE intent.
     */
    public void takePictureIntent() {

        LogUtils.debug(TAG, "++takePictureIntent()");
        mCallback.onQueryStarted();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getActivity() != null) {
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(
                        getActivity(),
                        "net.frostedbytes.android.cloudycurator.fileprovider",
                        photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    String message = "Failed to get file for photo.";
                    LogUtils.warn(TAG, message);
                    mCallback.onQueryActionComplete(message);
                }
            } else {
                String message = "Unable to create camera intent.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
            }
        } else {
            String message = "Could not get activity object.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        }
    }

    /*
        Private Method(s)
     */
    private File createImageFile() throws IOException {

        LogUtils.debug(TAG, "++createImageFile()");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        if (getActivity() != null) {
            File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            mCurrentPhotoPath = image.getAbsolutePath();
            return image;
        }

        return null;
    }

    private void parseFeed(ArrayList<CloudyBook> cloudyBooks) {

        LogUtils.debug(TAG, "++parseFeed(%d)", cloudyBooks.size());
        if (cloudyBooks.size() > 0) {
            mCallback.onQueryFoundMultipleBooks(cloudyBooks);
        } else {
            mCallback.onQueryActionComplete("No results found.");
        }
    }

    private void queryGoogleBookService(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++queryGoogleBookService(%s)", cloudyBook.toString());
        if (cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) &&
            cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) &&
            cloudyBook.LCCN.equals(BaseActivity.DEFAULT_LCCN) &&
            cloudyBook.Title.isEmpty()) {
            String message = "Invalid search criteria.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        } else {
            if (getActivity() != null) {
                new RetrieveBookDataTask(this, cloudyBook).execute();
            } else {
                String message = "Could not get activity's content resolver.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
            }
        }
    }

    private void scanImageForISBN() {

        LogUtils.debug(TAG, "++scanImageForISBN()");
        if (mImageBitmap != null) {
            FirebaseVisionBarcodeDetectorOptions options =
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_EAN_8,
                        FirebaseVisionBarcode.FORMAT_EAN_13)
                    .build();
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
            FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector(options);
            com.google.android.gms.tasks.Task<java.util.List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        CloudyBook cloudyBook = new CloudyBook();
                        for (FirebaseVisionBarcode barcode : task.getResult()) {
                            if (barcode.getValueType() == FirebaseVisionBarcode.TYPE_ISBN) {
                                String barcodeValue = barcode.getDisplayValue();
                                LogUtils.debug(TAG, "Found a bar code: %s", barcodeValue);
                                if (barcodeValue != null && barcodeValue.length() == 8) {
                                    cloudyBook.ISBN_8 = barcodeValue;
                                } else if (barcodeValue != null && barcodeValue.length() == 13) {
                                    cloudyBook.ISBN_13 = barcodeValue;
                                }
                            } else {
                                LogUtils.warn(TAG, "Unexpected bar code: %s", barcode.getDisplayValue());
                            }
                        }

                        if ((!cloudyBook.ISBN_8.isEmpty() && !cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) ||
                            (!cloudyBook.ISBN_13.isEmpty() && !cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13))) {
                            queryInUserBooks(cloudyBook);
                        } else {
                            mCallback.onQueryNoBarCodesDetected(mImageBitmap);
                        }
                    } else {
                        String message = "Bar code detection task failed.";
                        LogUtils.warn(TAG, message);
                        mCallback.onQueryActionComplete(message);
                    }
                });
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        }
    }

    private void scanImageForText() {

        LogUtils.debug(TAG, "++scanImageForText()");
        if (mImageBitmap != null) {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mImageBitmap);
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            com.google.android.gms.tasks.Task<FirebaseVisionText> result = detector.processImage(image).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    ArrayList<String> blocks = new ArrayList<>();
                    for (FirebaseVisionText.TextBlock textBlock : task.getResult().getTextBlocks()) {
                        String block = textBlock.getText().replace("\n", " ").replace("\r", " ");
                        blocks.add(block);
                    }

                    mCallback.onQueryTextResultsFound(blocks);
                } else {
                    String message = "Text detection task failed.";
                    LogUtils.warn(TAG, message);
                    mCallback.onQueryActionComplete(message);
                }
            });
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        }
    }

    protected void showManualDialog() {

        LogUtils.debug(TAG, "++showManualDialog()");
        mCallback.onQueryStarted();
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View promptView = layoutInflater.inflate(R.layout.dialog_search_manual, null);
        EditText editText = promptView.findViewById(R.id.manual_dialog_edit_search);
        RadioGroup radioGroup = promptView.findViewById(R.id.manual_dialog_radio_search);
        if (getActivity() != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setView(promptView);
            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {

                    CloudyBook cloudyBook = new CloudyBook();
                    switch (radioGroup.getCheckedRadioButtonId()) {
                        case R.id.manual_dialog_radio_isbn:
                            String value = editText.getText().toString();
                            if (value.length() == 8) {
                                cloudyBook.ISBN_8 = value;
                            } else if (value.length() == 13) {
                                cloudyBook.ISBN_13 = value;
                            }

                            if (!cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) ||
                                !cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
                                queryInUserBooks(cloudyBook);
                            } else {
                                String message = "Invalid ISBN value.";
                                mCallback.onQueryActionComplete(message);
                            }

                            break;
                        case R.id.manual_dialog_radio_title:
                            cloudyBook.Title = editText.getText().toString();
                            queryInUserBooks(cloudyBook);
                            break;
                        case R.id.manual_dialog_radio_lccn:
                            cloudyBook.LCCN = editText.getText().toString();
                            queryInUserBooks(cloudyBook);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    mCallback.onQueryActionComplete("");
                    dialog.cancel();
                });

            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else {
            String message = "Could not get activity object.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
        }
    }

    /*
        Retrieve data task; querying URLs for data
     */
    static class RetrieveBookDataTask extends AsyncTask<Void, Void, ArrayList<CloudyBook>> {

        private WeakReference<QueryFragment> mFragmentWeakReference;
        private CloudyBook mQueryForBook;

        RetrieveBookDataTask(QueryFragment context, CloudyBook queryForBook) {

            mFragmentWeakReference = new WeakReference<>(context);
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
            QueryFragment fragment = mFragmentWeakReference.get();
            if (fragment == null || fragment.isDetached()) {
                LogUtils.error(TAG, "Fragment is null or detached.");
                return;
            }

            fragment.parseFeed(cloudyBooks);
        }
    }
}
