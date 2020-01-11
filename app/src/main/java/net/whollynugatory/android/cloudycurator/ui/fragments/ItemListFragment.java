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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.db.entity.AuthorEntity;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.viewmodel.BookListViewModel;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ItemListFragment  extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ItemListFragment";

  public enum ItemType {
    Authors,
    Books,
    Categories
  }

  public interface OnItemListListener {

    void onItemListAddBook();
    void onItemListDeleteBook(String volumeId);
    void onAuthorItemSelected(AuthorEntity authorEntity);
    void onBookItemSelected(BookDetail bookDetail);
  }

  private OnItemListListener mCallback;

  private FloatingActionButton mAddButton;
  private RecyclerView mRecyclerView;

  private ItemType mItemType;

  private BookListViewModel mBookListViewModel;

  public static ItemListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return newInstance(ItemType.Books);
  }

  public static ItemListFragment newInstance(ItemType itemType) {

    Log.d(TAG, "++newInstance(ItemType)");
    ItemListFragment fragment = new ItemListFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_LIST_TYPE, itemType);
    fragment.setArguments(arguments);
    return fragment;
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Log.d(TAG, "++onActivityCreated()");
    mBookListViewModel = ViewModelProviders.of(this).get(BookListViewModel.class);
    switch (mItemType) {
      case Authors:
        AuthorEntityAdapter authorEntityAdapter = new AuthorEntityAdapter(getContext());
        mRecyclerView.setAdapter(authorEntityAdapter);
//        mCuratorViewModel.getAllAuthors().observe(this, new Observer<List<AuthorEntity>>() {
//          @Override
//          public void onChanged(List<AuthorEntity> authorEntityList) {
//            authorEntityAdapter.setAuthorEntityList(authorEntityList);
//          }
//        });

        break;
      case Books:
        BookEntityAdapter bookEntityAdapter = new BookEntityAdapter(getContext());
        mRecyclerView.setAdapter(bookEntityAdapter);
        mBookListViewModel.getAll().observe(this, new Observer<List<BookEntity>>() {
          @Override
          public void onChanged(List<BookEntity> bookEntityList) {
            bookEntityAdapter.setBookEntityList(bookEntityList);
          }
        });

        break;
    }

    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mAddButton.setOnClickListener(pickView -> mCallback.onItemListAddBook());
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnItemListListener) context;
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
      mItemType = (ItemType)arguments.getSerializable(BaseActivity.ARG_LIST_TYPE);
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    View view = inflater.inflate(R.layout.fragment_list, container, false);
    mAddButton = view.findViewById(R.id.fab_add);
    mRecyclerView = view.findViewById(R.id.list_view);
    return view;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
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
  }

  /*
    Adapter class for AuthorDetail objects
   */
  private class AuthorEntityAdapter extends RecyclerView.Adapter<AuthorEntityAdapter.AuthorEntityHolder> {

    /*
      Holder class for AuthorEntity objects
     */
    class AuthorEntityHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      private final TextView mNameTextView;
      private final TextView mCountTextView;

      private AuthorEntity mAuthorEntity;

      AuthorEntityHolder(View itemView) {
        super(itemView);

        mNameTextView = itemView.findViewById(R.id.author_item_name);
        mCountTextView = itemView.findViewById(R.id.author_item_count);
        itemView.setOnClickListener(this);
      }

      void bind(AuthorEntity authorEntity) {

        mAuthorEntity = authorEntity;

        if (mAuthorEntity != null) {
          mNameTextView.setText(mAuthorEntity.Name);
          mCountTextView.setText(String.format(getString(R.string.books_by_author), 0));
        }
      }

      @Override
      public void onClick(View view) {

        Log.d(TAG, "++AuthorEntityHolder::onClick(View)");
        mCallback.onAuthorItemSelected(mAuthorEntity);
      }
    }

    private final LayoutInflater mInflater;
    private List<AuthorEntity> mAuthorEntityList;

    AuthorEntityAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public AuthorEntityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.item_author, parent, false);
      return new AuthorEntityHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthorEntityHolder holder, int position) {

      if (mAuthorEntityList != null) {
        AuthorEntity authorEntity = mAuthorEntityList.get(position);
        holder.bind(authorEntity);
      } else {
        // No authors!
      }
    }

    @Override
    public int getItemCount() {

      if (mAuthorEntityList != null) {
        return mAuthorEntityList.size();
      } else {
        return 0;
      }
    }

    void setAuthorEntityList(List<AuthorEntity> authorEntityList) {

      Log.d(TAG, "++setAuthorEntityList(List<AuthorEntity>");
      mAuthorEntityList = authorEntityList;
      notifyDataSetChanged();
    }
  }

  /*
  Adapter class for BookEntity objects
 */
  private class BookEntityAdapter extends RecyclerView.Adapter<BookEntityAdapter.BookEntityHolder> {

    /*
      Holder class for BookEntity objects
     */
    class BookEntityHolder extends RecyclerView.ViewHolder {

      private final TextView mAuthorsTextView;
      private final TextView mCategoriesTextView;
      private final ImageView mDeleteImage;
      private final TextView mISBNTextView;
      private final Switch mOwnSwitch;
      private final TextView mPublishedTextView;
      private final TextView mPublisherTextView;
      private final Switch mReadSwitch;
      private final TextView mTitleTextView;

      private BookEntity mBookEntity;

      BookEntityHolder(View itemView) {
        super(itemView);

        mAuthorsTextView = itemView.findViewById(R.id.book_text_authors);
        mCategoriesTextView = itemView.findViewById(R.id.book_text_categories);
        mDeleteImage = itemView.findViewById(R.id.book_image_delete);
        mISBNTextView = itemView.findViewById(R.id.book_text_isbn);
        mOwnSwitch = itemView.findViewById(R.id.book_switch_owned);
        mPublishedTextView = itemView.findViewById(R.id.book_text_published);
        mPublisherTextView = itemView.findViewById(R.id.book_text_publisher);
        mReadSwitch = itemView.findViewById(R.id.book_switch_read);
        mTitleTextView = itemView.findViewById(R.id.book_text_title);

        mOwnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

          mBookEntity.IsOwned = isChecked;
          mBookListViewModel.update(mBookEntity);
          mOwnSwitch.setText(isChecked ? getString(R.string.owned) : getString(R.string.not_owned));
        });

        mReadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

          mBookEntity.HasRead = isChecked;
          mBookListViewModel.update(mBookEntity);
          mReadSwitch.setText(isChecked ? getString(R.string.read) : getString(R.string.unread));
        });
      }

      void bind(BookEntity bookEntity) {

        mBookEntity = bookEntity;

        if (mBookEntity != null) {
          mDeleteImage.setOnClickListener(v -> {
            if (getActivity() != null) {
              String message = String.format(Locale.US, getString(R.string.remove_specific_book_message), mBookEntity.Title);
              if (mBookEntity.Title.isEmpty()) {
                message = getString(R.string.remove_book_message);
              }

              AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> mCallback.onItemListDeleteBook(mBookEntity.VolumeId))
                .setNegativeButton(android.R.string.no, null)
                .create();
              removeBookDialog.show();
            } else {
              Log.w(TAG, "Unable to remove book at this time.");
            }
          });

          mAuthorsTextView.setText(mBookEntity.AuthorId);
          mCategoriesTextView.setVisibility(View.GONE);
          mISBNTextView.setText(
            String.format(
              Locale.US,
              getString(R.string.isbn_format),
              mBookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? mBookEntity.ISBN_8 : mBookEntity.ISBN_13));

          mOwnSwitch.setChecked(mBookEntity.IsOwned);
          mReadSwitch.setChecked(mBookEntity.HasRead);

          mPublishedTextView.setVisibility(View.GONE);
          mPublisherTextView.setVisibility(View.GONE);
          mTitleTextView.setText(mBookEntity.Title);
        }
      }
    }

    private final LayoutInflater mInflater;
    private List<BookEntity> mBookEntityList;

    BookEntityAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public BookEntityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.item_book, parent, false);
      return new BookEntityHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookEntityHolder holder, int position) {

      if (mBookEntityList != null) {
        BookEntity bookEntity = mBookEntityList.get(position);
        holder.bind(bookEntity);
      } else {
        // No books!
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

    void setBookEntityList(List<BookEntity> bookEntityList) {

      Log.d(TAG, "++setBookDetailList(List<BookEntity>");
      mBookEntityList = bookEntityList;
      notifyDataSetChanged();
    }
  }
}
