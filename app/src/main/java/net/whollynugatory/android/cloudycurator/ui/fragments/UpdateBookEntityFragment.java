package net.whollynugatory.android.cloudycurator.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class UpdateBookEntityFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "UpdateBookEntityFragment";

  public interface OnUpdateBookEntityListener {

    void onUpdateBookEntityActionComplete(String message);

    void onUpdateBookEntityInit(boolean isSuccessful);

    void onUpdateBookEntityRemove(String volumeId);

    void onUpdateBookEntityStarted();

    void onUpdateBookEntityUpdate(BookDetail bookDetail);
  }

  private OnUpdateBookEntityListener mCallback;

  private BookDetail mBookDetail;

  public static UpdateBookEntityFragment newInstance(BookDetail bookDetail) {

    Log.d(TAG, "++newInstance()");
    UpdateBookEntityFragment fragment = new UpdateBookEntityFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_BOOK, bookDetail);
    fragment.setArguments(args);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnUpdateBookEntityListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mBookDetail = (BookDetail) arguments.getSerializable(BaseActivity.ARG_BOOK);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_book, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Log.d(TAG, "++onViewCreated(View, Bundle)");
    TextView titleText = view.findViewById(R.id.book_text_title_value);
    titleText.setText(mBookDetail.Title);
    TextView authorsText = view.findViewById(R.id.book_text_author_value);
    authorsText.setText(mBookDetail.Authors);
    TextView publishedDateText = view.findViewById(R.id.book_text_published_date_value);
    publishedDateText.setText(mBookDetail.Published);
    TextView isbnText = view.findViewById(R.id.book_text_isbn_value);
    if (mBookDetail.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
      isbnText.setText(mBookDetail.ISBN_8);
    } else {
      isbnText.setText(mBookDetail.ISBN_13);
    }

    ToggleButton read = view.findViewById(R.id.book_toggle_read);
    read.setChecked(mBookDetail.HasRead);
    ToggleButton owned = view.findViewById(R.id.book_toggle_owned);
    owned.setChecked(mBookDetail.IsOwned);

    Button addToLibraryButton = view.findViewById(R.id.book_button_add);
    addToLibraryButton.setVisibility(View.GONE);
    Button updateButton = view.findViewById(R.id.book_button_update);
    Button removeFromLibraryButton = view.findViewById(R.id.book_button_remove);

    updateButton.setOnClickListener(v -> {

      mCallback.onUpdateBookEntityStarted();
      BookDetail updatedBook = new BookDetail(mBookDetail);
      updatedBook.HasRead = read.isChecked();
      updatedBook.IsOwned = owned.isChecked();
      mCallback.onUpdateBookEntityUpdate(updatedBook);
    });

    removeFromLibraryButton.setOnClickListener(v -> {

      mCallback.onUpdateBookEntityStarted();
      if (getActivity() != null) {
        String message = String.format(Locale.US, getString(R.string.remove_book_message), mBookDetail.Title);
        if (mBookDetail.Title.isEmpty()) {
          message = "Remove book from your library?";
        }

        AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
          .setMessage(message)
          .setPositiveButton(android.R.string.yes, (dialog, which) -> mCallback.onUpdateBookEntityRemove(mBookDetail.Id))
          .setNegativeButton(android.R.string.no, null)
          .create();
        removeBookDialog.show();
      } else {
        String message = "Unable to get activity; cannot remove book.";
        Log.d(TAG, message);
        mCallback.onUpdateBookEntityActionComplete(message);
      }
    });

    mCallback.onUpdateBookEntityInit(true);
  }
}
