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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.frostedbytes.android.cloudycurator.BaseActivity;
import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.utils.LogUtil;

import java.util.Locale;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class QueryFragment extends Fragment {

    private static final String TAG = BASE_TAG + QueryFragment.class.getSimpleName();

    public interface OnQueryListener {

        void onQueryActionComplete(String message);

        void onQueryShowManualDialog();

        void onQueryTakePicture(int scanType);
    }

    private OnQueryListener mCallback;

    public static QueryFragment newInstance() {

        LogUtil.debug(TAG, "++newInstance()");
        return new QueryFragment();
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        LogUtil.debug(TAG, "++onAttach(Context)");
        try {
            mCallback = (OnQueryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtil.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        View view = inflater.inflate(R.layout.fragment_book_query, container, false);

        CardView scanISBNCard = view.findViewById(R.id.query_card_isbn);
        CardView scanTitleCard = view.findViewById(R.id.query_card_text);
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                scanISBNCard.setOnClickListener(v -> mCallback.onQueryTakePicture(BaseActivity.SCAN_ISBN));

                scanTitleCard.setOnClickListener(v -> mCallback.onQueryTakePicture(BaseActivity.SCAN_TEXT));
            } else {
                String message = "Camera feature is not available; disabling camera.";
                LogUtil.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
                scanISBNCard.setEnabled(false);
                scanTitleCard.setEnabled(false);
            }
        } else {
            String message = "Camera not detected.";
            LogUtil.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
            scanISBNCard.setEnabled(false);
            scanTitleCard.setEnabled(false);
        }

        CardView manualCard = view.findViewById(R.id.query_card_manual);
        manualCard.setOnClickListener(v -> mCallback.onQueryShowManualDialog());

        mCallback.onQueryActionComplete("");
        return view;
    }
}
