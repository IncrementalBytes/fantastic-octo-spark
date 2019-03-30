package net.frostedbytes.android.cloudycurator.models;

import com.google.firebase.database.Exclude;

import net.frostedbytes.android.cloudycurator.BaseActivity;

import java.io.Serializable;

public class Book implements Serializable {

    @Exclude
    public static final String ROOT = "Books";

    public long AddedDate;

    public String AuthorId;

    public String Id;

    public String ISBN;

    public String Title;

    public long UpdatedDate;

    public Book() {

        this.AddedDate = 0;
        this.AuthorId = BaseActivity.DEFAULT_ID;
        this.Id = BaseActivity.DEFAULT_ID;
        this.ISBN = "";
        this.Title = "";
        this.UpdatedDate = 0;
    }
}
