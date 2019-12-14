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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookEntityListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + BookEntityListFragment.class.getSimpleName();

  public interface OnBookEntityListListener {

    void onBookEntityListAddBook();
    void onBookEntityListDeleteBook(String volumeId);
    void onBookEntityListItemSelected(BookDetail bookDetail);
  }

  private OnBookEntityListListener mCallback;

  private FloatingActionButton mAddButton;
  private RecyclerView mRecyclerView;

  private ArrayList<BookDetail> mBookDetails;

  public static BookEntityListFragment newInstance(ArrayList<BookDetail> bookDetailList) {

    Log.d(TAG, "++newInstance()");
    BookEntityListFragment fragment = new BookEntityListFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_BOOK_LIST, bookDetailList);
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
      mCallback = (OnBookEntityListListener) context;
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
      mBookDetails = (ArrayList<BookDetail>) arguments.getSerializable(BaseActivity.ARG_BOOK_LIST);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_book_list, container, false);

    mAddButton = view.findViewById(R.id.book_fab_add);
    mRecyclerView = view.findViewById(R.id.book_list_view);

    return view;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "++onActivityCreated()");
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mAddButton.setOnClickListener(pickView -> mCallback.onBookEntityListAddBook());
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "++onResume()");
    updateUI();
  }

  /*
     Private Method(s)
  */
  private void updateUI() {

    if (mBookDetails != null && mBookDetails.size() > 0) {
      Log.d(TAG, "++updateUI()");
      BookDetailAdapter bookEntityAdapter = new BookDetailAdapter(mBookDetails);
      mRecyclerView.setAdapter(bookEntityAdapter);
    } else {
      Log.w(TAG, "No book details found.");
    }
  }

  /*
    Adapter class for BookDetail objects
   */
  private class BookDetailAdapter extends RecyclerView.Adapter<BookDetailHolder> {

    private List<BookDetail> mBookDetailList;

    BookDetailAdapter(List<BookDetail> bookDetailList) {

      mBookDetailList = bookDetailList;
    }

    @NonNull
    @Override
    public BookDetailHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new BookDetailHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull BookDetailHolder holder, int position) {

      BookDetail bookDetail = mBookDetailList.get(position);
      holder.bind(bookDetail);
    }

    @Override
    public int getItemCount() {

      if (mBookDetailList != null) {
        return mBookDetailList.size();
      } else {
        return 0;
      }
    }
  }

  /*
    Holder class for BookDetail objects
   */
  private class BookDetailHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView mAuthorsTextView;
    private final TextView mCategoriesTextView;
    private final ImageView mDeleteImage;
    private final TextView mISBNTextView;
    private final ImageView mOwnImage;
    private final TextView mPublishedTextView;
    private final TextView mPublisherTextView;
    private final ImageView mReadImage;
    private final TextView mTitleTextView;

    private BookDetail mBookDetail;

    BookDetailHolder(LayoutInflater layoutInflater, ViewGroup parent) {
      super(layoutInflater.inflate(R.layout.book_item, parent, false));

      mAuthorsTextView = itemView.findViewById(R.id.book_item_authors);
      mCategoriesTextView = itemView.findViewById(R.id.book_item_categories);
      mDeleteImage = itemView.findViewById(R.id.book_item_image_delete);
      mISBNTextView = itemView.findViewById(R.id.book_item_isbn);
      mOwnImage = itemView.findViewById(R.id.book_item_image_own);
      mPublishedTextView = itemView.findViewById(R.id.book_item_published);
      mPublisherTextView = itemView.findViewById(R.id.book_item_publisher);
      mReadImage = itemView.findViewById(R.id.book_item_image_read);
      mTitleTextView = itemView.findViewById(R.id.book_item_title);

      itemView.setOnClickListener(this);
    }

    void bind(BookDetail bookDetail) {

      mBookDetail = bookDetail;

      mDeleteImage.setOnClickListener(v -> {
        if (getActivity() != null) {
          String message = String.format(Locale.US, getString(R.string.remove_specific_book_message), mBookDetail.Title);
          if (mBookDetail.Title.isEmpty()) {
            message = getString(R.string.remove_book_message);
          }

          AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, (dialog, which) -> mCallback.onBookEntityListDeleteBook(mBookDetail.Id))
            .setNegativeButton(android.R.string.no, null)
            .create();
          removeBookDialog.show();
        } else {
          Log.w(TAG, "Unable to remove book at this time.");
        }
      });

      mAuthorsTextView.setText(bookDetail.Authors);
      mCategoriesTextView.setVisibility(View.GONE);
      mISBNTextView.setText(
        String.format(
          Locale.US,
          getString(R.string.isbn_format),
          bookDetail.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? bookDetail.ISBN_8 : bookDetail.ISBN_13));

      if (mBookDetail.IsOwned) {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      if (mBookDetail.HasRead) {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_checked_dark, null));
      } else {
        mReadImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_unchecked_dark, null));
      }

      mPublishedTextView.setVisibility(View.GONE);
      mPublisherTextView.setVisibility(View.GONE);
      mTitleTextView.setText(bookDetail.Title);
    }

    @Override
    public void onClick(View view) {

      Log.d(TAG, "++BookDetailHolder::onClick(View)");
      mCallback.onBookEntityListItemSelected(mBookDetail);
    }
  }
}
