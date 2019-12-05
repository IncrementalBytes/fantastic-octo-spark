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
package net.whollynugatory.android.cloudycurator.db.viewmodel;

import android.app.Application;

import net.whollynugatory.android.cloudycurator.db.CuratorRepository;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class BookViewModel extends AndroidViewModel {

  private CuratorRepository mRepository;

  private LiveData<List<BookEntity>> mAllBooks;

  public BookViewModel(Application application) {
    super(application);

    mRepository = CuratorRepository.getInstance(application);
    mAllBooks = mRepository.getAllBooks();
  }

  public LiveData<List<BookEntity>> getAllBooks() {
    return mAllBooks;
  }

  public void insertBook(BookEntity bookEntity) {

    mRepository.insertBookEntity(bookEntity);
  }
}
