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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
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
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        void onQueryCancelled();

        void onQueryFailure(String message);

        void onQueryFeatureNotAvailable(String message);

        void onQueryFoundBook(CloudyBook cloudyBook);

        void onQueryFoundMultipleBooks(ArrayList<CloudyBook> cloudyBooks);

        void onQueryFoundUserBook(UserBook userBook);

        void onQueryInit(boolean isSuccessful);

        void onQueryNoBarcode(String message);

        void onQueryNoResultsFound();

        void onQueryStarted();

        void onQueryTakePicture();

        void onQueryTextResultsFound(ArrayList<String> textResults);
    }

    private OnQueryListener mCallback;

    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private ArrayList<UserBook> mUserBookList;

    public static QueryFragment newInstance(ArrayList<UserBook> userBookList) {

        LogUtils.debug(TAG, "++newInstance()");
        QueryFragment fragment = new QueryFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(BaseActivity.ARG_USER_BOOK_LIST, userBookList);
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
            mUserBookList = arguments.getParcelableArrayList(BaseActivity.ARG_USER_BOOK_LIST);
        } else {
            LogUtils.warn(TAG, "Arguments were null.");
            mCallback.onQueryInit(false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        View view = inflater.inflate(R.layout.fragment_book_query, container, false);

        CardView scanCard = view.findViewById(R.id.query_card_image);
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                scanCard.setOnClickListener(v -> mCallback.onQueryTakePicture());
            } else {
                LogUtils.warn(TAG, "Camera feature is not available; disabling camera.");
                scanCard.setEnabled(false);
            }
        } else {
            String message = "Camera not detected.";
            LogUtils.warn(TAG, message);
            scanCard.setEnabled(false);
            mCallback.onQueryFeatureNotAvailable(message);
        }

        CardView manualCard = view.findViewById(R.id.query_card_manual);
        manualCard.setOnClickListener(v -> showManualDialog());

        mCallback.onQueryInit(true);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mUserBookList = null;
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
                File f = new File(getString(R.string.debug_path), "20190408_233337.jpg");
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
                    scanImageForISBN();
                } else {
                    String message = "Image was empty.";
                    LogUtils.warn(TAG, message);
                    mCallback.onQueryNoBarcode(message);
                }
            } else {
                String message = "Bitmap was null.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryNoBarcode(message);
            }
        } else {
            String message = "Unexpected data from camera intent.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryNoBarcode(message);
        }
    }

    public void queryInUserBooks(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++queryInUserBooks(%s)", cloudyBook.toString());
        UserBook foundBook = null;
        for (UserBook userBook : mUserBookList) {
            if (userBook.Title.equals(cloudyBook.Title) ||
                (!userBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) && userBook.ISBN_8.equals(cloudyBook.ISBN_8)) ||
                (!userBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) && userBook.ISBN_13.equals(cloudyBook.ISBN_13)) ||
                (!userBook.LCCN.equals(BaseActivity.DEFAULT_LCCN) && userBook.LCCN.equals(cloudyBook.LCCN))) {
                foundBook = userBook;
                break;
            }
        }

        if (foundBook != null) {
            mCallback.onQueryFoundUserBook(foundBook);
        } else {
            LogUtils.debug(TAG, "Did not find %s in user's book list.", cloudyBook.toString());
            queryInCloudyBooks(cloudyBook);
        }
    }

    public void takePictureIntent() {

        LogUtils.debug(TAG, "++takePictureIntent()");
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
                    mCallback.onQueryFailure(message);
                }
            } else {
                String message = "Unable to create camera intent.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryFailure(message);
            }
        } else {
            String message = "Could not get activity object.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryFailure(message);
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
        if (cloudyBooks.size() == 1) {
            String queryPath = PathUtils.combine(CloudyBook.ROOT, cloudyBooks.get(0).VolumeId);
            FirebaseFirestore.getInstance().document(queryPath).set(cloudyBooks.get(0), SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        LogUtils.debug(TAG, "Successfully added: %s", cloudyBooks.get(0).toString());
                        mCallback.onQueryFoundBook(cloudyBooks.get(0));
                    } else {
                        LogUtils.warn(TAG, "Failed to added: %s", cloudyBooks.get(0).toString());
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }
                    }

                    // TODO: add empty object in cloud for manual import?
                });
        } else if (cloudyBooks.size() > 1) {
            mCallback.onQueryFoundMultipleBooks(cloudyBooks);
        } else {
            mCallback.onQueryNoResultsFound();
        }
    }

    private void queryInCloudyBooks(CloudyBook bookQueryFor) {

        LogUtils.debug(TAG, "++queryInCloudyBooks(%s)", bookQueryFor.toString());
        if (!bookQueryFor.Title.isEmpty()) {
            FirebaseFirestore.getInstance().collection(CloudyBook.ROOT).whereEqualTo("Title", bookQueryFor.Title).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        CloudyBook cloudyBook = null;
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            cloudyBook = document.toObject(CloudyBook.class);
                            if (cloudyBook != null) {
                                cloudyBook.VolumeId = document.getId();

                                // TODO: handle multiple documents
                                mCallback.onQueryFoundBook(cloudyBook);
                            }
                        }

                        if (cloudyBook == null) {
                            LogUtils.warn(TAG, "No matches found: %s", bookQueryFor.toString());
                            queryGoogleBookService(bookQueryFor);
                        }
                    } else {
                        String message = String.format(
                            Locale.US,
                            "Query failed: %s where Title == %s",
                            CloudyBook.ROOT,
                            bookQueryFor.Title);
                        LogUtils.warn(TAG, message);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }

                        mCallback.onQueryFailure(message);
                    }
                });
        } else if (!bookQueryFor.ISBN_8.contains(BaseActivity.DEFAULT_ISBN_8)) {
            FirebaseFirestore.getInstance().collection(CloudyBook.ROOT).whereEqualTo("ISBN_8", bookQueryFor.ISBN_8).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        CloudyBook cloudyBook = null;
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            cloudyBook = document.toObject(CloudyBook.class);
                            if (cloudyBook != null) {
                                cloudyBook.VolumeId = document.getId();

                                // TODO: handle multiple documents
                                mCallback.onQueryFoundBook(cloudyBook);
                            }
                        }

                        if (cloudyBook == null) {
                            LogUtils.warn(TAG, "No matches found: %s", bookQueryFor.toString());
                            queryGoogleBookService(bookQueryFor);
                        }
                    } else {
                        String message = String.format(
                            Locale.US,
                            "Query failed: %s where ISBN_8 == %s",
                            CloudyBook.ROOT,
                            bookQueryFor.ISBN_8);
                        LogUtils.warn(TAG, message);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }

                        mCallback.onQueryFailure(message);
                    }
                });
        } else if (!bookQueryFor.ISBN_13.contains(BaseActivity.DEFAULT_ISBN_13)) {
            FirebaseFirestore.getInstance().collection(CloudyBook.ROOT).whereEqualTo("ISBN_13", bookQueryFor.ISBN_13).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        CloudyBook cloudyBook = null;
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            cloudyBook = document.toObject(CloudyBook.class);
                            if (cloudyBook != null) {
                                cloudyBook.VolumeId = document.getId();

                                // TODO: handle multiple documents
                                mCallback.onQueryFoundBook(cloudyBook);
                            }
                        }

                        if (cloudyBook == null) {
                            LogUtils.warn(TAG, "No matches found: %s", bookQueryFor.toString());
                            queryGoogleBookService(bookQueryFor);
                        }
                    } else {
                        String message = String.format(
                            Locale.US,
                            "Query failed: %s where ISBN_13 == %s",
                            CloudyBook.ROOT,
                            bookQueryFor.ISBN_13);
                        LogUtils.warn(TAG, message);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }

                        mCallback.onQueryFailure(message);
                    }
                });
        } else if (!bookQueryFor.LCCN.equals(BaseActivity.DEFAULT_LCCN)) {
            FirebaseFirestore.getInstance().collection(CloudyBook.ROOT).whereEqualTo("LCCN", bookQueryFor.LCCN).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        CloudyBook cloudyBook = null;
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            cloudyBook = document.toObject(CloudyBook.class);
                            if (cloudyBook != null) {
                                cloudyBook.VolumeId = document.getId();
                                mCallback.onQueryFoundBook(cloudyBook);
                            }
                        }

                        if (cloudyBook == null) {
                            LogUtils.warn(TAG, "No matches found: %s", bookQueryFor.toString());
                            queryGoogleBookService(bookQueryFor);
                        }
                    } else {
                        String message = String.format(
                            Locale.US,
                            "Query failed: %s where LCCN == %s",
                            CloudyBook.ROOT,
                            bookQueryFor.LCCN);
                        LogUtils.warn(TAG, message);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }

                        mCallback.onQueryFailure(message);
                    }
                });
        } else {
            String message = String.format(Locale.US, "Cannot search; unexpected book: %s", bookQueryFor.toString());
            LogUtils.error(TAG, message);
            mCallback.onQueryFailure(message);
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
            mCallback.onQueryFailure(message);
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
                mCallback.onQueryFailure(message);
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

                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            CloudyBook cloudyBook = new CloudyBook();
                            for (FirebaseVisionBarcode barcode : task.getResult()) {
                                switch (barcode.getValueType()) {
                                    case FirebaseVisionBarcode.TYPE_ISBN:
                                        String barcodeValue = barcode.getDisplayValue();
                                        LogUtils.debug(TAG, "Found a bar code: %s", barcodeValue);
                                        if (barcodeValue != null && barcodeValue.length() == 8) {
                                            cloudyBook.ISBN_8 = barcodeValue;
                                        } else if (barcodeValue != null && barcodeValue.length() == 13) {
                                            cloudyBook.ISBN_13 = barcodeValue;
                                        }

                                        break;
                                    default:
                                        LogUtils.warn(TAG, "Unexpected bar code: %s", barcode.getDisplayValue());
                                        break;
                                }
                            }

                            if (!cloudyBook.ISBN_8.equals(BaseActivity.DEFAULT_ISBN_8) ||
                                !cloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
                                queryInUserBooks(cloudyBook);
                            } else {
                                scanImageForText();
                            }
                        } else {
                            scanImageForText();
                        }
                    } else {
                        String message = "Bar code detection task failed.";
                        LogUtils.warn(TAG, message);
                        mCallback.onQueryFailure(message);
                    }
                });
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryNoBarcode(message);
        }
    }

    private void scanImageForText() {

        LogUtils.debug(TAG, "++scanImageForText()");
        if (mImageBitmap != null) {
            FirebaseVisionBarcodeDetectorOptions options =
                new FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_EAN_8,
                        FirebaseVisionBarcode.FORMAT_EAN_13)
                    .build();
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
                    mCallback.onQueryFailure(message);
                }
            });
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryNoBarcode(message);
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
                                mCallback.onQueryFailure(message);
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
                    mCallback.onQueryCancelled();
                    dialog.cancel();
                });

            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else {
            String message = "Could not get activity object.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryFailure(message);
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
                    LogUtils.debug(TAG, "Query: [%s]", searchParam);
                    Books.Volumes.List volumesList = books.volumes().list(searchParam);
                    Volumes volumes = volumesList.execute();
                    if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
                        LogUtils.debug(TAG, "No matches found.");
                    } else {
                        for (Volume volume : volumes.getItems()) {
                            Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();
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

                            cloudyBook.CreatedDate = Calendar.getInstance().getTimeInMillis();
                            cloudyBook.PublishedDate = volumeInfo.getPublishedDate();
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
