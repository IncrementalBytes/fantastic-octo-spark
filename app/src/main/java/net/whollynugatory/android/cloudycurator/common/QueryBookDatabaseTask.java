package net.whollynugatory.android.cloudycurator.common;

import android.os.AsyncTask;
import android.util.Log;

import net.whollynugatory.android.cloudycurator.db.CuratorRepository;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.ui.LiveBarcodeScanningActivity;
import net.whollynugatory.android.cloudycurator.ui.MainActivity;

import java.lang.ref.WeakReference;

public class QueryBookDatabaseTask extends AsyncTask<Void, Void, BookDetail> {

  private static final String TAG = BaseActivity.BASE_TAG + "QueryBookDatabaseTask";

  private WeakReference<LiveBarcodeScanningActivity> mActivityWeakReference;
  private BookEntity mBookEntity;
  private CuratorRepository mRepository;

  public QueryBookDatabaseTask(LiveBarcodeScanningActivity activityContext, CuratorRepository repository, BookEntity bookEntity) {

    Log.d(TAG, "++QueryBookDatabaseTask(LiveBarcodeScanningActivity, CuratorRepository, BookDetail)");
    mActivityWeakReference = new WeakReference<>(activityContext);
    mBookEntity = bookEntity;
    mRepository = repository;
  }

  protected BookDetail doInBackground(Void... params) {

    Log.d(TAG, "++doInBackground(Void...)");
    BookDetail bookDetail = mRepository.getBook(mBookEntity.VolumeId);
    return bookDetail == null ? BookDetail.fromBookEntity(mBookEntity) : bookDetail;
  }

  protected void onPostExecute(BookDetail bookDetail) {

    Log.d(TAG, "++onPostExecute(BookEntity)");
    LiveBarcodeScanningActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.queryBookDatabaseComplete(bookDetail);
  }
}
