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

package net.whollynugatory.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.whollynugatory.android.cloudycurator.BaseActivity;
import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScanResultsFragment extends Fragment {

    private static final String TAG = BaseActivity.BASE_TAG + ScanResultsFragment.class.getSimpleName();

    public interface OnScanResultsListener {

        void onScanResultsPopulated(int size);

        void onScanResultsItemSelected(String searchText);
    }

    private OnScanResultsListener mCallback;

    private RecyclerView mRecyclerView;

    private ArrayList<String> mScanResults;

    public static ScanResultsFragment newInstance(ArrayList<String> scanResults) {

        LogUtils.debug(TAG, "++newInstance(ArrayList<>)");
        ScanResultsFragment fragment = new ScanResultsFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(BaseActivity.ARG_SCAN_RESULTS, scanResults);
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
            mCallback = (OnScanResultsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }

        Bundle arguments = getArguments();
        if (arguments != null) {
            mScanResults = arguments.getStringArrayList(BaseActivity.ARG_SCAN_RESULTS);
        } else {
            LogUtils.error(TAG, "Arguments were null.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_scan_results, container, false);

        mRecyclerView = view.findViewById(R.id.scan_list_view);

        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(manager);

        updateUI();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.debug(TAG, "++onDestroy()");
        mScanResults = null;
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

        if (mScanResults != null && mScanResults.size() > 0) {
            LogUtils.debug(TAG, "++updateUI()");
            ScanResultsAdapter scanResultsAdapter = new ScanResultsAdapter(mScanResults);
            mRecyclerView.setAdapter(scanResultsAdapter);
            mCallback.onScanResultsPopulated(scanResultsAdapter.getItemCount());
        } else {
            mCallback.onScanResultsPopulated(0);
        }
    }

    /**
     * Adapter class for MatchSummary objects
     */
    private class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsHolder> {

        private final List<String> mScanResults;

        ScanResultsAdapter(List<String> scanResults) {

            mScanResults = scanResults;
        }

        @NonNull
        @Override
        public ScanResultsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new ScanResultsHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ScanResultsHolder holder, int position) {

            String scanResult = mScanResults.get(position);
            holder.bind(scanResult);
        }

        @Override
        public int getItemCount() {
            return mScanResults.size();
        }
    }

    /**
     * Holder class for scan result objects
     */
    private class ScanResultsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mSearchTermTextView;

        private String mScanResult;

        ScanResultsHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.text_item, parent, false));

            itemView.setOnClickListener(this);
            mSearchTermTextView = itemView.findViewById(R.id.text_item_search_term);
        }

        void bind(String scanResult) {

            mScanResult = scanResult;
            mSearchTermTextView.setText(scanResult);
        }

        @Override
        public void onClick(View view) {

            LogUtils.debug(TAG, "++ScanResultsHolder::onClick(View)");
            mCallback.onScanResultsItemSelected(mScanResult);
        }
    }
}
