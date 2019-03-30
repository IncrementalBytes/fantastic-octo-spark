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

package net.frostedbytes.android.cloudycurator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.frostedbytes.android.cloudycurator.utils.LogUtils;

public class BaseActivity extends AppCompatActivity {

    public static final String ARG_EMAIL = "email";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_USER_NAME = "user_name";
    public static final String DEFAULT_DATE = "0000-01-01";
    public static final String DEFAULT_ISBN = "000000000";
    public static final String DEFAULT_ID = "000000000-0000-0000-0000-000000000000";

    public static final String BASE_TAG = "CloudyCurator::";
    private static final String TAG = BASE_TAG + BaseActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        LogUtils.debug(TAG, "++onCreate(Bundle)");
    }
}
