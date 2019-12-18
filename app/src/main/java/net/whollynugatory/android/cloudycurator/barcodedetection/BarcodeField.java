/*
 * Copyright 2019 Ryan Ward
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.whollynugatory.android.cloudycurator.barcodedetection;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about a barcode field.
 **/
public class BarcodeField implements Parcelable {

  public static final Creator<BarcodeField> CREATOR =
    new Creator<BarcodeField>() {

      @Override
      public BarcodeField createFromParcel(Parcel in) {
        return new BarcodeField(in);
      }

      @Override
      public BarcodeField[] newArray(int size) {
        return new BarcodeField[size];
      }
    };

  final String Label;
  final String Value;

  public BarcodeField(String label, String value) {

    Label = label;
    Value = value;
  }

  private BarcodeField(Parcel in) {

    Label = in.readString();
    Value = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {

    dest.writeString(Label);
    dest.writeString(Value);
  }
}
