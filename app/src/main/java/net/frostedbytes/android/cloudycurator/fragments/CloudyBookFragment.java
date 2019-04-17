package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;

import java.util.Calendar;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class CloudyBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + CloudyBookFragment.class.getSimpleName();

    public interface OnCloudyBookListener {

        void onCloudyBookActionComplete(String message);

        void onCloudyBookAddedToLibrary(CloudyBook cloudyBook);

        void onCloudyBookInit(boolean isSuccessful);

        void onCloudyBookRemoved(CloudyBook cloudyBook);

        void onCloudyBookStarted();

        void onCloudyBookUpdated(CloudyBook cloudyBook);
    }

    private OnCloudyBookListener mCallback;

    private CloudyBook mCloudyBook;
    private String mUserId;

    public static CloudyBookFragment newInstance(String userId, CloudyBook cloudyBook) {

        LogUtils.debug(TAG, "++newInstance()");
        CloudyBookFragment fragment = new CloudyBookFragment();
        Bundle args = new Bundle();
        args.putString(BaseActivity.ARG_USER_ID, userId);
        args.putParcelable(BaseActivity.ARG_CLOUDY_BOOK, cloudyBook);
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
            mCallback = (OnCloudyBookListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mCloudyBook = arguments.getParcelable(BaseActivity.ARG_CLOUDY_BOOK);
            mUserId = arguments.getString(BaseActivity.ARG_USER_ID);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_cloudy_book, container, false);
        TextView titleText = view.findViewById(R.id.cloudy_book_text_title_value);
        titleText.setText(mCloudyBook.Title);
        TextView authorsText = view.findViewById(R.id.cloudy_book_text_author_value);
        StringBuilder authorsList = new StringBuilder();
        for (String author : mCloudyBook.Authors) {
            authorsList.append(author);
            authorsList.append("\r\n");
        }

        if (!authorsList.toString().isEmpty()) {
            authorsText.setText(authorsList.deleteCharAt(authorsList.length() - 2));
        }

        TextView categoriesText = view.findViewById(R.id.cloudy_book_text_categories_value);
        StringBuilder categoriesList = new StringBuilder();
        for (String category : mCloudyBook.Categories) {
            categoriesList.append(category);
            categoriesList.append("\r\n");
        }

        if (!categoriesList.toString().isEmpty()) {
            categoriesText.setText(categoriesList.deleteCharAt(categoriesList.length() - 2));
        }

        TextView publishedDateText = view.findViewById(R.id.cloudy_book_text_published_date_value);
        publishedDateText.setText(mCloudyBook.PublishedDate);

        TextView publisherText = view.findViewById(R.id.cloudy_book_text_publisher_value);
        publisherText.setText(mCloudyBook.Publisher);

        TextView isbnText = view.findViewById(R.id.cloudy_book_text_isbn_value);
        if (mCloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
            isbnText.setText(mCloudyBook.ISBN_8);
        } else {
            isbnText.setText(mCloudyBook.ISBN_13);
        }

        ToggleButton read = view.findViewById(R.id.cloudy_book_toggle_read);
        read.setChecked(mCloudyBook.HasRead);
        ToggleButton owned = view.findViewById(R.id.cloudy_book_toggle_owned);
        owned.setChecked(mCloudyBook.IsOwned);

        Button addToLibraryButton = view.findViewById(R.id.cloudy_book_button_add);
        Button updateButton = view.findViewById(R.id.cloudy_book_button_update);
        Button removeFromLibraryButton = view.findViewById(R.id.cloudy_book_button_remove);

        if (mCloudyBook.AddedDate == 0) {
            updateButton.setVisibility(View.GONE);
            removeFromLibraryButton.setVisibility(View.GONE);
            addToLibraryButton.setVisibility(View.VISIBLE);
            addToLibraryButton.setOnClickListener(v -> {

                mCallback.onCloudyBookStarted();
                CloudyBook updatedBook = new CloudyBook();
                updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
                updatedBook.Authors.addAll(mCloudyBook.Authors);
                updatedBook.Categories.addAll(mCloudyBook.Categories);
                updatedBook.HasRead = read.isChecked();
                updatedBook.ISBN_8 = mCloudyBook.ISBN_8;
                updatedBook.ISBN_13 = mCloudyBook.ISBN_13;
                updatedBook.IsOwned = owned.isChecked();
                updatedBook.PublishedDate = mCloudyBook.PublishedDate;
                updatedBook.Publisher = mCloudyBook.Publisher;
                updatedBook.Title = titleText.getText().toString();
                updatedBook.VolumeId = mCloudyBook.VolumeId;

                String cloudyBookQueryPath = PathUtils.combine(User.ROOT, mUserId, CloudyBook.ROOT, updatedBook.VolumeId);
                FirebaseFirestore.getInstance().document(cloudyBookQueryPath).set(updatedBook, SetOptions.merge())
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mCallback.onCloudyBookAddedToLibrary(updatedBook);
                        } else {
                            LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", cloudyBookQueryPath);
                            if (task.getException() != null) {
                                Crashlytics.logException(task.getException());
                            }

                            mCallback.onCloudyBookAddedToLibrary(null);
                        }
                    });
            });
        } else {
            addToLibraryButton.setVisibility(View.GONE);
            updateButton.setVisibility(View.VISIBLE);
            updateButton.setOnClickListener(v -> {

                mCallback.onCloudyBookStarted();
                CloudyBook updatedBook = new CloudyBook(mCloudyBook);
                updatedBook.HasRead = read.isChecked();
                updatedBook.IsOwned = owned.isChecked();
                updatedBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();
                String cloudyBookQueryPath = PathUtils.combine(User.ROOT, mUserId, CloudyBook.ROOT, updatedBook.VolumeId);
                FirebaseFirestore.getInstance().document(cloudyBookQueryPath).set(updatedBook, SetOptions.merge())
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            mCallback.onCloudyBookUpdated(updatedBook);
                        } else {
                            LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", cloudyBookQueryPath);
                            if (task.getException() != null) {
                                Crashlytics.logException(task.getException());
                            }

                            mCallback.onCloudyBookUpdated(null);
                        }
                    });
            });

            removeFromLibraryButton.setOnClickListener(v -> {

                mCallback.onCloudyBookStarted();
                if (getActivity() != null) {
                    String message = String.format(Locale.US, getString(R.string.remove_book_message), mCloudyBook.Title);
                    if (mCloudyBook.Title.isEmpty()) {
                        message = "Remove book from your library?";
                    }

                    AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                            String queryPath = PathUtils.combine(User.ROOT, mUserId, CloudyBook.ROOT, mCloudyBook.VolumeId);
                            FirebaseFirestore.getInstance().document(queryPath).delete().addOnCompleteListener(task -> {

                                if (task.isSuccessful()) {
                                    mCallback.onCloudyBookRemoved(mCloudyBook);
                                } else {
                                    LogUtils.error(TAG, "Failed to remove book from user's library: %s", queryPath);
                                    if (task.getException() != null) {
                                        Crashlytics.logException(task.getException());
                                    }

                                    mCallback.onCloudyBookRemoved(null);
                                }
                            });
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                    removeBookDialog.show();
                } else {
                    String message = "Unable to get activity; cannot remove book.";
                    LogUtils.debug(TAG, message);
                    mCallback.onCloudyBookActionComplete(message);
                }
            });
        }


        mCallback.onCloudyBookInit(true);
        return view;
    }
}
