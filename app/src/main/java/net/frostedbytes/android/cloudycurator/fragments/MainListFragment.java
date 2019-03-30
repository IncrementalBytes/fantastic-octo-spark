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

import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import java.util.List;
import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class MainListFragment extends Fragment {

    private static final String TAG = BASE_TAG + MainListFragment.class.getSimpleName();

    public interface OnMainListListener {

        void onMainListPopulated(int size);

        void onMainListItemSelected();
    }

    private OnMainListListener mCallback;

    private RecyclerView mRecyclerView;

    private List<String> mMainSummaries;

    public static MainListFragment newInstance() {

        LogUtils.debug(TAG, "++newInstance()");
        MainListFragment fragment = new MainListFragment();
        Bundle args = new Bundle();
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
            mCallback = (OnMainListListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.ENGLISH, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_main_list, container, false);

        mRecyclerView = view.findViewById(R.id.main_list_view);

        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);

        updateUI();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
    }

    @Override
    public void onResume() {
        super.onResume();

        LogUtils.debug(TAG, "++onResume()");
        updateUI();
    }

    /*
        Private Method(s)
     */
    private void updateUI() {

        if (mMainSummaries != null && mMainSummaries.size() > 0) {
            LogUtils.debug(TAG, "++updateUI()");
            MainSummaryAdapter mainAdapter = new MainSummaryAdapter(mMainSummaries);
            mRecyclerView.setAdapter(mainAdapter);
            mCallback.onMainListPopulated(mainAdapter.getItemCount());
        } else {
            mCallback.onMainListPopulated(0);
        }
    }

    /**
     * Adapter class for MatchSummary objects
     */
    private class MainSummaryAdapter extends RecyclerView.Adapter<MainSummaryHolder> {

        private final List<String> mMainSummaries;

        MainSummaryAdapter(List<String> MainSummaries) {

            mMainSummaries = MainSummaries;
        }

        @NonNull
        @Override
        public MainSummaryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new MainSummaryHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull MainSummaryHolder holder, int position) {

            String MainSummary = mMainSummaries.get(position);
            holder.bind(MainSummary);
        }

        @Override
        public int getItemCount() {
            return mMainSummaries.size();
        }
    }

    /**
     * Holder class for MainSummary objects
     */
    private class MainSummaryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mTitleTextView;

        private String mMainSummary;

        MainSummaryHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.main_item, parent, false));

            mTitleTextView = itemView.findViewById(R.id.main_item_title);

            itemView.setOnClickListener(this);
        }

        void bind(String MainSummary) {

            mMainSummary = MainSummary;
        }

        @Override
        public void onClick(View view) {

            LogUtils.debug(TAG, "++MainSummaryHolder::onClick(View)");
            mCallback.onMainListItemSelected();
        }
    }
}