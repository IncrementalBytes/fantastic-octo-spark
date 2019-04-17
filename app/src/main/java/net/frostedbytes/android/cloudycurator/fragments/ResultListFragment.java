package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.models.CloudyBook;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class ResultListFragment extends Fragment {

    private static final String TAG = BASE_TAG + ResultListFragment.class.getSimpleName();

    public interface OnResultListListener {

        void onResultListActionComplete(String message);

        void onResultListItemSelected(CloudyBook cloudyBook);

        void onResultListPopulated(int size);
    }

    private OnResultListListener mCallback;

    private RecyclerView mRecyclerView;

    private ArrayList<CloudyBook> mCloudyBookList;

    public static ResultListFragment newInstance(ArrayList<CloudyBook> cloudyBookList) {

        LogUtils.debug(TAG, "++newInstance(%d)", cloudyBookList.size());
        ResultListFragment fragment = new ResultListFragment();
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

        LogUtils.debug(TAG, "++onAttach(Context)");
        try {
            mCallback = (OnResultListListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mCloudyBookList = arguments.getParcelableArrayList(BaseActivity.ARG_CLOUDY_BOOK_LIST);
        } else {
            String message = "Arguments were null.";
            LogUtils.error(TAG, message);
            mCallback.onResultListActionComplete(message);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
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

        LogUtils.debug(TAG, "++onDestroy()");
        mCloudyBookList = null;
    }

    /*
        Private Method(s)
     */
    private void updateUI() {

        if (mCloudyBookList == null || mCloudyBookList.size() == 0) {
            mCallback.onResultListPopulated(0);
        } else {
            LogUtils.debug(TAG, "++updateUI()");
            ResultAdapter resultAdapter = new ResultAdapter(mCloudyBookList);
            mRecyclerView.setAdapter(resultAdapter);
            mCallback.onResultListPopulated(resultAdapter.getItemCount());
        }
    }

    /**
     * Adapter class for query result objects
     */
    private class ResultAdapter extends RecyclerView.Adapter<ResultHolder> {

        private final List<CloudyBook> mCloudyBookList;

        ResultAdapter(List<CloudyBook> cloudyBookList) {

            mCloudyBookList = cloudyBookList;
        }

        @NonNull
        @Override
        public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new ResultHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultHolder holder, int position) {

            CloudyBook cloudyBook = mCloudyBookList.get(position);
            holder.bind(cloudyBook);
        }

        @Override
        public int getItemCount() {
            return mCloudyBookList.size();
        }
    }

    /**
     * Holder class for query result object
     */
    private class ResultHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mAuthorsTextView;
        private final TextView mCategoriesTextView;
        private final TextView mISBNTextView;
        private final TextView mPublishedTextView;
        private final TextView mPublisherTextView;
        private final TextView mTitleTextView;

        private CloudyBook mCloudyBook;

        ResultHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.cloudy_book_item, parent, false));

            mAuthorsTextView = itemView.findViewById(R.id.cloudy_book_item_authors);
            mCategoriesTextView = itemView.findViewById(R.id.cloudy_book_item_categories);
            mISBNTextView = itemView.findViewById(R.id.cloudy_book_item_isbn);
            mPublishedTextView = itemView.findViewById(R.id.cloudy_book_item_published);
            mPublisherTextView = itemView.findViewById(R.id.cloudy_book_item_publisher);
            mTitleTextView = itemView.findViewById(R.id.cloudy_book_item_title);

            itemView.setOnClickListener(this);
        }

        void bind(CloudyBook cloudyBook) {

            mCloudyBook = cloudyBook;

            mAuthorsTextView.setText(mCloudyBook.getAuthorsDelimited());
            mCategoriesTextView.setText(
                String.format(
                    Locale.US,
                    getString(R.string.categories_format),
                    mCloudyBook.getCategoriesDelimited()));
            mISBNTextView.setText(
                String.format(
                    Locale.US,
                    getString(R.string.isbn_format),
                    mCloudyBook.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13) ? mCloudyBook.ISBN_8 : mCloudyBook.ISBN_13));
            mPublishedTextView.setText(String.format(Locale.US, getString(R.string.published_date_format), mCloudyBook.PublishedDate));
            if (mCloudyBook.Publisher == null || mCloudyBook.Publisher.isEmpty()) {
                mPublisherTextView.setVisibility(View.GONE);
            } else {
                mPublisherTextView.setText(String.format(Locale.US, getString(R.string.publisher_format), mCloudyBook.Publisher));
            }

            mTitleTextView.setText(mCloudyBook.Title);
        }

        @Override
        public void onClick(View view) {

            LogUtils.debug(TAG, "++ResultHolder::onClick(View)");
            mCallback.onResultListItemSelected(mCloudyBook);
        }
    }
}