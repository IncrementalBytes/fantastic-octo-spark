package net.frostedbytes.android.cloudycurator.models;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.cloudycurator.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class Book {

    public List<String> Authors;

    public String ISBN_8;

    public String ISBN_13;

    public String LCCN;

    public String Title;

    /**
     * VolumeId is the identifier of the object and key in Firestore
     */
    @Exclude
    public String VolumeId;

    public Book () {
        Authors = new ArrayList<>();
        ISBN_8 = BaseActivity.DEFAULT_ISBN_8;
        ISBN_13 = BaseActivity.DEFAULT_ISBN_13;
        LCCN = BaseActivity.DEFAULT_LCCN;
        Title = "";
    }
}
