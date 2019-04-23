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

package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class CloudyBookListFragment extends Fragment {

    private static final String TAG = BASE_TAG + CloudyBookListFragment.class.getSimpleName();

    public interface OnCloudyBookListListener {

        void onCloudyBookListAddBook();

        void onCloudyBookListItemSelected(CloudyBook cloudyBook);

        void onCloudyBookListPopulated(int size);

        void onCloudyBookListSynchronize();
    }

    private OnCloudyBookListListener mCallback;

    private RecyclerView mRecyclerView;

    private ArrayList<CloudyBook> mCloudyBookList;

    public static CloudyBookListFragment newInstance(ArrayList<CloudyBook> cloudyBookList) {

        LogUtil.debug(TAG, "++newInstance(%d)", cloudyBookList.size());
        CloudyBookListFragment fragment = new CloudyBookListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(BaseActivity.ARG_CLOUDY_BOOK_LIST, cloudyBookList);
        fragment.setArguments(args);
        return fragment;
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        LogUtil.debug(TAG, "++onAttach(Context)");
        try {
            mCallback = (OnCloudyBookListListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mCloudyBookList = arguments.getParcelableArrayList(BaseActivity.ARG_CLOUDY_BOOK_LIST);
        } else {
            LogUtil.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtil.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_cloudy_book_list, container, false);

        FloatingActionButton mAddButton = view.findViewById(R.id.cloudy_book_fab_add);
        mRecyclerView = view.findViewById(R.id.cloudy_book_list_view);
        FloatingActionButton mSyncButton = view.findViewById(R.id.cloudy_book_fab_sync);


        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);

        mAddButton.setOnClickListener(pickView -> mCallback.onCloudyBookListAddBook());
        mSyncButton.setOnClickListener(pickView -> mCallback.onCloudyBookListSynchronize());

        updateUI();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtil.debug(TAG, "++onDestroy()");
        mCloudyBookList = null;
    }

    /*
        Private Method(s)
     */
    private void updateUI() {

        if (mCloudyBookList == null || mCloudyBookList.size() == 0) {
            mCallback.onCloudyBookListPopulated(0);
        } else {
            LogUtil.debug(TAG, "++updateUI()");
            CloudyBookAdapter cloudyBookAdapter = new CloudyBookAdapter(mCloudyBookList);
            mRecyclerView.setAdapter(cloudyBookAdapter);
            mCallback.onCloudyBookListPopulated(cloudyBookAdapter.getItemCount());
        }
    }

    /**
     * Adapter class for CloudyBook objects
     */
    private class CloudyBookAdapter extends RecyclerView.Adapter<CloudyBookHolder> {

        private final List<CloudyBook> mCloudyBookList;

        CloudyBookAdapter(List<CloudyBook> cloudyBookList) {

            mCloudyBookList = cloudyBookList;
        }

        @NonNull
        @Override
        public CloudyBookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new CloudyBookHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull CloudyBookHolder holder, int position) {

            CloudyBook cloudyBook = mCloudyBookList.get(position);
            holder.bind(cloudyBook);
        }

        @Override
        public int getItemCount() {
            return mCloudyBookList.size();
        }
    }

    /**
     * Holder class for CloudyBook objects
     */
    private class CloudyBookHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mAuthorsTextView;
        private final TextView mCategoriesTextView;
        private final TextView mISBNTextView;
        private final ImageView mOwnImage;
        private final TextView mPublishedTextView;
        private final TextView mPublisherTextView;
        private final ImageView mReadImage;
        private final TextView mTitleTextView;

        private CloudyBook mCloudyBook;

        CloudyBookHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.cloudy_book_item, parent, false));

            mAuthorsTextView = itemView.findViewById(R.id.cloudy_book_item_authors);
            mCategoriesTextView = itemView.findViewById(R.id.cloudy_book_item_categories);
            mISBNTextView = itemView.findViewById(R.id.cloudy_book_item_isbn);
            mOwnImage = itemView.findViewById(R.id.cloudy_book_image_own);
            mPublishedTextView = itemView.findViewById(R.id.cloudy_book_item_published);
            mPublisherTextView = itemView.findViewById(R.id.cloudy_book_item_publisher);
            mReadImage = itemView.findViewById(R.id.cloudy_book_image_read);
            mTitleTextView = itemView.findViewById(R.id.cloudy_book_item_title);

            itemView.setOnClickListener(this);
        }

        void bind(CloudyBook cloudyBook) {

            mCloudyBook = cloudyBook;

            mAuthorsTextView.setText(mCloudyBook.getAuthorsDelimited());
            mCategoriesTextView.setVisibility(View.GONE);
            mISBNTextView.setText(
                String.format(
                    Locale.US,
                    getString(R.string.isbn_format),
                    mCloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? mCloudyBook.ISBN_8 : mCloudyBook.ISBN_13));
            if (mCloudyBook.IsOwned) {
                mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_owned_dark, null));
            } else {
                mOwnImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_not_owned_dark, null));
            }

            mPublishedTextView.setVisibility(View.GONE);
            mPublisherTextView.setVisibility(View.GONE);
            mReadImage.setVisibility(mCloudyBook.HasRead ? View.VISIBLE : View.INVISIBLE);
            mTitleTextView.setText(mCloudyBook.Title);
        }

        @Override
        public void onClick(View view) {

            LogUtil.debug(TAG, "++CloudyBookHolder::onClick(View)");
            mCallback.onCloudyBookListItemSelected(mCloudyBook);
        }
    }
}
