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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class TutorialFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "TutorialFragment";

  public interface OnTutorialListener {

    void onTutorialContinue();

    void onTutorialShowHint(boolean show);
  }

  private OnTutorialListener mCallback;

  private boolean mShowBarcodeHint;

  public static TutorialFragment newInstance(boolean showBarcodeHint) {

    Log.d(TAG, "++newInstance()");
    TutorialFragment fragment = new TutorialFragment();
    Bundle args = new Bundle();
    args.putBoolean(BaseActivity.ARG_BARCODE_HINT, showBarcodeHint);
    fragment.setArguments(args);
    return fragment;
  }

  /*
  Fragment Override(s)
  */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
    try {
      mCallback = (OnTutorialListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(
        String.format(Locale.US, "Missing interface implementations for %s", context.toString()));
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
    Bundle arguments = getArguments();
    if (arguments != null) {
      mShowBarcodeHint = arguments.getBoolean(BaseActivity.ARG_BARCODE_HINT);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_tutorial, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();

    Log.d(TAG, "++onDetach()");
    mCallback = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    Log.d(TAG, "++onViewCreated(View, Bundle)");
    Switch showHintSwitch = view.findViewById(R.id.tutorial_switch_hide);
    showHintSwitch.setChecked(mShowBarcodeHint);
    showHintSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mCallback.onTutorialShowHint(isChecked));
    Button continueButton = view.findViewById(R.id.tutorial_button_continue);
    continueButton.setOnClickListener(v -> mCallback.onTutorialContinue());
  }
}
