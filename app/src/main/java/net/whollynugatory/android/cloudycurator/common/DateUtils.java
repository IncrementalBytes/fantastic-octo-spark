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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /**
     * Returns a user-friendly readable string of the date.
     *
     * @param date - Date; in ticks
     * @return - User-friendly readable string of the date; formatted YYYY-MM-dd
     */
    public static String formatDateForDisplay(long date) {

        Date temp = new Date(date);
        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd", Locale.US);
        return dateFormat.format(temp);
    }
}
