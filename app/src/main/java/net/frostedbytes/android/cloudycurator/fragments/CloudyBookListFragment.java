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

public class CloudyBookListFragment extends Fragment {

    private static final String TAG = BASE_TAG + CloudyBookListFragment.class.getSimpleName();

    public interface OnCloudyBookListListener {

        void onCloudyBookListItemSelected(CloudyBook cloudyBook);

        void onCloudyBookListPopulated(int size);
    }

    private OnCloudyBookListListener mCallback;

    private RecyclerView mRecyclerView;

    private ArrayList<CloudyBook> mCloudyBookList;

    public static CloudyBookListFragment newInstance(ArrayList<CloudyBook> cloudyBookList) {

        LogUtils.debug(TAG, "++newInstance(%d)", cloudyBookList.size());
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

        LogUtils.debug(TAG, "++onAttach(Context)");
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
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_cloudy_book_list, container, false);

        mRecyclerView = view.findViewById(R.id.cloudy_book_list_view);

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

    @Override
    public void onResume() {
        super.onResume();

        LogUtils.debug(TAG, "++onResume()");
        //updateUI();
    }

    /*
        Private Method(s)
     */
    private void updateUI() {

        if (mCloudyBookList == null || mCloudyBookList.size() == 0) {
            mCallback.onCloudyBookListPopulated(0);
        } else {
            LogUtils.debug(TAG, "++updateUI()");
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
        private final TextView mTitleTextView;

        private CloudyBook mCloudyBook;

        CloudyBookHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.book_item, parent, false));

            mAuthorsTextView = itemView.findViewById(R.id.book_item_authors);
            mTitleTextView = itemView.findViewById(R.id.book_item_title);

            itemView.setOnClickListener(this);
        }

        void bind(CloudyBook cloudyBook) {

            mCloudyBook = cloudyBook;

            if (mCloudyBook.Authors.size() > 0) {
                mAuthorsTextView.setText(mCloudyBook.Authors.get(0));
            }

            mTitleTextView.setText(mCloudyBook.Title);
        }

        @Override
        public void onClick(View view) {

            LogUtils.debug(TAG, "++CloudyBookHolder::onClick(View)");
            mCallback.onCloudyBookListItemSelected(mCloudyBook);
        }
    }
}
