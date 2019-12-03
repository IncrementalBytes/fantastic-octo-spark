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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
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

  private RecyclerView mRecyclerView;

  private ArrayList<BookEntity> mBookEntityList;

  public static BookEntityListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new BookEntityListFragment();
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
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
    mBookEntityList = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    FloatingActionButton mAddButton = view.findViewById(R.id.book_fab_add);
    mRecyclerView = view.findViewById(R.id.book_list_view);
    FloatingActionButton mSyncButton = view.findViewById(R.id.book_fab_sync);


    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);

    mAddButton.setOnClickListener(pickView -> mCallback.onBookEntityListAddBook());
    mSyncButton.setOnClickListener(pickView -> mCallback.onBookEntityListSynchronize());

    updateUI();
  }

  /*
      Private Method(s)
   */
  private void updateUI() {

    if (mBookEntityList == null || mBookEntityList.size() == 0) {
      mCallback.onBookEntityListPopulated(0);
    } else {
      Log.d(TAG, "++updateUI()");
      BookEntityAdapter bookEntityAdapter = new BookEntityAdapter(mBookEntityList);
      mRecyclerView.setAdapter(bookEntityAdapter);
      mCallback.onBookEntityListPopulated(bookEntityAdapter.getItemCount());
    }
  }

  /**
   * Adapter class for BookEntity objects
   */
  private class BookEntityAdapter extends RecyclerView.Adapter<BookEntityHolder> {

    private final List<BookEntity> mBookEntityList;

    BookEntityAdapter(List<BookEntity> bookEntityList) {

      mBookEntityList = bookEntityList;
    }

    @NonNull
    @Override
    public BookEntityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new BookEntityHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull BookEntityHolder holder, int position) {

      BookEntity bookEntity = mBookEntityList.get(position);
      holder.bind(bookEntity);
    }

    @Override
    public int getItemCount() {
      return mBookEntityList.size();
    }
  }

  /**
   * Holder class for BookEntity objects
   */
  private class BookEntityHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final TextView mAuthorsTextView;
    private final TextView mCategoriesTextView;
    private final TextView mISBNTextView;
    private final ImageView mOwnImage;
    private final TextView mPublishedTextView;
    private final TextView mPublisherTextView;
    private final ImageView mReadImage;
    private final TextView mTitleTextView;

    private BookEntity mBookEntity;

    BookEntityHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.book_item, parent, false));

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

    void bind(BookEntity bookEntity) {

      mBookEntity = bookEntity;

//            mAuthorsTextView.setText(mBookEntity.getAuthorsDelimited());
      mCategoriesTextView.setVisibility(View.GONE);
      mISBNTextView.setText(
        String.format(
          Locale.US,
          getString(R.string.isbn_format),
          mBookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? mBookEntity.ISBN_8 : mBookEntity.ISBN_13));
      if (mBookEntity.IsOwned) {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_owned_dark, null));
      } else {
        mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_not_owned_dark, null));
      }

      mPublishedTextView.setVisibility(View.GONE);
      mPublisherTextView.setVisibility(View.GONE);
      mReadImage.setVisibility(mBookEntity.HasRead ? View.VISIBLE : View.INVISIBLE);
      mTitleTextView.setText(mBookEntity.Title);
    }

    @Override
    public void onClick(View view) {

      mCallback.onBookEntityListItemSelected(mBookEntity);
    }
  }
}
