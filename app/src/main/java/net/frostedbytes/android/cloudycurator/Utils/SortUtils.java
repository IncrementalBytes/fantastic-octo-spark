package net.frostedbytes.android.cloudycurator.utils;

import net.frostedbytes.android.cloudycurator.models.UserBook;

import java.util.Comparator;

public class SortUtils {

    public static class ByBookName implements Comparator<UserBook> {

        public int compare(UserBook a, UserBook b) {

            return a.Title.compareTo(b.Title);
        }
    }
}
