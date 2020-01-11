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
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.whollynugatory.android.cloudycurator.db.viewmodel.BookListViewModel;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultListFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + ResultListFragment.class.getSimpleName();

  public interface OnResultListListener {

    void onResultListActionComplete(String message);

    void onResultListItemSelected(BookEntity bookEntity);
  }

  private OnResultListListener mCallback;

  private RecyclerView mRecyclerView;

  private BookListViewModel mBookListViewModel;
  private ArrayList<BookEntity> mBookEntityList;

  public static ResultListFragment newInstance(ArrayList<BookEntity> bookEntityList) {

    Log.d(TAG, "++newInstance(ArrayList<BookEntity>)");
    ResultListFragment fragment = new ResultListFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_LIST_BOOK, bookEntityList);
    fragment.setArguments(args);
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
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnResultListListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }

    Bundle arguments = getArguments();
    if (arguments != null) {
      mBookEntityList = (ArrayList<BookEntity>)arguments.getSerializable(BaseActivity.ARG_LIST_BOOK);
    } else {
      String message = "Arguments were null.";
      Log.e(TAG, message);
      mCallback.onResultListActionComplete(message);
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    final View view = inflater.inflate(R.layout.fragment_result_list, container, false);

    mRecyclerView = view.findViewById(R.id.result_list_view);

    final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
    mRecyclerView.setLayoutManager(manager);

    updateUI();
    return view;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    Log.d(TAG, "++onDestroy()");
    mBookEntityList = null;
  }

  /*
      Private Method(s)
   */
  private void updateUI() {

    if (mBookEntityList == null || mBookEntityList.size() == 0) {
      Log.w(TAG, "No results found.");
    } else {
      Log.d(TAG, "++updateUI()");
      ResultAdapter resultAdapter = new ResultAdapter(mBookEntityList);
      mRecyclerView.setAdapter(resultAdapter);
    }
  }

  /**
   * Adapter class for query result objects
   */
  private class ResultAdapter extends RecyclerView.Adapter<ResultHolder> {

    private final List<BookEntity> mBookEntityList;

    ResultAdapter(List<BookEntity> bookEntityList) {

      mBookEntityList = bookEntityList;
    }

    @NonNull
    @Override
    public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

      LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
      return new ResultHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultHolder holder, int position) {

      BookEntity bookEntity = mBookEntityList.get(position);
      holder.bind(bookEntity);
    }

    @Override
    public int getItemCount() {
      return mBookEntityList.size();
    }
  }

  /**
   * Holder class for query result object
   */
  private class ResultHolder extends RecyclerView.ViewHolder {

    private final Button mAddButton;
    private final TextView mAuthorsTextView;
    private final TextView mISBNTextView;
    private final TextView mPublishedTextView;
    private final TextView mPublisherTextView;
    private final TextView mTitleTextView;

    private BookEntity mBookEntity;

    ResultHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.item_result, parent, false));

      mAddButton = itemView.findViewById(R.id.result_button_add);
      mAuthorsTextView = itemView.findViewById(R.id.result_text_authors);
      mISBNTextView = itemView.findViewById(R.id.result_text_isbn);
      mPublishedTextView = itemView.findViewById(R.id.result_text_published);
      mPublisherTextView = itemView.findViewById(R.id.result_text_publisher);
      mTitleTextView = itemView.findViewById(R.id.result_text_title);

      mAddButton.setOnClickListener(v -> {

        Log.d(TAG, "++ResultHolder::onClick(View)");
        mBookListViewModel.insert(mBookEntity);
        mCallback.onResultListItemSelected(mBookEntity);
      });
    }

    void bind(BookEntity bookEntity) {

      mBookEntity = bookEntity;

      mAuthorsTextView.setText(mBookEntity.getAuthorsDelimited());
      mISBNTextView.setText(
        String.format(
          Locale.US,
          getString(R.string.isbn_format),
          mBookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? mBookEntity.ISBN_8 : mBookEntity.ISBN_13));
      mPublishedTextView.setText(String.format(Locale.US, getString(R.string.published_date_format), mBookEntity.PublishedDate));
//      mPublishedTextView.setText(String.format(Locale.US, getString(R.string.publisher_format), mBookEntity.getPublisherDelimited()));
      mTitleTextView.setText(mBookEntity.Title);
    }
  }
}
