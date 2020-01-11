package net.whollynugatory.android.cloudycurator.db.repositories;

import android.util.Log;

import net.whollynugatory.android.cloudycurator.db.CuratorDatabase;
import net.whollynugatory.android.cloudycurator.db.dao.AuthorDao;
import net.whollynugatory.android.cloudycurator.db.entity.AuthorEntity;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.util.List;

import androidx.lifecycle.LiveData;

public class AuthorRepository {

  private static final String TAG = BaseActivity.BASE_TAG + "AuthorRepository";

  private static volatile AuthorRepository INSTANCE;

  private AuthorDao mAuthorDao;

  private AuthorRepository(AuthorDao authorDao) {

    mAuthorDao = authorDao;
  }

  static AuthorRepository getInstance(final AuthorDao authorDao) {

    Log.d(TAG, "++getInstance(AuthorDao)");
    if (INSTANCE == null) {
      synchronized (AuthorRepository.class) {
        if (INSTANCE == null) {
          INSTANCE = new AuthorRepository(authorDao);
        }
      }
    }

    return INSTANCE;
  }

  public LiveData<AuthorEntity> find(String authorName) {

    return mAuthorDao.find(authorName);
  }

  public LiveData<List<AuthorEntity>> getAll() {

    return mAuthorDao.getAllAuthors();
  }

  public void insert(AuthorEntity authorEntity) {

    CuratorDatabase.databaseWriteExecutor.execute(() -> mAuthorDao.insert(authorEntity));
  }
}
