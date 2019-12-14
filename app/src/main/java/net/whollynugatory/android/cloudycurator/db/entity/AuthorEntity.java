package net.whollynugatory.android.cloudycurator.db.entity;

import java.io.Serializable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
  tableName = "authors_table"
)
public class AuthorEntity implements Serializable {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  public long Id;

  @ColumnInfo(name = "author_string")
  public String AuthorString;

  public AuthorEntity() {

    AuthorString = "";
  }

  public AuthorEntity(String authorString) {

    AuthorString = authorString;
  }
}
