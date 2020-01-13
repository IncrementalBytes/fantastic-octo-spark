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

package net.whollynugatory.android.cloudycurator.ui;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

  public static final String ARG_BOOK = "book";
  public static final String ARG_DEBUG_FILE_NAME = "debug_file_name";
  public static final String ARG_EMAIL = "email";
  public static final String ARG_FIREBASE_USER_ID = "firebase_user_id";
  public static final String ARG_LIST_BOOK = "list_book";
  public static final String ARG_LIST_TYPE = "list_type";
  public static final String ARG_MESSAGE = "message";
  public static final String ARG_USER_NAME = "user_name";

  public static final String DEFAULT_ISBN_8 = "00000000";
  public static final String DEFAULT_ISBN_13 = "0000000000000";
  public static final String DEFAULT_ID = "0000000000000000000000000000";
  public static final String DEFAULT_LCCN = "0000000000";
  public static final String DEFAULT_VOLUME_ID = "000000000000";

  public static final String DATABASE_NAME = "curator-db.sqlite";
  public static final int MAX_RESULTS = 10;

  public static final int REQUEST_IMAGE_CAPTURE = 4201;
  public static final int REQUEST_BOOK_ADD = 4202;

  public static final int REQUEST_CAMERA_PERMISSIONS = 4701;
  public static final int REQUEST_STORAGE_PERMISSIONS = 4702;

  public static final int RESULT_ADD_SUCCESS = 4900;
  public static final int RESULT_ADD_FAILED = 4901;

  public static final String BASE_TAG = "CloudyCurator::";
}
