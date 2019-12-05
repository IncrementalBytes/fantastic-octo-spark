package net.whollynugatory.android.cloudycurator.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class ManualSearchFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "ManualSearchFragment";

  public interface OnManualSearchListener {

    void onManualSearchActionComplete(BookEntity bookEntity);

    void onManualSearchRetry();
  }

  private OnManualSearchListener mCallback;

  private Button mContinueButton;
  private EditText mIssueCodeEdit;
  private EditText mProductCodeEdit;

  private BookEntity mBookEntity;

  public static ManualSearchFragment newInstance(BookEntity bookEntity) {

    Log.d(TAG, "++newInstance()");
    ManualSearchFragment fragment = new ManualSearchFragment();
    Bundle args = new Bundle();
    args.putSerializable(BaseActivity.ARG_BOOK, bookEntity);
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
      mBookEntity = (BookEntity) arguments.getSerializable(BaseActivity.ARG_BOOK);
    } else {
      Log.e(TAG, "Arguments were null.");
    }
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

    // 2 Scenarios:
    //   1) Product Code (Publisher & Series) is known, we need the IssueCode
    //   2) We need Product Code (Publisher & Series), & IssueCode

    Button retryButton = view.findViewById(R.id.manual_search_button_retry);
    retryButton.setOnClickListener(v -> mCallback.onManualSearchRetry());
    mContinueButton = view.findViewById(R.id.manual_search_button_continue);
    mContinueButton.setEnabled(false);
//    mContinueButton.setOnClickListener(v -> {
//      mComicBook = new ComicBook();
//      mComicBook.parseProductCode(
//        String.format(
//          Locale.US,
//          "%s-%s",
//          mProductCodeEdit.getText().toString(),
//          mIssueCodeEdit.getText().toString()));
//      mCallback.onManualSearchActionComplete(mComicBook);
//    });
//
//    TextView productCodeExampleText = view.findViewById(R.id.manual_search_text_product_example);
//    ImageView productCodeImage = view.findViewById(R.id.manual_search_image_product);
//    mProductCodeEdit = view.findViewById(R.id.manual_search_edit_product);
//    TextView issueCodeText = view.findViewById(R.id.manual_search_text_issue);
//    TextView issueCodeExampleText = view.findViewById(R.id.manual_search_text_issue_example);
//    ImageView issueCodeImage = view.findViewById(R.id.manual_search_image_issue);
//    mIssueCodeEdit = view.findViewById(R.id.manual_search_edit_issue);
//    TextView messageText = view.findViewById(R.id.manual_search_text_no_barcode);
//
//    if (mComicBook == null) {
//      // we need both ProductCode and IssueCode
//      messageText.setVisibility(View.VISIBLE);
//      productCodeExampleText.setVisibility(View.VISIBLE);
//      productCodeImage.setVisibility(View.VISIBLE);
//      issueCodeExampleText.setVisibility(View.VISIBLE);
//      issueCodeImage.setVisibility(View.VISIBLE);
//    } else {
//      if (mComicBook.ProductCode.equals(BaseActivity.DEFAULT_PRODUCT_CODE) ||
//        mComicBook.ProductCode.length() != BaseActivity.DEFAULT_PRODUCT_CODE.length()) { // we need ProductCode
//        productCodeExampleText.setVisibility(View.VISIBLE);
//        productCodeImage.setVisibility(View.VISIBLE);
//      } else {
//        messageText.setVisibility(View.GONE);
//        productCodeExampleText.setVisibility(View.GONE);
//        productCodeImage.setVisibility(View.GONE);
//        mProductCodeEdit.setText(mComicBook.ProductCode);
//        mProductCodeEdit.setEnabled(false);
//      }
//
//      if (mComicBook.IssueCode.equals(BaseActivity.DEFAULT_ISSUE_CODE) ||
//        mComicBook.IssueCode.length() != BaseActivity.DEFAULT_ISSUE_CODE.length()) {
//        issueCodeText.setVisibility(View.VISIBLE);
//        issueCodeImage.setVisibility(View.VISIBLE);
//      } else {
//        messageText.setVisibility(View.GONE);
//        issueCodeExampleText.setVisibility(View.GONE);
//        issueCodeImage.setVisibility(View.GONE);
//        mIssueCodeEdit.setText(mComicBook.IssueCode);
//        mIssueCodeEdit.setEnabled(false);
//      }
//    }
//
//    // setup text change watchers
//    mProductCodeEdit.addTextChangedListener(new TextWatcher() {
//
//      @Override
//      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//      }
//
//      @Override
//      public void onTextChanged(CharSequence s, int start, int before, int count) {
//      }
//
//      @Override
//      public void afterTextChanged(Editable s) {
//        validateAll();
//      }
//    });
//
//    mIssueCodeEdit.addTextChangedListener(new TextWatcher() {
//
//      @Override
//      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//      }
//
//      @Override
//      public void onTextChanged(CharSequence s, int start, int before, int count) {
//      }
//
//      @Override
//      public void afterTextChanged(Editable s) {
//        validateAll();
//      }
//    });
  }

  /*
    Private Method(s)
  */
  private void validateAll() {

//    if (mProductCodeEdit.getText().toString().length() == BaseActivity.DEFAULT_PRODUCT_CODE.length() &&
//      !mProductCodeEdit.getText().toString().equals(BaseActivity.DEFAULT_PRODUCT_CODE) &&
//      mIssueCodeEdit.getText().toString().length() == BaseActivity.DEFAULT_ISSUE_CODE.length() &&
//      !mIssueCodeEdit.getText().toString().equals(BaseActivity.DEFAULT_ISSUE_CODE)) {
//      mContinueButton.setEnabled(true);
//    } else {
      mContinueButton.setEnabled(false);
//    }
  }
}
