package net.whollynugatory.android.cloudycurator.db.data;

import net.whollynugatory.android.cloudycurator.db.entity.AuthorEntity;
import net.whollynugatory.android.cloudycurator.db.entity.BookEntity;

import java.util.List;

import androidx.room.Embedded;
import androidx.room.Relation;

public class AuthorAndBooks {
  @Embedded
  public AuthorEntity Author;

  @Relation(parentColumn = "AuthorId", entityColumn = "author_id")
  public List<BookEntity> Books;
}
