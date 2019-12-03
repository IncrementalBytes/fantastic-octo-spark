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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.whollynugatory.android.cloudycurator.R;

import static net.whollynugatory.android.cloudycurator.ui.BaseActivity.BASE_TAG;

public class LibrarianFragment extends Fragment {

  private static final String TAG = BASE_TAG + LibrarianFragment.class.getSimpleName();

  public static LibrarianFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new LibrarianFragment();
  }

  /*
      Fragment Override(s)
   */
  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_librarian, container, false);
  }
}
