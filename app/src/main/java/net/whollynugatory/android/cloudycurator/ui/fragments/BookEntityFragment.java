/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package net.whollynugatory.android.cloudycurator.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.R;

import java.util.Calendar;
import java.util.Locale;

public class BookEntityFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "BookEntityFragment";

  public interface OnBookEntityListener {

    void onBookEntityActionComplete(String message);

    void onBookEntityAddedToLibrary(BookEntity bookEntity);

    void onBookEntityInit(boolean isSuccessful);

    void onBookEntityRemoved(BookEntity bookEntity);

    void onBookEntityStarted();

    void onBookEntityUpdated(BookEntity bookEntity);
  }

  private OnBookEntityListener mCallback;

  private BookEntity mBookEntity;
  private String mUserId;

  public static BookEntityFragment newInstance(String userId, BookEntity bookEntity) {

    Log.d(TAG, "++newInstance()");
    BookEntityFragment fragment = new BookEntityFragment();
    Bundle args = new Bundle();
    args.putString(BaseActivity.ARG_FIREBASE_USER_ID, userId);
    args.putSerializable(BaseActivity.ARG_BOOK, bookEntity);
    fragment.setArguments(args);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnBookEntityListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mBookEntity = (BookEntity)arguments.getSerializable(BaseActivity.ARG_BOOK);
      mUserId = arguments.getString(BaseActivity.ARG_FIREBASE_USER_ID);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_book, container, false);
    TextView titleText = view.findViewById(R.id.book_text_title_value);
    titleText.setText(mBookEntity.Title);
    TextView authorsText = view.findViewById(R.id.book_text_author_value);
    StringBuilder authorsList = new StringBuilder();
//        for (String author : mBookEntity.Authors) {
//            authorsList.append(author);
//            authorsList.append("\r\n");
//        }

    if (!authorsList.toString().isEmpty()) {
      authorsText.setText(authorsList.deleteCharAt(authorsList.length() - 2));
    }

    TextView categoriesText = view.findViewById(R.id.book_text_categories_value);
    StringBuilder categoriesList = new StringBuilder();
//        for (String category : mBookEntity.Categories) {
//            categoriesList.append(category);
//            categoriesList.append("\r\n");
//        }

    if (!categoriesList.toString().isEmpty()) {
      categoriesText.setText(categoriesList.deleteCharAt(categoriesList.length() - 2));
    }

    TextView publishedDateText = view.findViewById(R.id.book_text_published_date_value);
    publishedDateText.setText(mBookEntity.PublishedDate);

    TextView publisherText = view.findViewById(R.id.book_text_publisher_value);
//        publisherText.setText(mBookEntity.Publisher);

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
    Button removeFromLibraryButton = view.findViewById(R.id.book_button_remove);

    if (mBookEntity.AddedDate == 0) {
      updateButton.setVisibility(View.GONE);
      removeFromLibraryButton.setVisibility(View.GONE);
      addToLibraryButton.setVisibility(View.VISIBLE);
      addToLibraryButton.setOnClickListener(v -> {

        mCallback.onBookEntityStarted();
        BookEntity updatedBook = new BookEntity();
        updatedBook.AddedDate = Calendar.getInstance().getTimeInMillis();
//                updatedBook.Authors.addAll(mBookEntity.Authors);
//                updatedBook.Categories.addAll(mBookEntity.Categories);
        updatedBook.HasRead = read.isChecked();
        updatedBook.ISBN_8 = mBookEntity.ISBN_8;
        updatedBook.ISBN_13 = mBookEntity.ISBN_13;
        updatedBook.IsOwned = owned.isChecked();
        updatedBook.PublishedDate = mBookEntity.PublishedDate;
//                updatedBook.Publisher = mBookEntity.Publisher;
        updatedBook.Title = titleText.getText().toString();
        updatedBook.VolumeId = mBookEntity.VolumeId;

        // TODO: add to library
      });
    } else {
      addToLibraryButton.setVisibility(View.GONE);
      updateButton.setVisibility(View.VISIBLE);
      updateButton.setOnClickListener(v -> {

        mCallback.onBookEntityStarted();
        BookEntity updatedBook = new BookEntity(mBookEntity);
        updatedBook.HasRead = read.isChecked();
        updatedBook.IsOwned = owned.isChecked();
        updatedBook.UpdatedDate = Calendar.getInstance().getTimeInMillis();

        // TODO: update library
      });

      removeFromLibraryButton.setOnClickListener(v -> {

        mCallback.onBookEntityStarted();
        if (getActivity() != null) {
          String message = String.format(Locale.US, getString(R.string.remove_book_message), mBookEntity.Title);
          if (mBookEntity.Title.isEmpty()) {
            message = "Remove book from your library?";
          }

          AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
              // TODO: remove from library
            })
            .setNegativeButton(android.R.string.no, null)
            .create();
          removeBookDialog.show();
        } else {
          String message = "Unable to get activity; cannot remove book.";
          Log.d(TAG, message);
          mCallback.onBookEntityActionComplete(message);
        }
      });
    }


    mCallback.onBookEntityInit(true);
    return view;
  }
}
