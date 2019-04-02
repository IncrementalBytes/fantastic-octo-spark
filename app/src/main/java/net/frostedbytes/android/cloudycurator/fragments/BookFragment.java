package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.Book;
import net.frostedbytes.android.cloudycurator.models.User;
import net.frostedbytes.android.cloudycurator.models.UserBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;
import net.frostedbytes.android.cloudycurator.utils.PathUtils;

import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class BookFragment extends Fragment {

    private static final String TAG = BASE_TAG + BookFragment.class.getSimpleName();

    public interface OnBookListListener {

        void onBookAddedToLibrary(UserBook userBook);
        void onBookInit(boolean isSuccessful);
        void onBookLibraryFail();
    }

    private OnBookListListener mCallback;

    private UserBook mUserBook;
    private String mUserId;

    public static BookFragment newInstance(String userId, UserBook userBook) {

        LogUtils.debug(TAG, "++newInstance()");
        BookFragment fragment = new BookFragment();
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
            mCallback = (OnBookListListener) context;
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
        final View view = inflater.inflate(R.layout.fragment_book, container, false);
        EditText titleEdit = view.findViewById(R.id.book_edit_title);
        titleEdit.setText(mUserBook.Title);
        EditText authorEdit = view.findViewById(R.id.book_edit_author);
        authorEdit.setText(mUserBook.Author);
        EditText isbnEdit = view.findViewById(R.id.book_edit_isbn);
        isbnEdit.setText(mUserBook.ISBN);
        ToggleButton read = view.findViewById(R.id.book_toggle_read);
        read.setChecked(mUserBook.HasRead);
        ToggleButton owned = view.findViewById(R.id.book_toggle_owned);
        owned.setChecked(mUserBook.IsOwned);
        Button addToLibraryButton = view.findViewById(R.id.book_button_add);
        addToLibraryButton.setOnClickListener(v -> {
            UserBook updatedBook = new UserBook();
            updatedBook.Title = titleEdit.getText().toString();
            updatedBook.Author = authorEdit.getText().toString();
            updatedBook.ISBN = isbnEdit.getText().toString();
            updatedBook.HasRead = read.isChecked();
            updatedBook.IsOwned = owned.isChecked();

            String queryPath = PathUtils.combine(User.ROOT, mUserId, Book.ROOT, updatedBook.ISBN);
            FirebaseFirestore.getInstance().document(queryPath).set(updatedBook, SetOptions.merge())
                .addOnSuccessListener(aVoid -> mCallback.onBookAddedToLibrary(updatedBook))
                .addOnFailureListener(e -> {
                    LogUtils.error(TAG, "Failed to add book to user's library: %s", e.getMessage());
                    mCallback.onBookLibraryFail();
                });
        });

        mCallback.onBookInit(true);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
    }

    // TODO: when leaving fragment; make an update (if changed)
}
