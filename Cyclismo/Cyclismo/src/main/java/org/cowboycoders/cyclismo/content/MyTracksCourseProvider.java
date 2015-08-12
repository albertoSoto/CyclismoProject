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

package org.cowboycoders.cyclismo.content;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;

import org.cowboycoders.cyclismo.R;
import org.cowboycoders.cyclismo.util.PreferencesUtils;

/**
 * A {@link ContentProvider} that handles access to track points, tracks, and
 * waypoints tables.
 * 
 * @author Leif Hendrik Wilden
 */
public class MyTracksCourseProvider extends ContentProvider {

  private static final String TAG = MyTracksCourseProvider.class.getSimpleName();
  @VisibleForTesting
  static final String DATABASE_NAME = "mytracks_turbo_courses.db";
  private static final int DATABASE_VERSION = 20;

  /**
   * Database helper for creating and upgrading the database.
   */
  @VisibleForTesting
  static class DatabaseHelper extends SQLiteOpenHelper {
  
    public DatabaseHelper(Context context) {
      this(context, DATABASE_NAME);
    }
    
    @VisibleForTesting
    public DatabaseHelper(Context context, String databaseName) {
      super(context, databaseName, null, DATABASE_VERSION);
    }
  
    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(CourseTrackPointsColumns.CREATE_TABLE);
      db.execSQL(CourseTracksColumns.CREATE_TABLE);
      db.execSQL(CourseWaypointsColumns.CREATE_TABLE);
    }
  
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
      if (oldVersion < 17) {
        Log.w(TAG, "Deleting all old data.");
        db.execSQL("DROP TABLE IF EXISTS " + CourseTrackPointsColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CourseTracksColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CourseWaypointsColumns.TABLE_NAME);
        onCreate(db);
      } else {
        // Incremental upgrades. One if statement per DB version.
  
        // Add track points SENSOR column
        if (oldVersion <= 17) {
          Log.w(TAG, "Upgrade DB: Adding sensor column.");
          db.execSQL("ALTER TABLE " + CourseTrackPointsColumns.TABLE_NAME + " ADD "
              + CourseTrackPointsColumns.SENSOR + " BLOB");
        }
        // Add tracks TABLEID column
        if (oldVersion <= 18) {
          Log.w(TAG, "Upgrade DB: Adding tableid column.");
          db.execSQL("ALTER TABLE " + CourseTracksColumns.TABLE_NAME + " ADD " + CourseTracksColumns.TABLEID
              + " STRING");
        }
        // Add tracks ICON column
        if (oldVersion <= 19) {
          Log.w(TAG, "Upgrade DB: Adding icon column.");
          db.execSQL(
              "ALTER TABLE " + CourseTracksColumns.TABLE_NAME + " ADD " + CourseTracksColumns.ICON + " STRING");
        }
      }
    }
  }

  /**
   * Types of url.
   * 
   * @author Jimmy Shih
   */
  @VisibleForTesting
  enum UrlType {
    TRACKPOINTS, TRACKPOINTS_ID, TRACKS, TRACKS_ID, WAYPOINTS, WAYPOINTS_ID
  }

  private final UriMatcher uriMatcher;
  private SQLiteDatabase db;

  public MyTracksCourseProvider() {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(MyTracksCourseProviderUtils.AUTHORITY, MyTracksCourseProviderUtils.TABLE_PREFIX + CourseTrackPointsColumns.TABLE_NAME,
        UrlType.TRACKPOINTS.ordinal());
    uriMatcher.addURI(MyTracksCourseProviderUtils.AUTHORITY, MyTracksCourseProviderUtils.TABLE_PREFIX + CourseTrackPointsColumns.TABLE_NAME + "/#",
        UrlType.TRACKPOINTS_ID.ordinal());
    uriMatcher.addURI(
        MyTracksCourseProviderUtils.AUTHORITY, MyTracksCourseProviderUtils.TABLE_PREFIX + CourseTracksColumns.TABLE_NAME, UrlType.TRACKS.ordinal());
    uriMatcher.addURI(MyTracksCourseProviderUtils.AUTHORITY, CourseTracksColumns.TABLE_NAME + "/#",
        UrlType.TRACKS_ID.ordinal());
    uriMatcher.addURI(
        MyTracksCourseProviderUtils.AUTHORITY, MyTracksCourseProviderUtils.TABLE_PREFIX + CourseWaypointsColumns.TABLE_NAME, UrlType.WAYPOINTS.ordinal());
    uriMatcher.addURI(MyTracksCourseProviderUtils.AUTHORITY, CourseWaypointsColumns.TABLE_NAME + "/#",
        UrlType.WAYPOINTS_ID.ordinal());
  }

  @Override
  public boolean onCreate() {
    return onCreate(getContext());
  }
  
  /**
   * Helper method to make onCreate is testable.
   * @param context context to creates database
   * @return true means run successfully
   */
  @VisibleForTesting
  boolean onCreate(Context context) {
    if (!canAccess()) {
      return false;
    }
    DatabaseHelper databaseHelper = new DatabaseHelper(context);
    try {
      db = databaseHelper.getWritableDatabase();
    } catch (SQLiteException e) {
      Log.e(TAG, "Unable to open database for writing.", e);
    }
    return db != null;
  }

  @Override
  public int delete(Uri url, String where, String[] selectionArgs) {
    if (!canAccess()) {
      return 0;
    }
    String table;
    boolean shouldVacuum = false;
    switch (getUrlType(url)) {
      case TRACKPOINTS:
        table = CourseTrackPointsColumns.TABLE_NAME;
        break;
      case TRACKS:
        table = CourseTracksColumns.TABLE_NAME;
        shouldVacuum = true;
        break;
      case WAYPOINTS:
        table = CourseWaypointsColumns.TABLE_NAME;
        break;
      default:
        throw new IllegalArgumentException("Unknown URL " + url);
    }
  
    Log.w(MyTracksCourseProvider.TAG, "Deleting table " + table);
    int count;
    try {
      db.beginTransaction();
      count = db.delete(table, where, selectionArgs);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    getContext().getContentResolver().notifyChange(url, null, true);
  
    if (shouldVacuum) {
      // If a potentially large amount of data was deleted, reclaim its space.
      Log.i(TAG, "Vacuuming the database.");
      db.execSQL("VACUUM");
    }
    return count;
  }

  @Override
  public String getType(Uri url) {
    if (!canAccess()) {
      return null;
    }
    Log.d(TAG,url.getPath());
    
    switch (getUrlType(url)) {
      case TRACKPOINTS:
        return CourseTrackPointsColumns.CONTENT_TYPE;
      case TRACKPOINTS_ID:
        return CourseTrackPointsColumns.CONTENT_ITEMTYPE;
      case TRACKS:
        return CourseTracksColumns.CONTENT_TYPE;
      case TRACKS_ID:
        return CourseTracksColumns.CONTENT_ITEMTYPE;
      case WAYPOINTS:
        return CourseWaypointsColumns.CONTENT_TYPE;
      case WAYPOINTS_ID:
        return CourseWaypointsColumns.CONTENT_ITEMTYPE;
      default:
        throw new IllegalArgumentException("Unknown URL " + url);
    }
  }

  @Override
  public Uri insert(Uri url, ContentValues initialValues) {
    if (!canAccess()) {
      return null;
    }
    if (initialValues == null) {
      initialValues = new ContentValues();
    }
    Uri result = null;
    try {
      db.beginTransaction();
      result = insertContentValues(url, getUrlType(url), initialValues);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return result;
  }

  @Override
  public int bulkInsert(Uri url, ContentValues[] valuesBulk) {
    if (!canAccess()) {
      return 0;
    }
    int numInserted = 0;
    try {
      // Use a transaction in order to make the insertions run as a single batch
      db.beginTransaction();
  
      UrlType urlType = getUrlType(url);
      for (numInserted = 0; numInserted < valuesBulk.length; numInserted++) {
        ContentValues contentValues = valuesBulk[numInserted];
        if (contentValues == null) {
          contentValues = new ContentValues();
        }
        insertContentValues(url, urlType, contentValues);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return numInserted;
  }

  @Override
  public Cursor query(
      Uri url, String[] projection, String selection, String[] selectionArgs, String sort) {
    if (!canAccess()) {
      return null;
    }
    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    String sortOrder = null;
    switch (getUrlType(url)) {
      case TRACKPOINTS:
        queryBuilder.setTables(CourseTrackPointsColumns.TABLE_NAME);
        sortOrder = sort != null ? sort : CourseTrackPointsColumns.DEFAULT_SORT_ORDER;
        break;
      case TRACKPOINTS_ID:
        queryBuilder.setTables(CourseTrackPointsColumns.TABLE_NAME);
        queryBuilder.appendWhere("_id=" + url.getPathSegments().get(1));
        break;
      case TRACKS:
        queryBuilder.setTables(CourseTracksColumns.TABLE_NAME);
        sortOrder = sort != null ? sort : CourseTracksColumns.DEFAULT_SORT_ORDER;
        break;
      case TRACKS_ID:
        queryBuilder.setTables(CourseTracksColumns.TABLE_NAME);
        queryBuilder.appendWhere("_id=" + url.getPathSegments().get(1));
        break;
      case WAYPOINTS:
        queryBuilder.setTables(CourseWaypointsColumns.TABLE_NAME);
        sortOrder = sort != null ? sort : CourseWaypointsColumns.DEFAULT_SORT_ORDER;
        break;
      case WAYPOINTS_ID:
        queryBuilder.setTables(CourseWaypointsColumns.TABLE_NAME);
        queryBuilder.appendWhere("_id=" + url.getPathSegments().get(1));
        break;
      default:
        throw new IllegalArgumentException("Unknown url " + url);
    }
    Cursor cursor = queryBuilder.query(
        db, projection, selection, selectionArgs, null, null, sortOrder);
    cursor.setNotificationUri(getContext().getContentResolver(), url);
    return cursor;
  }

  @Override
  public int update(Uri url, ContentValues values, String where, String[] selectionArgs) {
    if (!canAccess()) {
      return 0;
    }
    String table;
    String whereClause;
    switch (getUrlType(url)) {
      case TRACKPOINTS:
        table = CourseTrackPointsColumns.TABLE_NAME;
        whereClause = where;
        break;
      case TRACKPOINTS_ID:
        table = CourseTrackPointsColumns.TABLE_NAME;
        whereClause = CourseTrackPointsColumns._ID + "=" + url.getPathSegments().get(1);
        if (!TextUtils.isEmpty(where)) {
          whereClause += " AND (" + where + ")";
        }
        break;
      case TRACKS:
        table = CourseTracksColumns.TABLE_NAME;
        whereClause = where;
        break;
      case TRACKS_ID:
        table = CourseTracksColumns.TABLE_NAME;
        whereClause = CourseTracksColumns._ID + "=" + url.getPathSegments().get(1);
        if (!TextUtils.isEmpty(where)) {
          whereClause += " AND (" + where + ")";
        }
        break;
      case WAYPOINTS:
        table = CourseWaypointsColumns.TABLE_NAME;
        whereClause = where;
        break;
      case WAYPOINTS_ID:
        table = CourseWaypointsColumns.TABLE_NAME;
        whereClause = CourseWaypointsColumns._ID + "=" + url.getPathSegments().get(1);
        if (!TextUtils.isEmpty(where)) {
          whereClause += " AND (" + where + ")";
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown url " + url);
    }
    int count;
    try {
      db.beginTransaction();
      count = db.update(table, values, whereClause, selectionArgs);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    getContext().getContentResolver().notifyChange(url, null, true);
    return count;
  }

  /**
   * Returns true if the caller can access the content provider.
   */
  private boolean canAccess() {
    if (Binder.getCallingPid() == Process.myPid()) {
      return true;
    } else {
      return PreferencesUtils.getBoolean(
          getContext(), R.string.allow_access_key, PreferencesUtils.ALLOW_ACCESS_DEFAULT);
    }
  }

  /**
   * Gets the {@link UrlType} for a url.
   * 
   * @param url the url
   */
  private UrlType getUrlType(Uri url) {
    return UrlType.values()[uriMatcher.match(url)];
  }

  /**
   * Inserts a content based on the url type.
   * 
   * @param url the content url
   * @param urlType the url type
   * @param contentValues the content values
   */
  private Uri insertContentValues(Uri url, UrlType urlType, ContentValues contentValues) {
    switch (urlType) {
      case TRACKPOINTS:
        return insertTrackPoint(url, contentValues);
      case TRACKS:
        return insertTrack(url, contentValues);
      case WAYPOINTS:
        return insertWaypoint(url, contentValues);
      default:
        throw new IllegalArgumentException("Unknown url " + url);
    }
  }

  /**
   * Inserts a track point.
   * 
   * @param url the content url
   * @param values the content values
   */
  private Uri insertTrackPoint(Uri url, ContentValues values) {
    boolean hasLatitude = values.containsKey(CourseTrackPointsColumns.LATITUDE);
    boolean hasLongitude = values.containsKey(CourseTrackPointsColumns.LONGITUDE);
    boolean hasTime = values.containsKey(CourseTrackPointsColumns.TIME);
    if (!hasLatitude || !hasLongitude || !hasTime) {
      throw new IllegalArgumentException("Latitude, longitude, and time values are required.");
    }
    long rowId = db.insert(CourseTrackPointsColumns.TABLE_NAME, CourseTrackPointsColumns._ID, values);
    if (rowId >= 0) {
      Uri uri = ContentUris.appendId(CourseTrackPointsColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLiteException("Failed to insert a track point " + url);
  }

  /**
   * Inserts a track.
   * 
   * @param url the content url
   * @param contentValues the content values
   */
  private Uri insertTrack(Uri url, ContentValues contentValues) {
    boolean hasStartTime = contentValues.containsKey(CourseTracksColumns.STARTTIME);
    boolean hasStartId = contentValues.containsKey(CourseTracksColumns.STARTID);
    if (!hasStartTime || !hasStartId) {
      throw new IllegalArgumentException("Both start time and start id values are required.");
    }
    long rowId = db.insert(CourseTracksColumns.TABLE_NAME, CourseTracksColumns._ID, contentValues);
    if (rowId >= 0) {
      Uri uri = ContentUris.appendId(CourseTracksColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLException("Failed to insert a track " + url);
  }

  /**
   * Inserts a waypoint.
   * 
   * @param url the content url
   * @param contentValues the content values
   */
  private Uri insertWaypoint(Uri url, ContentValues contentValues) {
    long rowId = db.insert(CourseWaypointsColumns.TABLE_NAME, CourseWaypointsColumns._ID, contentValues);
    if (rowId >= 0) {
      Uri uri = ContentUris.appendId(CourseWaypointsColumns.CONTENT_URI.buildUpon(), rowId).build();
      getContext().getContentResolver().notifyChange(url, null, true);
      return uri;
    }
    throw new SQLException("Failed to insert a waypoint " + url);
  }
}