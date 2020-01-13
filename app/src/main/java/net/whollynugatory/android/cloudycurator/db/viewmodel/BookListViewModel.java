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

import net.whollynugatory.android.cloudycurator.db.CuratorDatabase;
import net.whollynugatory.android.cloudycurator.db.data.BookAuthor;
import net.whollynugatory.android.cloudycurator.db.data.BookCategory;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.repositories.BookRepository;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class BookListViewModel extends AndroidViewModel {

  private BookRepository mRepository;
  private LiveData<List<BookEntity>> mAllBooks;

  public BookListViewModel(Application application) {
    super(application);

    mRepository = BookRepository.getInstance(CuratorDatabase.getInstance(application).bookDao());
    mAllBooks = mRepository.getRecent();
  }

  public void delete(String volumeId) {

    mRepository.delete(volumeId);
  }

  public LiveData<BookEntity> find(String barcodeValue) {

    return mRepository.find(barcodeValue);
  }

  public LiveData<List<BookAuthor>> getAllByAuthors() {

    return mRepository.getByAuthors();
  }

  public LiveData<List<BookCategory>> getAllByCategories() {

    return mRepository.getByCategories();
  }

  public LiveData<List<BookEntity>> getAll() {

    return mAllBooks;
  }

  public void insert(BookEntity bookEntity) {

    mRepository.insert(bookEntity);
  }

  public void update(BookEntity bookEntity) {

    mRepository.update(bookEntity);
  }
}
