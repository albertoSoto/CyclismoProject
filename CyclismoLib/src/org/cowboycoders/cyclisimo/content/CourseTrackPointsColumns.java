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
 * Copyright 2008 Google Inc.
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

package org.cowboycoders.cyclisimo.content;

import static org.cowboycoders.cyclisimo.content.ContentTypeIds.BLOB_TYPE_ID;
import static org.cowboycoders.cyclisimo.content.ContentTypeIds.FLOAT_TYPE_ID;
import static org.cowboycoders.cyclisimo.content.ContentTypeIds.INT_TYPE_ID;
import static org.cowboycoders.cyclisimo.content.ContentTypeIds.LONG_TYPE_ID;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Constants for the track points table.
 *
 * @author Leif Hendrik Wilden
 */
public interface CourseTrackPointsColumns extends BaseColumns {

  public static final String TABLE_NAME = "trackpoints";
  public static final Uri CONTENT_URI = Uri.parse(
      "content://org.cowboycoders.cyclisimo.courses/course_trackpoints");
  public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.trackpoint";
  public static final String CONTENT_ITEMTYPE = "vnd.android.cursor.item/vnd.google.trackpoint";
  public static final String DEFAULT_SORT_ORDER = "_id";

  // Columns
  public static final String TRACKID = "trackid"; // track id
  public static final String LONGITUDE = "longitude"; // longitude
  public static final String LATITUDE = "latitude"; // latitude
  public static final String TIME = "time"; // time
  public static final String ALTITUDE = "elevation"; // altitude
  public static final String ACCURACY = "accuracy"; // accuracy
  public static final String SPEED = "speed"; // speed
  public static final String BEARING = "bearing"; // bearing
  public static final String SENSOR = "sensor"; // sensor

  public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
      + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + TRACKID + " INTEGER, "
      + LONGITUDE + " INTEGER, "
      + LATITUDE + " INTEGER, "
      + TIME + " INTEGER, "
      + ALTITUDE + " FLOAT, "
      + ACCURACY + " FLOAT, "
      + SPEED + " FLOAT, "
      + BEARING + " FLOAT, "
      + SENSOR + " BLOB" 
      + ");";

  public static final String[] COLUMNS = {
      _ID,
      TRACKID,
      LONGITUDE,
      LATITUDE,
      TIME,
      ALTITUDE,
      ACCURACY,
      SPEED,
      BEARING,
      SENSOR
   };

   public static final byte[] COLUMN_TYPES = {
       LONG_TYPE_ID, // id
       LONG_TYPE_ID, // track id
       INT_TYPE_ID, // longitude
       INT_TYPE_ID, // latitude
       LONG_TYPE_ID, // time
       FLOAT_TYPE_ID, // altitude
       FLOAT_TYPE_ID, // accuracy
       FLOAT_TYPE_ID, // speed
       FLOAT_TYPE_ID, // bearing
       BLOB_TYPE_ID // sensor
   };
}
