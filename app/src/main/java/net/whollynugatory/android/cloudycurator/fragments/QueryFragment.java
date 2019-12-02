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
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.LogUtils;
import net.whollynugatory.android.cloudycurator.BaseActivity;

import java.util.Locale;

public class QueryFragment extends Fragment {

    private static final String TAG = BaseActivity.BASE_TAG + QueryFragment.class.getSimpleName();

    public interface OnQueryListener {

        void onQueryActionComplete(String message);

        void onQueryShowManualDialog();

        void onQueryTakePicture();
    }

    private OnQueryListener mCallback;

    public static QueryFragment newInstance() {

        LogUtils.debug(TAG, "++newInstance()");
        return new QueryFragment();
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        LogUtils.debug(TAG, "++onAttach(Context)");
        try {
            mCallback = (OnQueryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        View view = inflater.inflate(R.layout.fragment_book_query, container, false);

        CardView scanPhotoCard = view.findViewById(R.id.query_card_photo);
        if (getActivity() != null) {
            PackageManager packageManager = getActivity().getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                scanPhotoCard.setOnClickListener(v -> mCallback.onQueryTakePicture());
            } else {
                String message = "Camera feature is not available; disabling camera.";
                LogUtils.warn(TAG, message);
                mCallback.onQueryActionComplete(message);
                scanPhotoCard.setEnabled(false);
            }
        } else {
            String message = "Camera not detected.";
            LogUtils.warn(TAG, message);
            mCallback.onQueryActionComplete(message);
            scanPhotoCard.setEnabled(false);
        }

        CardView manualCard = view.findViewById(R.id.query_card_manual);
        manualCard.setOnClickListener(v -> mCallback.onQueryShowManualDialog());

        mCallback.onQueryActionComplete("");
        return view;
    }
}
