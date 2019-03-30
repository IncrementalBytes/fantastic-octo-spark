package net.frostedbytes.android.cloudycurator.models;

import com.google.firebase.database.Exclude;

import net.frostedbytes.android.cloudycurator.BaseActivity;

import java.util.HashMap;

public class User {

    @Exclude
    public static final String ROOT = "Users";

    public HashMap<String, String> Books;

    @Exclude
    public String Email;

    @Exclude
    public String FullName;

    @Exclude
    public String Id;

    public User() {

        this.Books = new HashMap<>();
        this.Id = BaseActivity.DEFAULT_ID;
    }
}
