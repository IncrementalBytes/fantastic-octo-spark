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
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.db.data.BookAuthor;
import net.whollynugatory.android.cloudycurator.db.data.BookCategory;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.viewmodel.BookListViewModel;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ItemListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ItemListFragment";

  public enum ItemType {
    Authors,
    Books,
    Categories
  }

  public interface OnItemListListener {

    void onItemListAddBook();
    void onItemListAuthorSelected(String authorName);
    void onItemListCategorySelected(String category);
    void onItemListPopulated(int size);
  }

  private OnItemListListener mCallback;

  private FloatingActionButton mAddButton;
  private RecyclerView mRecyclerView;

  private ItemType mItemType;

  private BookListViewModel mBookListViewModel;

  private String mItemName;

  public static ItemListFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return newInstance(ItemType.Books, "");
  }

  public static ItemListFragment newInstance(ItemType itemType) {

    Log.d(TAG, "++newInstance(ItemType)");
    return newInstance(itemType, "");
  }

  public static ItemListFragment newInstance(ItemType itemType, String itemName) {

    Log.d(TAG, "++newInstance(ItemType, String)");
    ItemListFragment fragment = new ItemListFragment();
    Bundle arguments = new Bundle();
    arguments.putSerializable(BaseActivity.ARG_LIST_TYPE, itemType);
    arguments.putString(BaseActivity.ARG_ITEM_NAME, itemName);
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
    switch (mItemType) {
      case Authors:
        if (mItemName != null && mItemName.length() > 0) {
          BookEntityAdapter specificAuthorAdapter = new BookEntityAdapter(getContext());
          mRecyclerView.setAdapter(specificAuthorAdapter);
          mBookListViewModel.getAllByAuthor(mItemName).observe(this, specificAuthorAdapter::setBookEntityList);
        } else {
          BookAuthorAdapter bookAuthorAdapter = new BookAuthorAdapter(getContext());
          mRecyclerView.setAdapter(bookAuthorAdapter);
          mBookListViewModel.getSummaryByAuthors().observe(this, bookAuthorAdapter::setBookAuthorList);
        }

        break;
      case Books:
        BookEntityAdapter bookEntityAdapter = new BookEntityAdapter(getContext());
        mRecyclerView.setAdapter(bookEntityAdapter);
        mBookListViewModel.getAll().observe(this, bookEntityAdapter::setBookEntityList);
        break;
      case Categories:
        if (mItemName != null && mItemName.length() > 0) {
          BookEntityAdapter specificCategoryAdapter = new BookEntityAdapter(getContext());
          mRecyclerView.setAdapter(specificCategoryAdapter);
          mBookListViewModel.getAllByCategory(mItemName).observe(this, specificCategoryAdapter::setBookEntityList);
        } else {
          BookCategoryAdapter bookCategoryAdapter = new BookCategoryAdapter(getContext());
          mRecyclerView.setAdapter(bookCategoryAdapter);
          mBookListViewModel.getSummaryByCategories().observe(this, bookCategoryAdapter::setBookCategoryList);
        }

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
    mItemType = ItemType.Books;
    if (arguments != null) {
      if (arguments.containsKey(BaseActivity.ARG_LIST_TYPE)) {
        mItemType = (ItemType) arguments.getSerializable(BaseActivity.ARG_LIST_TYPE);
      }

      if (arguments.containsKey(BaseActivity.ARG_ITEM_NAME)) {
        mItemName = arguments.getString(BaseActivity.ARG_ITEM_NAME);
      }
    }

    mBookListViewModel = ViewModelProviders.of(this).get(BookListViewModel.class);
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
  }

  /*
    Adapter class for BookAuthor objects
  */
  private class BookAuthorAdapter extends RecyclerView.Adapter<BookAuthorAdapter.BookAuthorHolder> {

    /*
      Holder class for BookAuthor objects
     */
    class BookAuthorHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      private final TextView mAuthorTextView;
      private final TextView mBookCountTextView;

      private BookAuthor mBookAuthor;

      BookAuthorHolder(View itemView) {
        super(itemView);

        mAuthorTextView = itemView.findViewById(R.id.author_item_name);
        mBookCountTextView = itemView.findViewById(R.id.author_item_count);

        itemView.setOnClickListener(this);
      }

      void bind(BookAuthor bookAuthor) {

        mBookAuthor = bookAuthor;

        if (mBookAuthor != null) {
          mAuthorTextView.setText(mBookAuthor.AuthorName);
          mBookCountTextView.setText(String.format(getString(R.string.books_by_author), mBookAuthor.BookCount));
        }
      }

      @Override
      public void onClick(View view) {

        Log.d(TAG, "++BookAuthorHolder::onClick(View)");
        mCallback.onItemListAuthorSelected(mBookAuthor.AuthorName);
      }
    }

    private final LayoutInflater mInflater;
    private List<BookAuthor> mBookAuthorList;

    BookAuthorAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public BookAuthorHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.item_author, parent, false);
      return new BookAuthorHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookAuthorHolder holder, int position) {

      if (mBookAuthorList != null) {
        BookAuthor bookAuthor = mBookAuthorList.get(position);
        holder.bind(bookAuthor);
      } else {
        // No books!
      }
    }

    @Override
    public int getItemCount() {

      if (mBookAuthorList != null) {
        return mBookAuthorList.size();
      } else {
        return 0;
      }
    }

    void setBookAuthorList(List<BookAuthor> bookAuthorList) {

      Log.d(TAG, "++setBookAuthorList(List<BookAuthor>)");
      mBookAuthorList = bookAuthorList;
      notifyDataSetChanged();
    }
  }

  /*
  Adapter class for BookCategory objects
*/
  private class BookCategoryAdapter extends RecyclerView.Adapter<BookCategoryAdapter.BookCategoryHolder> {

    /*
      Holder class for BookCategory objects
     */
    class BookCategoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      private final TextView mCategoryTextView;
      private final TextView mBookCountTextView;

      private BookCategory mBookCategory;

      BookCategoryHolder(View itemView) {
        super(itemView);

        mCategoryTextView = itemView.findViewById(R.id.category_item_name);
        mBookCountTextView = itemView.findViewById(R.id.category_item_count);

        itemView.setOnClickListener(this);
      }

      void bind(BookCategory bookCategory) {

        mBookCategory = bookCategory;

        if (mBookCategory != null) {
          mCategoryTextView.setText(mBookCategory.CategoryName);
          mBookCountTextView.setText(String.format(getString(R.string.books_within_category), mBookCategory.BookCount));
        }
      }

      @Override
      public void onClick(View view) {

        Log.d(TAG, "++BookCategoryHolder::onClick(View)");
        mCallback.onItemListCategorySelected(mBookCategory.CategoryName);
      }
    }

    private final LayoutInflater mInflater;
    private List<BookCategory> mBookCategoryList;

    BookCategoryAdapter(Context context) {

      mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public BookCategoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      View itemView = mInflater.inflate(R.layout.item_category, parent, false);
      return new BookCategoryHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookCategoryHolder holder, int position) {

      if (mBookCategoryList != null) {
        BookCategory bookCategory = mBookCategoryList.get(position);
        holder.bind(bookCategory);
      } else {
        // No books!
      }
    }

    @Override
    public int getItemCount() {

      if (mBookCategoryList != null) {
        return mBookCategoryList.size();
      } else {
        return 0;
      }
    }

    void setBookCategoryList(List<BookCategory> bookCategoryList) {

      Log.d(TAG, "++setBookCategoryList(List<BookCategory>)");
      mBookCategoryList = bookCategoryList;
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
              AlertDialog removeBookDialog = new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> mBookListViewModel.delete(mBookEntity.VolumeId))
                .setNegativeButton(android.R.string.no, null)
                .create();
              removeBookDialog.show();
            } else {
              Log.w(TAG, "Unable to remove book at this time.");
            }
          });

          mAuthorsTextView.setText(mBookEntity.Authors);
          mCategoriesTextView.setVisibility(View.GONE);
          mISBNTextView.setText(
            String.format(
              Locale.US,
              getString(R.string.isbn_format),
              mBookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? mBookEntity.ISBN_8 : mBookEntity.ISBN_13));

          mOwnSwitch.setText(mBookEntity.IsOwned ? getString(R.string.owned) : getString(R.string.not_owned));
          mOwnSwitch.setChecked(mBookEntity.IsOwned);
          mReadSwitch.setText(mBookEntity.HasRead ? getString(R.string.read) : getString(R.string.unread));
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

      Log.d(TAG, "++setBookDetailList(List<BookEntity>)");
      mBookEntityList = new ArrayList<>(bookEntityList);
      mCallback.onItemListPopulated(bookEntityList.size());
      notifyDataSetChanged();
    }
  }
}
