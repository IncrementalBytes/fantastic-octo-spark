package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;

import java.util.Calendar;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class CloudyBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + CloudyBookFragment.class.getSimpleName();

    public interface OnCloudyBookListener {

        void onUserBookAddedToLibrary(UserBook userBook);

        void onUserBookAddedToLibraryFail();

        void onCloudyBookInit(boolean isSuccessful);
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
        final View view = inflater.inflate(R.layout.fragment_book, container, false);
        TextView titleText = view.findViewById(R.id.book_text_title_value);
        titleText.setText(mCloudyBook.Title);
        TextView authorText = view.findViewById(R.id.book_text_author_value);
        StringBuilder authorList = new StringBuilder();
        for (String author : mCloudyBook.Authors) {
            authorList.append(author);
            authorList.append("\r\n");
        }

        if (!authorList.toString().isEmpty()) {
            authorText.setText(authorList.deleteCharAt(authorList.length() - 2));
        }

        TextView isbnText = view.findViewById(R.id.book_text_isbn_value);
        if (mCloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
            isbnText.setText(mCloudyBook.ISBN_8);
        } else {
            isbnText.setText(mCloudyBook.ISBN_13);
        }

        ToggleButton read = view.findViewById(R.id.book_toggle_read);
        ToggleButton owned = view.findViewById(R.id.book_toggle_owned);

        Button addToLibraryButton = view.findViewById(R.id.book_button_add);
        addToLibraryButton.setOnClickListener(v -> {

            // add book to both general collection and user's specific library
            String cloudyBookQueryPath = PathUtils.combine(CloudyBook.ROOT, mCloudyBook.VolumeId);
            FirebaseFirestore.getInstance().document(cloudyBookQueryPath).set(mCloudyBook, SetOptions.merge())
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        LogUtils.error(TAG, "Failed to add cloudy book to main library: %s", cloudyBookQueryPath);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }
                    }
            });

            UserBook updatedBook = new UserBook();
            updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
            updatedBook.Authors.addAll(mCloudyBook.Authors);
            updatedBook.HasRead = read.isChecked();
            updatedBook.ISBN_8 = mCloudyBook.ISBN_8;
            updatedBook.ISBN_13 = mCloudyBook.ISBN_13;
            updatedBook.IsOwned = owned.isChecked();
            updatedBook.Title = titleText.getText().toString();
            updatedBook.VolumeId = mCloudyBook.VolumeId;

            String userBookQueryPath = PathUtils.combine(User.ROOT, mUserId, UserBook.ROOT, updatedBook.VolumeId);
            FirebaseFirestore.getInstance().document(userBookQueryPath).set(updatedBook, SetOptions.merge())
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        mCallback.onUserBookAddedToLibrary(updatedBook);
                    } else {
                        LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", userBookQueryPath);
                        if (task.getException() != null) {
                            Crashlytics.logException(task.getException());
                        }

                        mCallback.onUserBookAddedToLibraryFail();
                    }
            });
        });

        mCallback.onCloudyBookInit(true);
        return view;
    }
}
