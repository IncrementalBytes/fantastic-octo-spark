package net.whollynugatory.android.cloudycurator.common;

import android.os.AsyncTask;
import android.util.Log;

import net.whollynugatory.android.cloudycurator.db.CuratorRepository;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;
import net.whollynugatory.android.cloudycurator.ui.MainActivity;

import java.lang.ref.WeakReference;

public class QueryBookDatabaseTask extends AsyncTask<Void, Void, BookDetail> {

  private static final String TAG = BaseActivity.BASE_TAG + "QueryBookDatabaseTask";

  private WeakReference<MainActivity> mActivityWeakReference;
  private BookEntity mBookEntity;
  private CuratorRepository mRepository;

  public QueryBookDatabaseTask(MainActivity activityContext, CuratorRepository repository, BookEntity bookEntity) {

    Log.d(TAG, "++QueryBookDatabaseTask(MainActivity, CuratorRepository, BookDetail)");
    mActivityWeakReference = new WeakReference<>(activityContext);
    mBookEntity = bookEntity;
    mRepository = repository;
  }

  protected BookDetail doInBackground(Void... params) {

    Log.d(TAG, "++doInBackground(Void...)");
    BookDetail bookDetail;
    if (!mBookEntity.VolumeId.equals(BaseActivity.DEFAULT_VOLUME_ID)) {
      bookDetail = mRepository.getBook(mBookEntity.VolumeId);
    } else if (!mBookEntity.ISBN_13.equals(BaseActivity.DEFAULT_ISBN_13)) {
      bookDetail = mRepository.getBook(mBookEntity.ISBN_13);
    } else {
      bookDetail = mRepository.getBook(mBookEntity.ISBN_8);
    }

    return bookDetail == null ? BookDetail.fromBookEntity(mBookEntity) : bookDetail;
  }

  protected void onPostExecute(BookDetail bookDetail) {

    Log.d(TAG, "++onPostExecute(BookEntity)");
    MainActivity activity = mActivityWeakReference.get();
    if (activity == null) {
      Log.e(TAG, "MainActivity is null or detached.");
      return;
    }

    activity.queryBookDatabaseComplete(bookDetail);
  }
}
