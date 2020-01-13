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

package net.whollynugatory.android.cloudycurator.db.views;

import java.io.Serializable;

import androidx.room.DatabaseView;

@DatabaseView(
  "SELECT Authors AS AuthorName, " +
    "COUNT() AS BookCount " +
    "FROM books_table GROUP BY AuthorName")
public class AuthorSummaryView implements Serializable {

  public String AuthorName;
  public int BookCount;

  public AuthorSummaryView() {

    AuthorName = "";
    BookCount = 0;
  }
}
