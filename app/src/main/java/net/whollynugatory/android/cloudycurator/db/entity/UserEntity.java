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

package net.whollynugatory.android.cloudycurator.db.entity;

import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.Serializable;

public class UserEntity implements Serializable {

    public static final String ROOT = "Users";

    public String Email;

    public String FullName;

    public String Id;

    public boolean IsLibrarian;

    public boolean ShowBarcodeHint;

    public boolean UseImageCapture;

    public UserEntity() {

        Email = "";
        FullName = "";
        Id = BaseActivity.DEFAULT_ID;
        IsLibrarian = false;
        ShowBarcodeHint = true;
        UseImageCapture = false;
    }

    @Override
    public String toString() {
        return "User{" +
            "Email='" + Email + '\'' +
            ", FullName='" + FullName + '\'' +
            ", Id='" + Id + '\'' +
            '}';
    }
}
