package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;
import com.google.common.io.BaseEncoding;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        for (CloudyBook book : mCloudyBookList) {
            if (book.isPartiallyEqual(cloudyBook)) {
                foundBook = book;
                break;
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
                new RetrieveBookDataTask(
                    this,
                    cloudyBook,
                    getActivity().getApplicationContext().getPackageManager(),
                    getString(R.string.google_books_key)).execute();
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

        private String mAPIKey;
        private CloudyBook mCloudyBook;
        private ArrayList<CloudyBook> mCloudyBooks;
        private WeakReference<QueryFragment> mFragmentWeakReference;
        private PackageManager mPackageManager;

        RetrieveBookDataTask(QueryFragment context, CloudyBook cloudyBook, PackageManager packageManager, String apiKey) {

            mAPIKey = apiKey;
            mCloudyBook = cloudyBook;
            mCloudyBooks = new ArrayList<>();
            mFragmentWeakReference = new WeakReference<>(context);
            mPackageManager = packageManager;
        }

        protected ArrayList<CloudyBook> doInBackground(Void... params) {

            try {
                final Books books = new Books.Builder(
                    new com.google.api.client.http.javanet.NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> {

                        String SHA1 = getSHA1();
                        request.getHeaders().set("X-Android-Package", BuildConfig.APPLICATION_ID);
                        request.getHeaders().set("X-Android-Cert", SHA1);
                    })
                    .setApplicationName(BuildConfig.APPLICATION_ID)
                    .setGoogleClientRequestInitializer(new BooksRequestInitializer(mAPIKey))
                    .build();

                String searchParam = null;
                if (!mCloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8)) {
                    searchParam = String.format(Locale.US, "isbn:%s", mCloudyBook.ISBN_8);
                } else if (!mCloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
                    searchParam = String.format(Locale.US, "isbn:%s", mCloudyBook.ISBN_13);
                } else if (!mCloudyBook.Title.isEmpty()){
                    searchParam = "intitle:" + "\"" + mCloudyBook.Title + "\"";
                } else if (!mCloudyBook.LCCN.equals(BaseActivity.DEFAULT_LCCN)) {
                    searchParam = String.format(Locale.US, "lccn:%s", mCloudyBook.LCCN);
                }

                if (searchParam != null && !searchParam.isEmpty()) {
                    LogUtils.debug(TAG, "Query: %s", searchParam);
                    Books.Volumes.List volumesList = books.volumes().list(searchParam);
                    Volumes volumes = volumesList.execute();
                    if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
                        LogUtils.debug(TAG, "No matches found.");
                    } else {
                        // TODO: what if a search returns multiple copies, but some data is missing (e.g. Authors blank)?
                        for (Volume volume : volumes.getItems()) {
                            Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();
                            if (!volumeInfo.getPrintType().equals("BOOK")) {
                                continue;
                            }

                            CloudyBook cloudyBook = new CloudyBook();
                            cloudyBook.Title = volumeInfo.getTitle();
                            java.util.List<String> authors = volumeInfo.getAuthors();
                            if (authors != null && !authors.isEmpty()) {
                                cloudyBook.Authors.addAll(authors);
                            }

                            java.util.List<String> categories = volumeInfo.getCategories();
                            if (categories != null && !categories.isEmpty()) {
                                cloudyBook.Categories.addAll(categories);
                            }

                            cloudyBook.PublishedDate = volumeInfo.getPublishedDate();
                            cloudyBook.Publisher = volumeInfo.getPublisher();
                            for (Volume.VolumeInfo.IndustryIdentifiers isbn : volumeInfo.getIndustryIdentifiers()) {
                                if (isbn.getType().equalsIgnoreCase("ISBN_10")) {
                                    cloudyBook.ISBN_8 = isbn.getIdentifier();
                                } else if (isbn.getType().equalsIgnoreCase("ISBN_13")) {
                                    cloudyBook.ISBN_13 = isbn.getIdentifier();
                                }
                            }

                            cloudyBook.VolumeId = volume.getId();
                            mCloudyBooks.add(cloudyBook);
                        }
                    }
                } else {
                    LogUtils.error(TAG, "Unexpected search parameters.");
                }

                return mCloudyBooks;
            } catch (Exception e) {
                LogUtils.warn(TAG, "Exception when querying Book API service.");
                Crashlytics.logException(e);
                return new ArrayList<>();
            }
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

        private String getSHA1() {

            try {
                Signature[] signatures = mPackageManager.getPackageInfo(
                    BuildConfig.APPLICATION_ID,
                    PackageManager.GET_SIGNATURES).signatures;
                if (signatures.length > 0) {
                    MessageDigest md;
                    md = MessageDigest.getInstance("SHA-1");
                    md.update(signatures[0].toByteArray());
                    return BaseEncoding.base16().encode(md.digest());
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
