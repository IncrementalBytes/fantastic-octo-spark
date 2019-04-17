package net.frostedbytes.android.cloudycurator.utils;

import net.frostedbytes.android.cloudycurator.models.CloudyBook;

import java.util.Comparator;

public class SortUtils {

    public static class ByBookName implements Comparator<CloudyBook> {

        public int compare(CloudyBook a, CloudyBook b) {

            return a.Title.compareTo(b.Title);
        }
    }
}
