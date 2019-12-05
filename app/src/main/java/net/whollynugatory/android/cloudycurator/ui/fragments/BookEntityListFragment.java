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

import androidx.fragment.app.Fragment;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.viewmodel.BookViewModel;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookEntityListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + BookEntityListFragment.class.getSimpleName();

  public interface OnBookEntityListListener {

    void onBookEntityListAddBook();

    void onBookEntityListItemSelected(BookEntity bookEntity);

    void onBookEntityListPopulated(int size);

    void onBookEntityListSynchronize();
  }

  private OnBookEntityListListener mCallback;

  public static BookEntityListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new BookEntityListFragment();
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
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_book_list, container, false);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    FloatingActionButton mAddButton = view.findViewById(R.id.book_fab_add);
    RecyclerView mRecyclerView = view.findViewById(R.id.book_list_view);
    FloatingActionButton mSyncButton = view.findViewById(R.id.book_fab_sync);

    BookEntityAdapter bookEntityAdapter = new BookEntityAdapter(getActivity());
    mRecyclerView.setAdapter(bookEntityAdapter);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mCallback.onBookEntityListPopulated(bookEntityAdapter.getItemCount());

    mAddButton.setOnClickListener(pickView -> mCallback.onBookEntityListAddBook());
    mSyncButton.setOnClickListener(pickView -> mCallback.onBookEntityListSynchronize());

    BookViewModel bookViewModel = ViewModelProviders.of(this).get(BookViewModel.class);

    bookViewModel.getAllBooks().observe(this, bookEntityAdapter::setBooks);
  }

  /**
   * Adapter class for BookEntity objects
   */
  private class BookEntityAdapter extends RecyclerView.Adapter<BookEntityAdapter.BookEntityHolder> {

    private List<BookEntity> mBookEntityList;

    private final LayoutInflater mInflater;

    BookEntityAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
      mBookEntityList = new ArrayList<>();
    }

    @NonNull
    @Override
    public BookEntityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.book_item, parent, false);
      return new BookEntityHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookEntityHolder holder, int position) {

      if (mBookEntityList != null) {
        BookEntity bookEntity = mBookEntityList.get(position);
//        holder.mAuthorsTextView.setText(bookEntity.getAuthorsDelimited());
        holder.mCategoriesTextView.setVisibility(View.GONE);
        holder.mISBNTextView.setText(
          String.format(
            Locale.US,
            getString(R.string.isbn_format),
            bookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? bookEntity.ISBN_8 : bookEntity.ISBN_13));
        if (bookEntity.IsOwned) {
          holder.mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_owned_dark, null));
        } else {
          holder.mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_not_owned_dark, null));
        }

        holder.mPublishedTextView.setVisibility(View.GONE);
        holder.mPublisherTextView.setVisibility(View.GONE);
        holder.mReadImage.setVisibility(bookEntity.HasRead ? View.VISIBLE : View.INVISIBLE);
        holder.mTitleTextView.setText(bookEntity.Title);
      } else {
        // TODO: cover case of data not being ready yet
      }
    }

    @Override
    public int getItemCount() {

      if (mBookEntityList != null) {
        return mBookEntityList.size();
      } else {
        return 0;
      }
    }

    void setBooks(List<BookEntity> bookEntityList) {

      mBookEntityList = bookEntityList;
      notifyDataSetChanged();
    }

    class BookEntityHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      private final TextView mAuthorsTextView;
      private final TextView mCategoriesTextView;
      private final TextView mISBNTextView;
      private final ImageView mOwnImage;
      private final TextView mPublishedTextView;
      private final TextView mPublisherTextView;
      private final ImageView mReadImage;
      private final TextView mTitleTextView;

      BookEntityHolder(View itemView) {
        super(itemView);

        mAuthorsTextView = itemView.findViewById(R.id.book_item_authors);
        mCategoriesTextView = itemView.findViewById(R.id.book_item_categories);
        mISBNTextView = itemView.findViewById(R.id.book_item_isbn);
        mOwnImage = itemView.findViewById(R.id.book_image_own);
        mPublishedTextView = itemView.findViewById(R.id.book_item_published);
        mPublisherTextView = itemView.findViewById(R.id.book_item_publisher);
        mReadImage = itemView.findViewById(R.id.book_image_read);
        mTitleTextView = itemView.findViewById(R.id.book_item_title);

        itemView.setOnClickListener(this);
      }

      @Override
      public void onClick(View view) {

        // TODO: mCallback.onBookEntityListItemSelected(mBookEntity);
      }
    }
  }
}
