package net.whollynugatory.android.cloudycurator.db.entity;

import java.io.Serializable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(
  tableName = "publishers_table"
)
public class PublisherEntity implements Serializable {

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  public int Id;

  @ColumnInfo(name = "publisher_string")
  public String PublisherString;

  public PublisherEntity() {

    PublisherString = "";
  }
}
