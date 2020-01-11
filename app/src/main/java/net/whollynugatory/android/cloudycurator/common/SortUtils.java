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
package net.whollynugatory.android.cloudycurator.common;

import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;
import net.whollynugatory.android.cloudycurator.db.views.BookDetail;

import java.util.Comparator;

public class SortUtils {

    public static class ByBookName implements Comparator<BookEntity> {

        public int compare(BookEntity a, BookEntity b) {

            return a.Title.compareTo(b.Title);
        }
    }

    public static class ByRecent implements Comparator<BookDetail> {

        public int compare(BookDetail a, BookDetail b) {

            return Long.compare(a.DateAdded, b.DateAdded);
        }
    }
}
