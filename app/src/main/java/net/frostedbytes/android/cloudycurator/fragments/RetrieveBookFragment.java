package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class RetrieveBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + RetrieveBookFragment.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;

    public interface OnRetrieveBookListener {

        void onRetrieveBookInit(boolean isSuccessful);
        void onBookFound();
    }

    private OnRetrieveBookListener mCallback;

    private ImageView mImageView;
    private EditText mIsbnEdit;

    private Bitmap mImageBitmap;

    public static RetrieveBookFragment newInstance() {

        LogUtils.debug(TAG, "++newInstance()");
        RetrieveBookFragment fragment = new RetrieveBookFragment();
        Bundle args = new Bundle();
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
            mCallback = (OnRetrieveBookListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            // TODO: handle arguments
        } else {
            LogUtils.warn(TAG, "Arguments were null.");
            mCallback.onRetrieveBookInit(false);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        View view = inflater.inflate(R.layout.fragment_retrieve, container, false);

        mImageView = view.findViewById(R.id.main_image_preview);
        Button takeButton = view.findViewById(R.id.main_button_take);
        takeButton.setOnClickListener(v -> takePictureIntent());
        Button retrieveButton = view.findViewById(R.id.main_button_retrieve);
        retrieveButton.setOnClickListener(v -> retrieveBookFromInput());
        mIsbnEdit = view.findViewById(R.id.main_edit_isbn);
        mIsbnEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {

                if (!mIsbnEdit.getText().toString().equals("")) {
                    retrieveButton.setEnabled(true);
                } else {
                    retrieveButton.setEnabled(false);
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
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
            } catch (FileNotFoundException e) {
                LogUtils.debug(TAG, e.getMessage());
            }
        }
    }

    /*
        Private Method(s)
     */
    private void retrieveBookFromInput() {

        LogUtils.debug(TAG, "++retrieveBookFromInput()");
        if (mImageView.getVisibility() == View.INVISIBLE) {
            LogUtils.debug(TAG, "%s", mIsbnEdit.getText().toString());
            new RetrieveISBNDataTask().execute(
                String.format(
                    Locale.ENGLISH,
                    "https://isbnsearch.org/isbn/%s",
                    mIsbnEdit.getText().toString()));
        } else {
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
                    .addOnSuccessListener(barcodes -> {
                        for (FirebaseVisionBarcode barcode : barcodes) {
                            int valueType = barcode.getValueType();
                            switch (valueType) {
                                case FirebaseVisionBarcode.TYPE_ISBN:
                                    LogUtils.debug(TAG, "%s", barcode.getDisplayValue());
                                    new RetrieveISBNDataTask().execute(
                                        String.format(
                                            Locale.ENGLISH,
                                            "https://isbnsearch.org/isbn/%s",
                                            barcode.getDisplayValue()));
                                    break;
                                default:
                                    LogUtils.warn(TAG, "Unexpected barcode: %s", barcode.getDisplayValue());
                                    break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> LogUtils.warn(TAG, "Could not detect bar code in image."));
            } else {
                LogUtils.debug(TAG, "Image not loaded!");
            }
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
    static class RetrieveISBNDataTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urls) {

            try {
                URL url = new URL(urls[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                StringBuilder fullResponseBuilder = new StringBuilder();
                fullResponseBuilder.append(con.getResponseCode())
                    .append(" ")
                    .append(con.getResponseMessage())
                    .append("\n");

                con.getHeaderFields()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null)
                    .forEach(entry -> {

                        fullResponseBuilder.append(entry.getKey())
                            .append(": ");

                        List<String> headerValues = entry.getValue();
                        Iterator<String> it = headerValues.iterator();
                        if (it.hasNext()) {
                            fullResponseBuilder.append(it.next());

                            while (it.hasNext()) {
                                fullResponseBuilder.append(", ")
                                    .append(it.next());
                            }
                        }

                        fullResponseBuilder.append("\n");
                    });

                Reader streamReader = null;

                if (con.getResponseCode() > 299) {
                    streamReader = new InputStreamReader(con.getErrorStream());
                } else {
                    streamReader = new InputStreamReader(con.getInputStream());
                }

                BufferedReader in = new BufferedReader(streamReader);
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                fullResponseBuilder.append("Response: ").append(content);
                con.disconnect();
                return fullResponseBuilder.toString();
            } catch (Exception e) {
                LogUtils.debug(TAG, e.getMessage());
                return null;
            }
        }

        protected void onPostExecute(String feed) {

            LogUtils.debug(TAG, "++onPostExecute(String)");
            LogUtils.debug(TAG, feed);
        }
    }
}
