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

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.Book;
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class QueryBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + QueryBookFragment.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public interface OnQueryBookListener {

        void onQueryBookInit(boolean isSuccessful);
        void onQueryBookFailure();
        void onQueryBookFound(UserBook userBook);
    }

    private OnQueryBookListener mCallback;

    private ImageView mImageView;
    private Button mScanImageButton;

    private Bitmap mImageBitmap;
    private ArrayList<UserBook> mUserBookList;

    public static QueryBookFragment newInstance(ArrayList<UserBook> userBookList) {

        LogUtils.debug(TAG, "++newInstance()");
        QueryBookFragment fragment = new QueryBookFragment();
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
            mCallback = (OnQueryBookListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUserBookList = arguments.getParcelableArrayList(BaseActivity.ARG_USER_BOOK_LIST);
        } else {
            LogUtils.warn(TAG, "Arguments were null.");
            mCallback.onQueryBookInit(false);
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
        mCallback.onQueryBookInit(true);
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
                LogUtils.debug(TAG, e.getMessage());
            }
        }
    }

    /*
        Private Method(s)
     */
    private void parseFeed(String feed) {

        LogUtils.debug(TAG, "++parseFeed()");

        // TODO: parse the feed for information
        LogUtils.debug(TAG, "%s", feed);

        UserBook userBook = new UserBook();
        userBook.AddedDate = userBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();
        userBook.Author = "";
        userBook.HasRead = false;
        userBook.IsOwned = false;
        userBook.Title = "";

        if (!userBook.ISBN.isEmpty() && !userBook.ISBN.equals(BaseActivity.DEFAULT_ISBN)) {
            String queryPath = PathUtils.combine(Book.ROOT, userBook.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).set(userBook)
                .addOnSuccessListener(aVoid -> {
                    LogUtils.debug(TAG, "Successfully added: %s", userBook.toString());
                    mCallback.onQueryBookFound(userBook);
                })
                .addOnFailureListener(e -> {
                    LogUtils.warn(TAG, "Failed to added: %s", userBook.toString());
                    LogUtils.debug(TAG, e.getMessage());
                    // TODO: add empty object in cloud for manual import?
                });
        } else {
            LogUtils.error(TAG, "Parsing feed failed.");
            // TODO: add empty object in cloud for manual import?
        }
    }

    private void queryForBook(UserBook userBook) {

        LogUtils.debug(TAG, "++queryForBook(%s)", userBook.toString());
        if ((userBook.ISBN.isEmpty() || userBook.ISBN.equals(BaseActivity.DEFAULT_ISBN)) && !userBook.Title.isEmpty()) {
            //FirebaseFirestore.getInstance().collection(Book.ROOT).whereEqualTo("Title", userBook.Title).get()
            FirebaseFirestore.getInstance().collection(Book.ROOT).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    UserBook book = null;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        book = document.toObject(UserBook.class);
                        book.ISBN = document.getId();
                        if (book.Title.equals(userBook.Title)) {
                            mCallback.onQueryBookFound(book);
                        }
                    }

                    if (book == null) {
                        LogUtils.warn(TAG, "Unable to convert document: %s where Title == %s", Book.ROOT, userBook.Title);
                        mCallback.onQueryBookFailure();
                    }
                }).addOnFailureListener(e -> {

                    LogUtils.debug(TAG, "Query failed: %s where Title == %s", Book.ROOT, userBook.Title);
                    mCallback.onQueryBookFailure();
                });

        } else if (!userBook.ISBN.isEmpty() && userBook.ISBN.equals(BaseActivity.DEFAULT_ISBN)) {
            String queryPath = PathUtils.combine(Book.ROOT, userBook.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).get().addOnSuccessListener(documentSnapshot -> {

                UserBook book = documentSnapshot.toObject(UserBook.class);
                if (book != null) {
                    book.ISBN = documentSnapshot.getId();
                    mCallback.onQueryBookFound(book);
                } else {
                    LogUtils.warn(TAG, "Unable to convert document to UserBook: %s", queryPath);
                    queryISBNService(userBook.ISBN);
                }
            }).addOnFailureListener(e -> {

                LogUtils.debug(TAG, "Query failed: %s where Title == %s", queryPath, userBook.Title);
                queryISBNService(userBook.ISBN);
            });
        } else {
            LogUtils.error(TAG, "Malformed user book; cannot search.");
        }
    }

    private void queryForUserBook(UserBook userBook) {

        LogUtils.debug(TAG, "++queryForUserBook(%s)", userBook.toString());
        UserBook foundBook = null;
        for (UserBook book : mUserBookList) {
            if (book.Title.equals(userBook.Title) || book.ISBN.equals(userBook.ISBN)) {
                foundBook = book;
                break;
            }
        }

        if (foundBook != null) {
            mCallback.onQueryBookFound(foundBook);
        } else {
            LogUtils.debug(TAG, "Did not find %s in user's book list.", userBook.Title);
            queryForBook(userBook);
        }
    }

    private void queryISBNService(String isbn) {

        LogUtils.debug(TAG, "++queryISBNService(%s)", isbn);
        new RetrieveISBNDataTask(this).execute(
            getActivity().getContentResolver(),
            isbn);
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
            Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(codes -> {
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
                })
                .addOnFailureListener(e -> {

                    LogUtils.warn(TAG, "Could not detect bar code in image.");
                    mCallback.onQueryBookFailure();
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
    static class RetrieveISBNDataTask extends AsyncTask<Object, Void, String> {

        private WeakReference<QueryBookFragment> mFragmentWeakReference;

        RetrieveISBNDataTask(QueryBookFragment context) {

            mFragmentWeakReference = new WeakReference<>(context);
        }

        protected String doInBackground(Object... params) {

            ContentResolver contentResolver = (ContentResolver) params[0];
            if (contentResolver == null) {
                LogUtils.warn(TAG, "ContentResolver unexpected.");
                return "";
            }

            try {
                URL url = new URL(String.format(Locale.ENGLISH, "https://isbnsearch.org/isbn/%s", params[1].toString()));
                LogUtils.debug(TAG, "Looking for %s", url);
//                HttpURLConnection con = (HttpURLConnection) url.openConnection();
//                con.setRequestMethod("GET");
//
//                con.setConnectTimeout(5000);
//                con.setReadTimeout(5000);
//
//                StringBuilder fullResponseBuilder = new StringBuilder();
//                fullResponseBuilder.append(con.getResponseCode())
//                    .append(" ")
//                    .append(con.getResponseMessage())
//                    .append("\n");
//
//                con.getHeaderFields()
//                    .entrySet()
//                    .stream()
//                    .filter(entry -> entry.getKey() != null)
//                    .forEach(entry -> {
//
//                        fullResponseBuilder.append(entry.getKey())
//                            .append(": ");
//
//                        List<String> headerValues = entry.getValue();
//                        Iterator<String> it = headerValues.iterator();
//                        if (it.hasNext()) {
//                            fullResponseBuilder.append(it.next());
//
//                            while (it.hasNext()) {
//                                fullResponseBuilder.append(", ")
//                                    .append(it.next());
//                            }
//                        }
//
//                        fullResponseBuilder.append("\n");
//                    });
//
//                Reader streamReader = null;
//
//                if (con.getResponseCode() > 299) {
//                    streamReader = new InputStreamReader(con.getErrorStream());
//                } else {
//                    streamReader = new InputStreamReader(con.getInputStream());
//                }
//
//                BufferedReader in = new BufferedReader(streamReader);
//                String inputLine;
//                StringBuilder content = new StringBuilder();
//                while ((inputLine = in.readLine()) != null) {
//                    content.append(inputLine);
//                }
//
//                in.close();
//                fullResponseBuilder.append("Response: ").append(content);
//                con.disconnect();
//                return fullResponseBuilder.toString();
                return "Success";
            } catch (Exception e) {
                LogUtils.debug(TAG, e.getMessage());
                return "Failure";
            }
        }

        protected void onPostExecute(String feed) {

            LogUtils.debug(TAG, "++onPostExecute(String)");
            QueryBookFragment fragment = mFragmentWeakReference.get();
            if (fragment == null || fragment.isDetached()) {
                LogUtils.error(TAG, "Fragment is null or detached.");
                return;
            }

            fragment.parseFeed(feed);
        }
    }
}
