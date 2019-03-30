package net.frostedbytes.android.cloudycurator.models;

import com.google.firebase.database.Exclude;

import net.frostedbytes.android.cloudycurator.BaseActivity;

public class Author {

    @Exclude
    public static final String ROOT = "Authors";

    public long AddedDate;

    public String Id;

    public String Name;

    public long UpdatedDate;

    public Author() {

        this.AddedDate = 0;
        this.Id = BaseActivity.DEFAULT_ID;
        this.Name = "";
        this.UpdatedDate = 0;
    }
}
