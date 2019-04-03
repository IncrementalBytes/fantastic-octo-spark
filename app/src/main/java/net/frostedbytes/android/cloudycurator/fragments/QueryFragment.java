package net.frostedbytes.android.cloudycurator.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.frostedbytes.android.cloudycurator.BaseActivity;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class QueryFragment extends Fragment {

    private static final String TAG = BASE_TAG + QueryFragment.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public interface OnQueryListener {

        void onQueryFailure();
        void onQueryFoundBook(CloudyBook cloudyBook);
        void onQueryFoundUserBook(UserBook userBook);
        void onQueryInit(boolean isSuccessful);
    }

    private OnQueryListener mCallback;

    private ImageView mImageView;
    private Button mScanImageButton;

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
            throw new ClassCastException(String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
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
        View view = inflater.inflate(R.layout.fragment_add_book, container, false);

        EditText byISBNEdit = view.findViewById(R.id.add_edit_isbn);
        ImageView byISBNImage = view.findViewById(R.id.add_image_by_isbn);
        byISBNImage.setOnClickListener(v -> {

            if (!byISBNEdit.getText().toString().isEmpty()) {
                UserBook userBook = new UserBook();
                userBook.ISBN = byISBNEdit.getText().toString();
                queryForUserBook(userBook);
            }
        });

        EditText byTitleEdit = view.findViewById(R.id.add_edit_title);
        ImageView byTitleImage = view.findViewById(R.id.add_image_by_title);
        byTitleImage.setOnClickListener(v -> {

            if (!byTitleEdit.getText().toString().isEmpty()) {
                UserBook userBook = new UserBook();
                userBook.Title = byTitleEdit.getText().toString();
                queryForUserBook(userBook);
            }
        });

        mImageView = view.findViewById(R.id.add_image_preview);
        Button takeButton = view.findViewById(R.id.add_button_take);
        takeButton.setOnClickListener(v -> takePictureIntent());
        mScanImageButton = view.findViewById(R.id.add_button_scan);
        mScanImageButton.setEnabled(false);
        mScanImageButton.setOnClickListener(v -> scanImageForISBN());
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
            //Bundle extras = data.getExtras();
            //mImageBitmap = (Bitmap) extras.get("data");
            File f = new File("/storage/self/primary/DCIM/Camera", "20190327_151502.jpg");
            try {
                mImageBitmap = BitmapFactory.decodeStream(new FileInputStream(f));
                int height = Math.round(getActivity().getResources().getDimension(R.dimen.image_height_thumbnail));
                int width =  Math.round(getActivity().getResources().getDimension(R.dimen.image_width_thumbnail));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
                mImageView.setLayoutParams(layoutParams);
                mImageView.setImageBitmap(mImageBitmap);
                mImageView.setVisibility(View.VISIBLE);
                mScanImageButton.setEnabled(true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        Private Method(s)
     */
    private void parseFeed(CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++parseFeed(%s)", cloudyBook.toString());
        if (!cloudyBook.ISBN.isEmpty() && !cloudyBook.ISBN.equals(BaseActivity.DEFAULT_ISBN)) {
            String queryPath = PathUtils.combine(CloudyBook.ROOT, cloudyBook.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).set(cloudyBook, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    LogUtils.debug(TAG, "Successfully added: %s", cloudyBook.toString());
                    mCallback.onQueryFoundBook(cloudyBook);
                })
                .addOnFailureListener(e -> {
                    LogUtils.warn(TAG, "Failed to added: %s", cloudyBook.toString());
                    e.printStackTrace();
                    // TODO: add empty object in cloud for manual import?
                });
        } else {
            LogUtils.error(TAG, "Parsing feed failed.");
            // TODO: add empty object in cloud for manual import?
        }
    }

    private void queryForBook(CloudyBook bookQueryFor) {

        LogUtils.debug(TAG, "++queryForBook(%s)", bookQueryFor.toString());
        if (!bookQueryFor.Title.isEmpty()) {
            FirebaseFirestore.getInstance().collection(CloudyBook.ROOT).whereEqualTo("Title", bookQueryFor.Title).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        CloudyBook cloudyBook = null;
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            cloudyBook = document.toObject(CloudyBook.class);
                            cloudyBook.ISBN = document.getId();
                            mCallback.onQueryFoundBook(cloudyBook);
                        }

                        if (cloudyBook == null) {
                            LogUtils.warn(TAG, "No matches found: %s", bookQueryFor.toString());
                            queryGoogleBookService(bookQueryFor);
                        } else {
                            LogUtils.debug(TAG, "Document not found: %s", bookQueryFor.toString());
                            mCallback.onQueryFailure();
                        }
                    } else {
                        if (task.getException() != null) {
                            LogUtils.debug(TAG, "Query failed: %s where Title == %s", CloudyBook.ROOT, bookQueryFor.Title);
                            task.getException().printStackTrace();
                        }

                    }
                });
        } else if (!bookQueryFor.ISBN.isEmpty() && !bookQueryFor.ISBN.equals(BaseActivity.DEFAULT_ISBN)) {
            String queryPath = PathUtils.combine(CloudyBook.ROOT, bookQueryFor.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
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
            LogUtils.error(TAG, "Cannot search; unexpected book: %s", bookQueryFor.toString());
            mCallback.onQueryFailure();
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
        if (!cloudyBook.ISBN.isEmpty()) {
            searchParams = String.format(Locale.ENGLISH, "isbn:%s", cloudyBook.ISBN);
        } else {
            searchParams = "intitle:" + "\"" + cloudyBook.Title + "\"";
        }

        if (!searchParams.isEmpty()) {
            new RetrieveISBNDataTask(
                this,
                searchParams,
                getString(R.string.app_name),
                getString(R.string.google_books_key)).execute(getActivity().getContentResolver());
        } else {
            LogUtils.debug(TAG, "Cannot search; unexpected search parameters: %s", cloudyBook.toString());
            mCallback.onQueryFailure();
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
                        List<FirebaseVisionBarcode> codes = task.getResult();
                        for (FirebaseVisionBarcode barcode : codes) {
                            int valueType = barcode.getValueType();
                            switch (valueType) {
                                case FirebaseVisionBarcode.TYPE_ISBN:
                                    LogUtils.debug(TAG, "Found barcode: %s", barcode.getDisplayValue());
                                    UserBook userBook = new UserBook();
                                    userBook.ISBN = barcode.getDisplayValue();
                                    queryForUserBook(userBook);
                                    break;
                                default:
                                    LogUtils.warn(TAG, "Unexpected barcode: %s", barcode.getDisplayValue());
                                    break;
                            }
                        }
                    } else {
                        LogUtils.warn(TAG, "Could not detect bar code in image.");
                        mCallback.onQueryFailure();
                    }
                });
        } else {
            LogUtils.debug(TAG, "Image not loaded!");
        }
    }

    private void takePictureIntent() {

        LogUtils.debug(TAG, "++dispatchTakePictureIntent()");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            LogUtils.warn(TAG, "Unable to create take picture intent.");
        }
    }

    /*
        Retrieve data task; querying URLs for ISBN data
     */
    static class RetrieveISBNDataTask extends AsyncTask<Object, Void, CloudyBook> {

        private WeakReference<QueryFragment> mFragmentWeakReference;
        private String mSearchParam;
        private String mAppName;
        private String mAPIKey;

        RetrieveISBNDataTask(QueryFragment context, String searchParam, String appName, String apiKey) {

            mFragmentWeakReference = new WeakReference<>(context);
            mSearchParam = searchParam;
            mAppName = appName;
            mAPIKey = apiKey;
        }

        protected CloudyBook doInBackground(Object... params) {

            ContentResolver contentResolver = (ContentResolver) params[0];
            if (contentResolver == null) {
                LogUtils.warn(TAG, "ContentResolver unexpected.");
                return new CloudyBook();
            }

            try {
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

                // set up Books client
                final Books books = new Books.Builder(new com.google.api.client.http.javanet.NetHttpTransport(), jsonFactory, null)
                    .setApplicationName(mAppName)
                    .setGoogleClientRequestInitializer(new BooksRequestInitializer(mAPIKey))
                    .build();

                LogUtils.debug(TAG, "Query: [" + mSearchParam + "]");
                Books.Volumes.List volumesList = books.volumes().list(mSearchParam);

                Volumes volumes = volumesList.execute();
                if (volumes.getTotalItems() == 0 || volumes.getItems() == null) {
                    LogUtils.debug(TAG, "No matches found.");
                    return new CloudyBook();
                }

                // TODO: handle multiple
                Volume volume = volumes.getItems().get(0);
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
                return cloudyBook;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return new CloudyBook();
            }
        }

        protected void onPostExecute(CloudyBook cloudyBook) {

            LogUtils.debug(TAG, "++onPostExecute(%s)", cloudyBook.toString());
            QueryFragment fragment = mFragmentWeakReference.get();
            if (fragment == null || fragment.isDetached()) {
                LogUtils.error(TAG, "Fragment is null or detached.");
                return;
            }

            fragment.parseFeed(cloudyBook);
        }
    }
}
