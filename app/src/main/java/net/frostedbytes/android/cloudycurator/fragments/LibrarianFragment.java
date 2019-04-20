package net.frostedbytes.android.cloudycurator.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.frostedbytes.android.cloudycurator.R;
import net.frostedbytes.android.cloudycurator.utils.LogUtils;

import static net.frostedbytes.android.cloudycurator.BaseActivity.BASE_TAG;

public class LibrarianFragment extends Fragment {

    private static final String TAG = BASE_TAG + LibrarianFragment.class.getSimpleName();

    public static LibrarianFragment newInstance() {

        LogUtils.debug(TAG, "++newInstance()");
        LibrarianFragment fragment = new LibrarianFragment();
        return fragment;
    }

    /*
        Fragment Override(s)
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        LogUtils.debug(TAG, "++onAttach(Context)");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LogUtils.debug(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
        final View view = inflater.inflate(R.layout.fragment_librarian, container, false);
        return view;
    }
}
