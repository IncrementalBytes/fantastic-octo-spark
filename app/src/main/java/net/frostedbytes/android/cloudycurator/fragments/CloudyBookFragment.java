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
                String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
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
        if (mCloudyBook.Authors.size() > 0) {
            authorText.setText(mCloudyBook.Authors.get(0)); // TODO: update control to multiline
        }

        TextView isbnText = view.findViewById(R.id.book_text_isbn_value);
        isbnText.setText(mCloudyBook.ISBN);
        ToggleButton read = view.findViewById(R.id.book_toggle_read);
        ToggleButton owned = view.findViewById(R.id.book_toggle_owned);

        Button addToLibraryButton = view.findViewById(R.id.book_button_add);
        addToLibraryButton.setOnClickListener(v -> {

            UserBook updatedBook = new UserBook();
            updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
            updatedBook.Authors.add(authorText.getText().toString()); // TODO: parse the control for list of authors
            updatedBook.HasRead = read.isChecked();
            updatedBook.ISBN = isbnText.getText().toString();
            updatedBook.IsOwned = owned.isChecked();
            updatedBook.Title = titleText.getText().toString();

            String queryPath = PathUtils.combine(User.ROOT, mUserId, UserBook.ROOT, updatedBook.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).set(updatedBook, SetOptions.merge()).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    mCallback.onUserBookAddedToLibrary(updatedBook);
                } else {
                    LogUtils.error(TAG, "Failed to add cloudy book to user's library: %s", queryPath);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
    }

    // TODO: when leaving fragment; make an update (if changed)
}
