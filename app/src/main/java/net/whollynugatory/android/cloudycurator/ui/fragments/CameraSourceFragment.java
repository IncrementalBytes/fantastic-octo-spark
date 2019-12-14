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
package net.whollynugatory.android.cloudycurator.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.whollynugatory.android.cloudycurator.R;
import net.whollynugatory.android.cloudycurator.common.BarcodeProcessor;
import net.whollynugatory.android.cloudycurator.common.CameraSource;
import net.whollynugatory.android.cloudycurator.common.CameraSourcePreview;
import net.whollynugatory.android.cloudycurator.common.GraphicOverlay;
import net.whollynugatory.android.cloudycurator.ui.BaseActivity;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class CameraSourceFragment extends Fragment {

  private static final String TAG = BaseActivity.BASE_TAG + "CameraSourceFragment";

  private CameraSource mCameraSource;
  private CameraSourcePreview mPreview;
  private GraphicOverlay mGraphicOverlay;

  public static CameraSourceFragment newInstance() {

    Log.d(TAG, "++newInstance()");
    return new CameraSourceFragment();
  }

  /*
  Fragment Override(s)
  */
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    Log.d(TAG, "++onAttach(Context)");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "++onCreate(Bundle)");
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    return inflater.inflate(R.layout.fragment_camera_source, container, false);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "++onDetach()");

    if (mCameraSource != null) {
      mCameraSource.release();
    }

    if (mGraphicOverlay != null) {
      mGraphicOverlay.clear();
    }

    if (mPreview != null) {
      mPreview.release();
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mPreview = view.findViewById(R.id.camera_preview);
    mGraphicOverlay = view.findViewById(R.id.camera_graphic_overlay);

    if (mCameraSource == null) {
      mCameraSource = new CameraSource(getActivity(), mGraphicOverlay);
    }

    mCameraSource.setMachineLearningFrameProcessor(new BarcodeProcessor(getContext()));
    if (mCameraSource != null) {
      try {
        if (mPreview != null && mGraphicOverlay != null) {
          mPreview.start(mCameraSource, mGraphicOverlay);
        } else {
          Log.e(TAG, "ImagePreview and/or GraphicsOverlay are not initialized.");
        }
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        mCameraSource.release();
        mCameraSource = null;
      }
    }
  }
}
