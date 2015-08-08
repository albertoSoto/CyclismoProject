/*
*    Copyright (c) 2013, Will Szumski
*    Copyright (c) 2013, Doug Szumski
*
*    This file is part of Cyclismo.
*
*    Cyclismo is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    Cyclismo is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with Cyclismo.  If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * Copyright 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.cowboycoders.cyclismo.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.cowboycoders.cyclismo.R;
import org.cowboycoders.cyclismo.content.DescriptionGeneratorImpl;
import org.cowboycoders.cyclismo.content.MyTracksProviderUtils;
import org.cowboycoders.cyclismo.content.Track;
import org.cowboycoders.cyclismo.io.file.TrackWriterFactory.TrackFileFormat;

import java.io.File;

/**
 * Utilities for creating intents.
 * 
 * @author Jimmy Shih
 */
public class IntentUtils {

  public static final String TEXT_PLAIN_TYPE = "text/plain";
  private static final String BLUETOOTH_PACKAGE_NAME = "com.android.bluetooth";
  private static final String TWITTER_PACKAGE_NAME = "com.twitter.android";

  private IntentUtils() {}

  /**
   * Creates an intent with {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} and
   * {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
   * 
   * @param context the context
   * @param cls the class
   */
  public static Intent newIntent(Context context, Class<?> cls) {
    return new Intent(context, cls).addFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  /**
   * Creates an intent to share a track url with an app.
   * 
   * @param context the context
   * @param trackId the track id
   * @param trackUrl the track url
   * @param packageName the sharing app package name
   * @param className the sharing app class name
   */
  public static Intent newShareUrlIntent(
      Context context, long trackId, String trackUrl, String packageName, String className) {
    Track track = MyTracksProviderUtils.Factory.get(context).getTrack(trackId);
    String trackDescription = track == null ? ""
        : new DescriptionGeneratorImpl(context).generateTrackDescription(track, null, null, false);
    boolean urlOnly = TWITTER_PACKAGE_NAME.equals(packageName)
        || BLUETOOTH_PACKAGE_NAME.equals(packageName);
    
    return new Intent(Intent.ACTION_SEND)
        .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_track_subject))
        .putExtra(Intent.EXTRA_TEXT, urlOnly 
            ? trackUrl 
            : context.getString(R.string.share_track_share_url_body, trackUrl, trackDescription))
        .setComponent(new ComponentName(packageName, className))
        .setType(TEXT_PLAIN_TYPE);
  }
  
  /**
   * Creates an intent to share a track file with an app.
   * 
   * @param context the context
   * @param trackId the track id
   * @param filePath the file path
   * @param trackFileFormat the track file format
   */
  public static Intent newShareFileIntent(
      Context context, long trackId, String filePath, TrackFileFormat trackFileFormat) {
    Track track = MyTracksProviderUtils.Factory.get(context).getTrack(trackId);
    String trackDescription = track == null ? ""
        : new DescriptionGeneratorImpl(context).generateTrackDescription(track, null, null, false);

    return new Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_track_subject))
        .putExtra(Intent.EXTRA_TEXT,
            context.getString(R.string.share_track_share_file_body, trackDescription))
        .putExtra(context.getString(R.string.track_id_broadcast_extra), trackId)
        .setType(trackFileFormat.getMimeType());
  }
}
