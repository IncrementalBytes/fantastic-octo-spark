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
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;

import java.util.Calendar;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class UserBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + UserBookFragment.class.getSimpleName();

    public interface OnUserBookListListener {

        void onUserBookInit(boolean isSuccessful);

        void onUserBookFail();

        void onUserBookUpdated(UserBook userBook);
    }

    private OnUserBookListListener mCallback;

    private UserBook mUserBook;
    private String mUserId;

    public static UserBookFragment newInstance(String userId, UserBook userBook) {

        LogUtils.debug(TAG, "++newInstance(%s, %s)", userId, userBook.toString());
        UserBookFragment fragment = new UserBookFragment();
        Bundle args = new Bundle();
        args.putString(BaseActivity.ARG_USER_ID, userId);
        args.putParcelable(BaseActivity.ARG_USER_BOOK, userBook);
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
            mCallback = (OnUserBookListListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUserBook = arguments.getParcelable(BaseActivity.ARG_USER_BOOK);
            mUserId = arguments.getString(BaseActivity.ARG_USER_ID);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_userbook, container, false);
        TextView titleText = view.findViewById(R.id.userbook_text_title_value);
        titleText.setText(mUserBook.Title);
        TextView authorText = view.findViewById(R.id.userbook_text_author_value);
        if (mUserBook.Authors.size() > 0) {
            authorText.setText(mUserBook.Authors.get(0)); // TODO: update control to multiline
        }

        TextView isbnText = view.findViewById(R.id.userbook_text_isbn_value);
        isbnText.setText(mUserBook.ISBN);
        ToggleButton read = view.findViewById(R.id.userbook_toggle_read);
        read.setChecked(mUserBook.HasRead);
        ToggleButton owned = view.findViewById(R.id.userbook_toggle_owned);
        owned.setChecked(mUserBook.IsOwned);
        Button updateLibraryButton = view.findViewById(R.id.userbook_button_update);

        // TODO: enable updateLibraryButton only if content has changed
        updateLibraryButton.setOnClickListener(v -> {

            UserBook updatedBook = new UserBook();
            updatedBook.AddedDate = mUserBook.AddedDate;
            updatedBook.Title = titleText.getText().toString();
            updatedBook.Authors.add(authorText.getText().toString()); // TODO: parse the control for list of authors
            updatedBook.ISBN = isbnText.getText().toString();
            updatedBook.HasRead = read.isChecked();
            updatedBook.IsOwned = owned.isChecked();
            updatedBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();

            String queryPath = PathUtils.combine(User.ROOT, mUserId, UserBook.ROOT, updatedBook.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).set(updatedBook, SetOptions.merge()).addOnCompleteListener(task -> {

                if (task.isSuccessful()) {
                    mCallback.onUserBookUpdated(updatedBook);
                } else {
                    LogUtils.error(TAG, "Failed to add book to user's library: %s", queryPath);
                    if (task.getException() != null) {
                        Crashlytics.logException(task.getException());
                    }

                    mCallback.onUserBookFail();
                }
            });
        });

        mCallback.onUserBookInit(true);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
    }

    // TODO: when leaving fragment; make an update (if changed)
}
