package net.frostedbytes.android.cloudycurator.utils;

import net.frostedbytes.android.cloudycurator.models.Book;

import java.util.Comparator;

public class SortUtils {

    public static class ByBookName implements Comparator<Book> {

        public int compare(Book a, Book b) {

            return a.Title.compareTo(b.Title);
        }
    }
}
