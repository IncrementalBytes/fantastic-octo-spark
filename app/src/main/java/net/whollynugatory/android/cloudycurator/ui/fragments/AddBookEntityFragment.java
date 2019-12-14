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
import net.whollynugatory.android.cloudycurator.common.GetPropertyIdsTask;
import net.whollynugatory.android.cloudycurator.db.CuratorDatabase;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class AddBookEntityFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "AddBookEntityFragment";

  public interface OnAddBookEntityListener {

    void onAddBookEntityAddToLibrary(BookEntity bookEntity);

    void onAddBookEntityInit(boolean isSuccessful);

    void onAddBookEntityStarted();
  }

  private OnAddBookEntityListener mCallback;

  private BookEntity mBookEntity;

  public static AddBookEntityFragment newInstance(BookEntity bookEntity) {

    Log.d(TAG, "++newInstance()");
    AddBookEntityFragment fragment = new AddBookEntityFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_BOOK, bookEntity);
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
      mCallback = (OnAddBookEntityListener) context;
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
      mBookEntity = (BookEntity) arguments.getSerializable(BaseActivity.ARG_BOOK);
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
    titleText.setText(mBookEntity.Title);
    TextView authorsText = view.findViewById(R.id.book_text_author_value);
    StringBuilder authorsList = new StringBuilder();
    for (String author : mBookEntity.Authors) {
      authorsList.append(author);
      authorsList.append("\r\n");
    }

    if (!authorsList.toString().isEmpty()) {
      authorsText.setText(authorsList.deleteCharAt(authorsList.length() - 2));
    }

    TextView categoriesText = view.findViewById(R.id.book_text_categories_value);
    StringBuilder categoriesList = new StringBuilder();
    for (String category : mBookEntity.Categories) {
      categoriesList.append(category);
      categoriesList.append("\r\n");
    }

    if (!categoriesList.toString().isEmpty()) {
      categoriesText.setText(categoriesList.deleteCharAt(categoriesList.length() - 2));
    }

    TextView publishedDateText = view.findViewById(R.id.book_text_published_date_value);
    publishedDateText.setText(mBookEntity.PublishedDate);

    TextView publisherText = view.findViewById(R.id.book_text_publisher_value);
    publisherText.setText(mBookEntity.Publisher);

    TextView isbnText = view.findViewById(R.id.book_text_isbn_value);
    if (mBookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
      isbnText.setText(mBookEntity.ISBN_8);
    } else {
      isbnText.setText(mBookEntity.ISBN_13);
    }

    ToggleButton read = view.findViewById(R.id.book_toggle_read);
    read.setChecked(mBookEntity.HasRead);
    ToggleButton owned = view.findViewById(R.id.book_toggle_owned);
    owned.setChecked(mBookEntity.IsOwned);

    Button addToLibraryButton = view.findViewById(R.id.book_button_add);
    Button updateButton = view.findViewById(R.id.book_button_update);
    updateButton.setVisibility(View.GONE);
    Button removeFromLibraryButton = view.findViewById(R.id.book_button_remove);
    removeFromLibraryButton.setVisibility(View.GONE);

    addToLibraryButton.setOnClickListener(v -> {

      mCallback.onAddBookEntityStarted();
      BookEntity updatedBook = new BookEntity();
      updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
      updatedBook.Authors.addAll(mBookEntity.Authors);
      updatedBook.Categories.addAll(mBookEntity.Categories);
      updatedBook.HasRead = read.isChecked();
      updatedBook.ISBN_8 = mBookEntity.ISBN_8;
      updatedBook.ISBN_13 = mBookEntity.ISBN_13;
      updatedBook.IsOwned = owned.isChecked();
      updatedBook.PublishedDate = mBookEntity.PublishedDate;
      updatedBook.Publisher = mBookEntity.Publisher;
      updatedBook.Title = titleText.getText().toString();
      updatedBook.VolumeId = mBookEntity.VolumeId;
      mCallback.onAddBookEntityAddToLibrary(updatedBook);
    });

    mCallback.onAddBookEntityInit(true);
  }
}
