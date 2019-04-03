package net.frostedbytes.android.cloudycurator.models;

import com.google.firebase.firestore.Exclude;

import net.frostedbytes.android.cloudycurator.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class User {

    @Exclude
    public static final String ROOT = "Users";

    @Exclude
    public String Email;

    @Exclude
    public String FullName;

    @Exclude
    public String Id;

    public List<UserBook> UserBooks;

    public User() {

        Email = "";
        FullName = "";
        Id = BaseActivity.DEFAULT_ID;
        UserBooks = new ArrayList<>();
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
