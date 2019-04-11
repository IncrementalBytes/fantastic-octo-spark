package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class QueryFragment extends Fragment {

    private static final String TAG = BASE_TAG + QueryFragment.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public interface OnQueryListener {

        void onQueryCancelled();

        void onQueryFailure(String message);

        void onQueryFoundBook(CloudyBook cloudyBook);

        void onQueryFoundMultipleBooks(ArrayList<CloudyBook> cloudyBooks);

        void onQueryFoundUserBook(UserBook userBook);

        void onQueryInit(boolean isSuccessful);

        void onQueryNoBarcode(String message);

        void onQueryNoResultsFound();

        void onQueryStarted();

        void onQueryTakePicture();
    }

    private OnQueryListener mCallback;

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
                String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
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

        CardView titleCard = view.findViewById(R.id.book_query_card_title);
        titleCard.setOnClickListener(v -> showInputDialog(R.string.search_title_hint));
        CardView isbnCard = view.findViewById(R.id.book_query_card_isbn);
        isbnCard.setOnClickListener(v -> showInputDialog(R.string.search_isbn_hint));
        CardView scanCard = view.findViewById(R.id.book_query_card_photo);
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                scanCard.setOnClickListener(v -> mCallback.onQueryTakePicture());
            } else {
                LogUtils.warn(TAG, "Camera feature is not available; disabling camera.");
                scanCard.setEnabled(false);
            }
        } else {
            LogUtils.warn(TAG, "Could not get package manager information from activity; disabling camera.");
            scanCard.setEnabled(false);
        }

        mCallback.onQueryInit(true);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mUserBookList = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        LogUtils.debug(TAG, "++onActivityResult(%d, %d, Intent)", requestCode, resultCode);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                if (BuildConfig.DEBUG) {
                    File f = new File(getString(R.string.debug_path), "20190408_233337.jpg");
                    try {
                        mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    mImageBitmap = (Bitmap) extras.get("data");
                }

                if (mImageBitmap != null) {
                    Bitmap emptyBitmap = Bitmap.createBitmap(
                        mImageBitmap.getWidth(),
                        mImageBitmap.getHeight(),
                        mImageBitmap.getConfig());
                    if (!mImageBitmap.sameAs(emptyBitmap)) {
                        showInputDialog(R.string.search_image_hint);
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
        } else {
            String message = String.format(Locale.ENGLISH, "Unexpected result code: %d", resultCode);
            LogUtils.error(TAG, message);
            mCallback.onQueryFailure(message);
        }
    }

    public void takePictureIntent() {

        LogUtils.debug(TAG, "++takePictureIntent()");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getActivity() != null) {
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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
    private void parseFeed(ArrayList<CloudyBook> cloudyBooks) {

        LogUtils.debug(TAG, "++parseFeed(%d)", cloudyBooks.size());
        if (cloudyBooks.size() == 1) {
            String queryPath = PathUtils.combine(CloudyBook.ROOT, cloudyBooks.get(0).ISBN);
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

    private void queryForBook(CloudyBook bookQueryFor) {

        LogUtils.debug(TAG, "++queryForBook(%s)", bookQueryFor.toString());
        if (!bookQueryFor.Title.isEmpty()) {
            FirebaseFirestore.getInstance().collection(CloudyBook.ROOT).whereEqualTo("Title", bookQueryFor.Title).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        CloudyBook cloudyBook = null;
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            cloudyBook = document.toObject(CloudyBook.class);
                            if (cloudyBook != null) {
                                cloudyBook.ISBN = document.getId();
                                mCallback.onQueryFoundBook(cloudyBook);
                            } else {
                                LogUtils.warn(TAG, "Failed to convert document into CloudyBook: %s", document.getId());
                            }
                        }

                        if (cloudyBook == null) {
                            LogUtils.warn(TAG, "No matches found: %s", bookQueryFor.toString());
                            queryGoogleBookService(bookQueryFor);
                        }
                    } else {
                        String message = String.format(
                            Locale.ENGLISH,
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
        } else if (!bookQueryFor.ISBN.isEmpty() && !bookQueryFor.ISBN.equals(BaseActivity.DEFAULT_ISBN)) {
            String queryPath = PathUtils.combine(CloudyBook.ROOT, bookQueryFor.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        CloudyBook cloudyBook = document.toObject(CloudyBook.class);
                        if (cloudyBook != null) {
                            cloudyBook.ISBN = document.getId();
                            mCallback.onQueryFoundBook(cloudyBook);
                        } else {
                            LogUtils.warn(TAG, "Unable to convert cloudy book: %s", queryPath);
                            queryGoogleBookService(bookQueryFor);
                        }
                    } else {
                        LogUtils.debug(TAG, "Query failed: %s", queryPath);
                        queryGoogleBookService(bookQueryFor);
                    }
                });
        } else {
            String message = String.format(Locale.ENGLISH, "Cannot search; unexpected book: %s", bookQueryFor.toString());
            LogUtils.error(TAG, message);
            mCallback.onQueryFailure(message);
        }
    }

    private void queryForUserBook(UserBook userBookQueryFor) {

        LogUtils.debug(TAG, "++queryForUserBook(%s)", userBookQueryFor.toString());
        UserBook foundBook = null;
        for (UserBook userBook : mUserBookList) {
            if (userBook.Title.equals(userBookQueryFor.Title) || userBook.ISBN.equals(userBookQueryFor.ISBN)) {
                foundBook = userBook;
                break;
            }
        }

        if (foundBook != null) {
            mCallback.onQueryFoundUserBook(foundBook);
        } else {
            LogUtils.debug(TAG, "Did not find %s in user's book list.", userBookQueryFor.toString());
            CloudyBook cloudyBook = new CloudyBook();
            cloudyBook.ISBN = userBookQueryFor.ISBN;
            cloudyBook.Title = userBookQueryFor.Title;
            queryForBook(cloudyBook);
        }
    }

    private void queryGoogleBookService(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++queryGoogleBookService(%s)", cloudyBook.toString());
        String searchParams;
        if (!cloudyBook.ISBN.isEmpty() && !cloudyBook.ISBN.equalsIgnoreCase(BaseActivity.DEFAULT_ISBN)) {
            searchParams = String.format(Locale.ENGLISH, "isbn:%s", cloudyBook.ISBN);
        } else {
            searchParams = "intitle:" + "\"" + cloudyBook.Title + "\"";
        }

        if (!searchParams.isEmpty()) {
            if (getActivity() != null) {
                new RetrieveISBNDataTask(
                    this,
                    searchParams,
                    getActivity().getApplicationContext().getPackageManager(),
                    getString(R.string.google_books_key)).execute();
            } else {
                String message = "Could not get activity's content resolver.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryFailure(message);
            }
        } else {
            String message = String.format(Locale.ENGLISH, "Cannot search; unexpected search parameters: %s", cloudyBook.toString());
            LogUtils.debug(TAG, message);
            mCallback.onQueryFailure(message);
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
                .addOnSuccessListener(firebaseVisionBarcodes -> {

                    if (firebaseVisionBarcodes != null && firebaseVisionBarcodes.size() > 0) {
                        int booksQueried = 0;
                        for (FirebaseVisionBarcode barcode : firebaseVisionBarcodes) {
                            int valueType = barcode.getValueType();
                            switch (valueType) {
                                case FirebaseVisionBarcode.TYPE_ISBN:
                                    LogUtils.debug(TAG, "Found bar code: %s", barcode.getDisplayValue());
                                    booksQueried++;
                                    UserBook userBook = new UserBook();
                                    userBook.ISBN = barcode.getDisplayValue();
                                    queryForUserBook(userBook);
                                    break;
                                default:
                                    LogUtils.warn(TAG, "Unexpected bar code: %s", barcode.getDisplayValue());
                                    break;
                            }
                        }

                        if (booksQueried < 1) {
                            String message = "Image processed, but no bar codes found.";
                            LogUtils.warn(TAG, message);
                            mCallback.onQueryNoBarcode(message);
                        }
                    } else {
                        String message = "No bar codes found.";
                        LogUtils.warn(TAG, message);
                        mCallback.onQueryNoBarcode(message);
                    }
                }).addOnFailureListener(e -> {

                    String message = "Could not detect bar code in image.";
                    LogUtils.warn(TAG, message);
                    mCallback.onQueryNoBarcode(message);
                });
        } else {
            String message = "Image not loaded.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryNoBarcode(message);
        }
    }

    protected void showInputDialog(int hintResourceId) {

        LogUtils.debug(TAG, "++showInputDialog(%s)", getString(hintResourceId));
        mCallback.onQueryStarted();
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View promptView = layoutInflater.inflate(R.layout.dialog_search_input, null);
        if (getActivity() != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setView(promptView);

            final TextView textView = promptView.findViewById(R.id.dialog_text_search);
            textView.setText(getString(R.string.search));
            final EditText editText = promptView.findViewById(R.id.dialog_edit_search);
            editText.setHint(getString(hintResourceId));
            final ImageView imageView = promptView.findViewById(R.id.dialog_image_search);

            // adjust layout
            if (hintResourceId == R.string.search_image_hint) {
                editText.setVisibility(View.INVISIBLE);
                imageView.setImageBitmap(mImageBitmap);
            } else {
                ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(0, 0);
                imageView.setLayoutParams(layoutParams);
                imageView.setVisibility(View.INVISIBLE);
            }

            alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {

                    UserBook userBook = new UserBook();
                    switch (hintResourceId) {
                        case R.string.search_isbn_hint:
                            userBook.ISBN = editText.getText().toString();
                            queryForUserBook(userBook);
                            break;
                        case R.string.search_title_hint:
                            userBook.Title = editText.getText().toString();
                            queryForUserBook(userBook);
                            break;
                        case R.string.search_image_hint:
                            scanImageForISBN();
                            break;
                        default:
                            mCallback.onQueryFailure(String.format(Locale.ENGLISH, "Unknown search term: %d", hintResourceId));
                            break;
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
        Retrieve data task; querying URLs for ISBN data
     */
    static class RetrieveISBNDataTask extends AsyncTask<Void, Void, ArrayList<CloudyBook>> {

        private WeakReference<QueryFragment> mFragmentWeakReference;
        private String mSearchParam;
        private PackageManager mPackageManager;
        private String mAPIKey;
        private ArrayList<CloudyBook> mCloudyBooks;

        RetrieveISBNDataTask(QueryFragment context, String searchParam, PackageManager packageManager, String apiKey) {

            mFragmentWeakReference = new WeakReference<>(context);
            mPackageManager = packageManager;
            mSearchParam = searchParam;
            mAPIKey = apiKey;
        }

        protected ArrayList<CloudyBook> doInBackground(Void... params) {

            mCloudyBooks = new ArrayList<>();
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

                LogUtils.debug(TAG, "Query: [" + mSearchParam + "]");
                Books.Volumes.List volumesList = books.volumes().list(mSearchParam);

                Volumes volumes = volumesList.execute();
                if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
                    LogUtils.debug(TAG, "No matches found.");
                    return new ArrayList<>();
                }

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
                        if (isbn.getType().equalsIgnoreCase("ISBN_13")) {
                            cloudyBook.ISBN = isbn.getIdentifier();
                        }
                    }

                    cloudyBook.VolumeId = volume.getId();
                    mCloudyBooks.add(cloudyBook);
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
