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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.viewmodel.BookListViewModel;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchContinue(BookEntity bookEntity);
    void onManualSearchContinue(String barcodeValue);
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mISBNEdit;

  private BookListViewModel mBookListViewModel;

  private String mBarcode;

  public static ManualSearchFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new ManualSearchFragment();
  }

  public static ManualSearchFragment newInstance(String barcodeValue) {

    Log.d(TAG, "++newInstance(String)");
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle arguments = new Bundle();
    arguments.putString(BaseActivity.ARG_BAR_CODE, barcodeValue);
    fragment.setArguments(arguments);
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
      mCallback = (OnManualSearchListener) context;
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
      if (arguments.containsKey(BaseActivity.ARG_BAR_CODE)) {
        mBarcode = arguments.getString(BaseActivity.ARG_BAR_CODE);
      }
    }

    mBookListViewModel = ViewModelProviders.of(this).get(BookListViewModel.class);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    Log.d(TAG, "++onCreateView(LayoutInflater, ViewGroup, Bundle)");
    return inflater.inflate(R.layout.fragment_manual_search, container, false);
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

    mISBNEdit = view.findViewById(R.id.manual_search_edit_isbn);
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);

    mContinueButton.setEnabled(false);
    mContinueButton.setOnClickListener(v ->
      mBookListViewModel.find(mISBNEdit.getText().toString()).observe(this, bookEntity -> {

          if (bookEntity != null) {
            mCallback.onManualSearchContinue(bookEntity);
          } else {
            mCallback.onManualSearchContinue(mISBNEdit.getText().toString());
          }
        }));

    // setup text change watchers
    mISBNEdit.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        validateAll();
      }
    });


    if (mBarcode != null && mBarcode.length() > 0) {
      mISBNEdit.setText(mBarcode);
    }
  }

  /*
    Private Method(s)
  */
  private void validateAll() {

    String testValue = mISBNEdit.getText().toString();
    if (testValue.length() == 8 || testValue.length() == 13) {
      mContinueButton.setEnabled(true);
    } else {
      mContinueButton.setEnabled(false);
    }
  }
}
