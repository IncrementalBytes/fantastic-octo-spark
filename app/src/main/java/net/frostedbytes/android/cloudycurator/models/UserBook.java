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

package net.frostedbytes.android.cloudycurator.models;

import java.util.Objects;

public class UserBook extends Book {

    public boolean HasRead;

    public boolean IsOwned;

    public UserBook() {

        this.HasRead = false;
        this.IsOwned = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        UserBook userBook = (UserBook) o;
        return HasRead == userBook.HasRead &&
            IsOwned == userBook.IsOwned;
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), HasRead, IsOwned);
    }
}
