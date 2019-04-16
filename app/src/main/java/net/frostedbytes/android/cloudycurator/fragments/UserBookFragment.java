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

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import java.util.Calendar;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class UserBookFragment extends Fragment {

    private static final String TAG = BASE_TAG + UserBookFragment.class.getSimpleName();

    public interface OnUserBookListListener {

        void onUserBookInit(boolean isSuccessful);

        void onUserBookRemoved(UserBook userBook);

        void onUserBookUpdated(UserBook userBook);
    }

    private OnUserBookListListener mCallback;

    private UserBook mUserBook;

    public static UserBookFragment newInstance(UserBook userBook) {

        LogUtils.debug(TAG, "++newInstance(%s)", userBook.toString());
        UserBookFragment fragment = new UserBookFragment();
        Bundle args = new Bundle();
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
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUserBook = arguments.getParcelable(BaseActivity.ARG_USER_BOOK);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_user_book, container, false);
        TextView titleText = view.findViewById(R.id.userbook_text_title_value);
        titleText.setText(mUserBook.Title);
        TextView authorText = view.findViewById(R.id.userbook_text_author_value);
        StringBuilder authorList = new StringBuilder();
        for (String author : mUserBook.Authors) {
            authorList.append(author);
            authorList.append("\r\n");
        }

        if (!authorList.toString().isEmpty()) {
            authorText.setText(authorList.deleteCharAt(authorList.length() - 2));
        }

        TextView isbnText = view.findViewById(R.id.userbook_text_isbn_value);
        if (mUserBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
            isbnText.setText(mUserBook.ISBN_8);
        } else {
            isbnText.setText(mUserBook.ISBN_13);
        }

        ToggleButton read = view.findViewById(R.id.userbook_toggle_read);
        read.setChecked(mUserBook.HasRead);
        ToggleButton owned = view.findViewById(R.id.userbook_toggle_owned);
        owned.setChecked(mUserBook.IsOwned);
        Button updateLibraryButton = view.findViewById(R.id.userbook_button_update);
        Button removeFromLibraryButton = view.findViewById(R.id.userbook_button_remove);

        updateLibraryButton.setOnClickListener(v -> {

            UserBook updatedBook = new UserBook(mUserBook);
            updatedBook.HasRead = read.isChecked();
            updatedBook.IsOwned = owned.isChecked();
            updatedBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();
            mCallback.onUserBookUpdated(updatedBook);
        });

        removeFromLibraryButton.setOnClickListener(v -> mCallback.onUserBookRemoved(mUserBook));

        mCallback.onUserBookInit(true);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
    }
}
