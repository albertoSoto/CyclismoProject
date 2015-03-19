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

package org.cowboycoders.cyclisimo.fragments;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


//FIXME: maphack
//import com.google.android.gms.maps.CameraUpdate;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
//import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
//import com.google.android.gms.maps.LocationSource;
//import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.CameraPosition;
//import com.google.android.gms.maps.model.LatLong;
//import com.google.android.gms.maps.model.LatLongBounds;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.Polyline;


import org.cowboycoders.cyclisimo.maps.AugmentedPolyline;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.model.MapViewPosition;


import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layers;
//import android.view.Gravity;
//import android.widget.TextView;

import org.cowboycoders.cyclisimo.DummyOverlay;
import org.cowboycoders.cyclisimo.MapOverlay;
import org.cowboycoders.cyclisimo.MarkerDetailActivity;

import org.cowboycoders.cyclisimo.R;
import org.cowboycoders.cyclisimo.StaticOverlay;
import org.cowboycoders.cyclisimo.TrackDetailActivity;
import org.cowboycoders.cyclisimo.content.MyTracksCourseProviderUtils;
import org.cowboycoders.cyclisimo.content.MyTracksProviderUtils;
import org.cowboycoders.cyclisimo.content.MyTracksProviderUtils.Factory;
import org.cowboycoders.cyclisimo.content.Track;
import org.cowboycoders.cyclisimo.content.TrackDataHub;
import org.cowboycoders.cyclisimo.content.TrackDataListener;
import org.cowboycoders.cyclisimo.content.TrackDataType;
import org.cowboycoders.cyclisimo.content.Waypoint;
import org.cowboycoders.cyclisimo.stats.TripStatistics;
import org.cowboycoders.cyclisimo.util.ApiAdapterFactory;
import org.cowboycoders.cyclisimo.util.GoogleLocationUtils;
import org.cowboycoders.cyclisimo.util.IntentUtils;
import org.cowboycoders.cyclisimo.util.LocationUtils;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.util.MapPositionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.mapsforge.core.util.LatLongUtils.zoomForBounds;

/**
 * A fragment to display map to the user.
 * 
 * FIXME: added lots or redrawCourseOverlay()'s : remove some (if poss)
 * 
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MyTracksMapFragment extends Fragment implements TrackDataListener {
  
  private static final String TAG = "MyTracksMapFragment";

  public static final String MAP_FRAGMENT_TAG = "mapFragment";

  private static final String CURRENT_LOCATION_KEY = "current_location_key";
  private static final String
      KEEP_CURRENT_LOCATION_VISIBLE_KEY = "keep_current_location_visible_key";
  private static final String ZOOM_TO_CURRENT_LOCATION_KEY = "zoom_to_current_location_key";
  private static final String MAP_TYPE = "map_type";
  
  private static final float DEFAULT_ZOOM_LEVEL = 18f;

  // Google's latitude and longitude
  private static final double DEFAULT_LATITUDE = 37.423;
  private static final double DEFAULT_LONGITUDE = -122.084;

  private static final int MAP_VIEW_PADDING = 32;

  private static final long COURSE_OVERLAY_REDRAW_REFRESH_PERIOD_MS = 1000;

  private static final long COURSE_LOAD_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(10);

  private TrackDataHub trackDataHub;
  
  private TrackDataHub courseDataHub;

  // Current location
  private Location currentLocation;

  // True to keep the currentLocation visible
  private boolean keepCurrentLocationVisible;

  // True to zoom to currentLocation when it is available
  private boolean zoomToCurrentLocation;

  //private OnLocationChangedListener onLocationChangedListener;

  // For showing a marker
  private long markerTrackId = -1L;
  private long markerId = -1L;

  // Current track
  private Track currentTrack;
  private Track currentCourse;
  
  private boolean courseMode = false;

  // Current paths
  private ArrayList<AugmentedPolyline> paths = new ArrayList<AugmentedPolyline>();
  boolean reloadPaths = true;

  // UI elements
  private MapView googleMap;
  private MapViewPosition mapViewPos;
  private MapOverlay mapOverlay;
  private DummyOverlay courseDummyOverlay;
  private StaticOverlay courseOverlay;
  private View mapView;
  private ImageButton myLocationImageButton;
  //private TextView messageTextView;

  private boolean mUseCourseProvider;
  private long courseTrackId;
  
  private Lock courseLoadLock = new ReentrantLock();
  private Condition courseLoadedChanged = courseLoadLock.newCondition(); // courseOverlay != null
  
  
//  private boolean loadCompleted = false;

  
  private TrackDataListener courseTrackDataListener = new TrackDataListener() {
    
    @SuppressWarnings("hiding")
    //private boolean reloadPaths = true;
    
    @Override
    public void onLocationStateChanged(final LocationState state) {
      //ignore
      return;
    }

    @Override
    public void onLocationChanged(final Location location) {
      //ignore
    }

    @Override
    public void onHeadingChanged(double heading) {
      // We don't care.
    }

    @Override
    public void onSelectedTrackChanged(final Track track) {
      if (isResumed()) {
        if (courseMode) {
          Log.d(TAG,"in courseTrackDataListener : onSelectedTrackChanged");
          currentCourse = track;
//        boolean hasTrack = track != null;
//        if (hasTrack) {
          //courseOverlay.setShowEndMarker(true);
          if(courseOverlay == null) {
            showTrack(track);
          }
        }
          //redrawCourseOverlay();
          //currentTrack = track;
          //showTrack();
//          synchronized (this) {
//            /*
//             * Synchronize to prevent race condition in changing markerTrackId and
//             * markerId variables.
//             */
//            if (track.getId() == markerTrackId) {
//              // Show the marker
//              showMarker(markerId);
//
//              markerTrackId = -1L;
//              markerId = -1L;
//            } else {
//              // Show the track
//              showTrack();
//            }
//          }
//        }
      }
    }

    @Override
    public void onTrackUpdated(Track track) {
      // We don't care.
    }

    @Override
    public void clearTrackPoints() {
      if (isResumed()) {
        //courseDummyOverlay.clearPoints();
        //reloadPaths = true;
      }
    }

    @Override
    public void onSampledInTrackPoint(final Location location) {
      if (isResumed()) {
        courseDummyOverlay.addLocation(location);
      }
    }

    @Override
    public void onSampledOutTrackPoint(Location location) {
      // We don't care.
    }

    @Override
    public void onSegmentSplit(Location location) {
      if (isResumed()) {
        courseDummyOverlay.addSegmentSplit();
      }
    }

    @Override
    public void onNewTrackPointsDone() {
      // we have our data
      courseDataHub.unregisterTrackDataListener(this);
      if (isResumed()) {
        getActivity().runOnUiThread(new Runnable() {

          public void run() {
            if (isResumed() && googleMap != null) {
              courseDummyOverlay.update(null, null, true);
            
              if (courseOverlay == null && courseMode) {
                courseOverlay = new StaticOverlay(MyTracksMapFragment.this.getActivity(),
                    courseDummyOverlay.getLocations());
                Log.d(TAG,"new courseOverlay");
                try {
                  courseLoadLock.lock();
                  courseLoadedChanged.signalAll();
                } finally {
                  courseLoadLock.unlock();
                }
              }
              
              //reloadPaths = false;
            }
          }
        });
      }
    }

    @Override
    public void clearWaypoints() {
      if (isResumed()) {
        //courseDummyOverlay.clearWaypoints();
      }
    }

    @Override
    public void onNewWaypoint(Waypoint waypoint) {
      if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
        courseDummyOverlay.addWaypoint(waypoint);
      }
    }

    @Override
    public void onNewWaypointsDone() {
      if (isResumed()) {
        getActivity().runOnUiThread(new Runnable() {
          public void run() {
            if (isResumed() && googleMap != null) {
              courseDummyOverlay.update(null, null, true);
            }
          }
        });
      }
    }

    @Override
    public boolean onMetricUnitsChanged(boolean metric) {
      // We don't care.
      return false;
    }

    @Override
    public boolean onReportSpeedChanged(boolean reportSpeed) {
      // We don't care.
      return false;
    }

    @Override
    public boolean onMinRecordingDistanceChanged(int minRecordingDistance) {
      // We don't care.
      return false;
    }
  };

  private long redrawCourseOverlayTimestamp;
    private TileCache tileCache;

    @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    AndroidGraphicFactory.createInstance(this.getActivity().getApplication());
    //courseTrackId = bundle.getLong(TrackDetailActivity.EXTRA_COURSE_TRACK_ID);
    setHasOptionsMenu(true);
    ApiAdapterFactory.getApiAdapter().invalidMenu(getActivity());
    courseTrackId = -1l;
    if (getActivity() instanceof TrackDetailActivity) {
      mUseCourseProvider = ((TrackDetailActivity) getActivity()).isUsingCourseProivder();
      courseTrackId = ((TrackDetailActivity) getActivity()).getCourseTrackId();
      
    }
    courseMode = ((TrackDetailActivity) getActivity()).isCourseMode();
    mapOverlay = new MapOverlay(getActivity());
    courseDummyOverlay = new DummyOverlay(getActivity());
    
    Log.d(TAG,"courseMode : " + courseMode);
    Log.d(TAG,"courseTrackId in bundle : " + courseTrackId);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mapView = super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.map, container, false);
    googleMap = (MapView) layout.findViewById(R.id.mapView);

    myLocationImageButton = (ImageButton) layout.findViewById(R.id.map_my_location);
    myLocationImageButton.setOnClickListener(new View.OnClickListener() {
        @Override
      public void onClick(View v) {
        forceUpdateLocation();
        keepCurrentLocationVisible = true;
        zoomToCurrentLocation = true;
        updateCurrentLocation();
      }
    });
    //messageTextView = (TextView) layout.findViewById(R.id.map_message);


//      googleMap.setOnMarkerClickListener(new OnMarkerClickListener() {
//
//          @Override
//        public boolean onMarkerClick(Marker marker) {
//          if (isResumed()) {
//            String title = marker.getTitle();
//            if (title != null && title.length() > 0) {
//              long id = Long.valueOf(title);
//              Context context = getActivity();
//              Intent intent = IntentUtils.newIntent(context, MarkerDetailActivity.class)
//                  .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, id);
//              context.startActivity(intent);
//            }
//          }
//          return true;
//        }
//      });
//      googleMap.setLocationSource(new LocationSource() {
//
//          @Override
//        public void activate(OnLocationChangedListener listener) {
//          onLocationChangedListener = listener;
//        }
//
//          @Override
//        public void deactivate() {
//          onLocationChangedListener = null;
//        }
//      });
      Model model = googleMap.getModel();
      mapViewPos = googleMap.getModel().mapViewPosition;
      //TODO: move this to button
      googleMap.setClickable(true);
      mapViewPos.addObserver(new Observer() {

          @Override
          public void onChange() {
              if (isResumed() && keepCurrentLocationVisible && currentLocation != null
                      && !isLocationVisible(currentLocation)) {
                  keepCurrentLocationVisible = false;
                  zoomToCurrentLocation = false;
              }
          }
      });
      LayerManager layerManager = this.googleMap.getLayerManager();
      Layers layers = layerManager.getLayers();

      mapViewPos.setZoomLevel((byte) 16);

      this.tileCache = AndroidUtil.createTileCache(this.getActivity(),
              "fragments",
              this.googleMap.getModel().displayModel.getTileSize(), 1.0f,
              1.5);

      layers.add(AndroidUtil.createTileRendererLayer(this.tileCache,
              mapViewPos, getMapFile(),
              InternalRenderTheme.OSMARENDER, false, true));


      mapViewPos.animateTo(getDefaultLatLong());

    return layout;
  }



  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      keepCurrentLocationVisible = savedInstanceState.getBoolean(
          KEEP_CURRENT_LOCATION_VISIBLE_KEY, false);
      zoomToCurrentLocation = savedInstanceState.getBoolean(ZOOM_TO_CURRENT_LOCATION_KEY, false);
      currentLocation = (Location) savedInstanceState.getParcelable(CURRENT_LOCATION_KEY);
      updateCurrentLocation();
      if (googleMap != null) {
        // set map type e.g map vs satellite
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeTrackDataHub();
    resumeCourseDataHub();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (currentLocation != null) {
      outState.putParcelable(CURRENT_LOCATION_KEY, currentLocation);
    }
    outState.putBoolean(KEEP_CURRENT_LOCATION_VISIBLE_KEY, keepCurrentLocationVisible);
    outState.putBoolean(ZOOM_TO_CURRENT_LOCATION_KEY, zoomToCurrentLocation);
    if (googleMap != null) {
      // set map type e.g map vs satellite
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseTrackDataHub();
    pauseCourseDataHub();
  }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mapView != null) {
            this.googleMap.destroy();
        }
        if (this.tileCache != null) {
            this.tileCache.destroy();
        }
        AndroidGraphicFactory.clearResourceMemoryCache();
    }

    protected MapFile getMapFile() {
        return new MapFile(new File(Environment.getExternalStorageDirectory(),
                this.getMapFileName()));
    }

    protected String getMapFileName() {
        return "germany.map";
    }


  /**
   * Shows the marker on the map.
   * 
   * @param trackId the track id
   * @param id the marker id
   */
  public void showMarker(long trackId, long id) {
    /*
     * Synchronize to prevent race condition in changing markerTrackId and
     * markerId variables.
     */
    synchronized (this) {
      if (currentTrack != null && currentTrack.getId() == trackId) {
        showMarker(id);
        markerTrackId = -1L;
        markerId = -1L;
        return;
      }
      markerTrackId = trackId;
      markerId = id;
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflator) {
    menuInflator.inflate(R.menu.map, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (googleMap != null) {
        // map types
    }
    super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem menuItem) {

    return true;
  }

  @Override
  public void onLocationStateChanged(final LocationState state) {
    if (isResumed()) {
      Log.v(TAG,"onLocationStateChanged");
      getActivity().runOnUiThread(new Runnable() {
          @Override
        public void run() {
          if (!isResumed() || googleMap == null) {
            return;
          }
          boolean myLocationEnabled = true;
          if (state == LocationState.DISABLED) {
            currentLocation = null;
            myLocationEnabled = false;
          }

          String message;
          boolean isGpsDisabled;
          if (!isSelectedTrackRecording()) {
            message = null;
            isGpsDisabled = false;
          } else {
            switch (state) {
              case DISABLED:
                String setting = getString(GoogleLocationUtils.isAvailable(getActivity())
                    ? R.string.gps_google_location_settings
                    : R.string.gps_location_access);
                message = getString(R.string.gps_disabled, setting);
                isGpsDisabled = true;
                break;
              case NO_FIX:
                message = getString(R.string.gps_wait_for_signal);
                isGpsDisabled = false;
                break;
              case BAD_FIX:
                message = getString(R.string.gps_wait_for_better_signal);
                isGpsDisabled = false;
                break;
              case GOOD_FIX:
                message = null;
                isGpsDisabled = false;
                break;
              default:
                throw new IllegalArgumentException("Unexpected state: " + state);
            }
          }
//          if (message == null) {
//            messageTextView.setVisibility(View.GONE);
//            return;
//          }
//          messageTextView.setText(message);
//          messageTextView.setVisibility(View.VISIBLE);
//          if (isGpsDisabled) {
//            Toast.makeText(getActivity(), R.string.gps_not_found, Toast.LENGTH_LONG).show();
//
//            // Click to show the location source settings
//            messageTextView.setOnClickListener(new OnClickListener() {
//
//                @Override
//              public void onClick(View v) {
//                Intent intent = GoogleLocationUtils.isAvailable(getActivity()) ? new Intent(
//                    GoogleLocationUtils.ACTION_GOOGLE_LOCATION_SETTINGS)
//                    : new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//              }
//            });
//          } else {
//            messageTextView.setOnClickListener(null);
//          }
        }
      });
    }
  }

  @Override
  public synchronized void onLocationChanged(final Location location) {
    if (isResumed()) {
      Log.v(TAG, "location changed");
      if (isSelectedTrackRecording() && currentLocation == null && location != null) {
        zoomToCurrentLocation = true;

      }
      currentLocation = location;
      updateCurrentLocation();
    }
  }

  @Override
  public void onHeadingChanged(double heading) {
    // We don't care.
  }

  @Override
  public synchronized void onSelectedTrackChanged(final Track track) {
    if (isResumed()) {
      currentTrack = track;
      boolean hasTrack = track != null;
      if (hasTrack) {
        mapOverlay.setShowEndMarker(!isSelectedTrackRecording());
        synchronized (this) {
          /*
           * Synchronize to prevent race condition in changing markerTrackId and
           * markerId variables.
           */
          if (track.getId() == markerTrackId) {
            // Show the marker
            showMarker(markerId);

            markerTrackId = -1L;
            markerId = -1L;
          } else {
            // Show the track
            showTrack();
          }
        }
      }
    }
  }
  
  private void reloadCourse() {
    if (currentCourse != null && currentCourse.getId() != -1L) {
      courseDataHub.loadTrack(currentCourse.getId());
    }
  }
  
  private void reloadTrack() {
    if (currentTrack != null && !this.isSelectedTrackRecording()) {
      trackDataHub.loadTrack(currentTrack.getId());
    }
  }

  @Override
  public void onTrackUpdated(Track track) {
    // We don't care.
  }

  @Override
  public synchronized void clearTrackPoints() {
    //FIXED: (left around in case)
    // otherwise temperamental showing of previously recored track 
    // (will sometimes show map and sometimes will clear).
    // Correlated with number of trackDetailInstances open?
    // && this.isSelectedTrackRecording()
    if (isResumed()) {
      mapOverlay.clearPoints();
      reloadPaths = true;
      //redrawCourseOverlay();
    }
  }

  @Override
  public synchronized void onSampledInTrackPoint(final Location location) {
    if (isResumed()) {
      Log.v(TAG,"sampled in track point");
//      if (!this.isSelectedTrackRecording() && loadCompleted) {
//        return;
//      }
      mapOverlay.addLocation(location);
      
      //redrawCourseOverlay();
    }
  }

  @Override
  public synchronized void onSampledOutTrackPoint(Location location) {
    // We don't care.
  }

  @Override
  public synchronized void onSegmentSplit(Location location) {
    if (isResumed()) {
//      if (!this.isSelectedTrackRecording() && loadCompleted) {
//        return;
//      }
      mapOverlay.addSegmentSplit();
    }
  }
  
  @Override
  public synchronized void onNewTrackPointsDone() {
    if (isResumed()) {
      Log.v(TAG,"track points done");
      if (courseMode && courseOverlay == null) {
        try {
          courseLoadLock.lock();
          final long startTime = System.nanoTime();
          while( courseOverlay == null) {
            long timeLeft = COURSE_LOAD_TIMEOUT_NS - (System.nanoTime() - startTime);
            if(!courseLoadedChanged.await(timeLeft, TimeUnit.NANOSECONDS)) {
              break;
            }
          }
        } catch (InterruptedException e) {
          Log.e(TAG,"interrupted waiting for course overlay");
        } finally {
          courseLoadLock.unlock();
        }
      }
      
      getActivity().runOnUiThread(new Runnable() {
        
        public void run() {
          if (isResumed() && googleMap != null) {

            mapOverlay.addUnderlay(courseOverlay);
            mapOverlay.update(googleMap, paths, reloadPaths);
            //add the overlays
            reloadPaths = false;
            
          }
        }
      });
    }
  }

  @Override
  public synchronized void clearWaypoints() {
    if (isResumed()) {
      //clear layer
      mapOverlay.clearWaypoints();
    }

  }

  @Override
  public synchronized void onNewWaypoint(Waypoint waypoint) {
    if (isResumed() && waypoint != null && LocationUtils.isValidLocation(waypoint.getLocation())) {
      mapOverlay.addWaypoint(waypoint);
    }
  }

  @Override
  public synchronized void onNewWaypointsDone() {
    if (isResumed()) {
      getActivity().runOnUiThread(new Runnable() {
        public void run() {
          if (isResumed() && googleMap != null) {
            mapOverlay.update(googleMap, paths, true);
          }
        }
      });
    }
  }

  @Override
  public boolean onMetricUnitsChanged(boolean metric) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onReportSpeedChanged(boolean reportSpeed) {
    // We don't care.
    return false;
  }

  @Override
  public boolean onMinRecordingDistanceChanged(int minRecordingDistance) {
    // We don't care.
    return false;
  }
  
  private synchronized void redrawCourseOverlay() {
    if (courseOverlay != null && googleMap != null && courseMode) {

      Log.d(TAG,"redrawing courseOverlay");

      getActivity().runOnUiThread(new Runnable() {
        @Override
      public void run() {
      try {
      courseOverlay.update(googleMap);
      } catch (IllegalStateException e) {
        Log.d(TAG,"Illegal state exception whilst updating map polyline");
      }
    }
      });
    }
  }

  /**
   * Resumes the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void resumeTrackDataHub() {
    trackDataHub = ((TrackDetailActivity) getActivity()).getTrackDataHub();
    trackDataHub.registerTrackDataListener(this, EnumSet.of(TrackDataType.SELECTED_TRACK,
        TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
        TrackDataType.LOCATION));
  }

  /**
   * Pauses the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void pauseCourseDataHub() {
    //FIXME: needs new listener
    if (courseDataHub != null) {
      courseDataHub.unregisterTrackDataListener(courseTrackDataListener);
    }
    courseDataHub = null;
  }
  
  /**
   * Resumes the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void resumeCourseDataHub() {
    courseDataHub = ((TrackDetailActivity) getActivity()).getCourseDataHub();
    courseDataHub.registerTrackDataListener(courseTrackDataListener, EnumSet.of(TrackDataType.SELECTED_TRACK,
        TrackDataType.WAYPOINTS_TABLE, TrackDataType.SAMPLED_IN_TRACK_POINTS_TABLE,
        TrackDataType.LOCATION));
  }

  /**
   * Pauses the trackDataHub. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void pauseTrackDataHub() {
    if (trackDataHub != null) {
      trackDataHub.unregisterTrackDataListener(this);
    }
    trackDataHub = null;
  }

  /**
   * Returns true if the selected track is recording. Needs to be synchronized
   * because the trackDataHub can be accessed by multiple threads.
   */
  private synchronized boolean isSelectedTrackRecording() {
    return trackDataHub != null && trackDataHub.isSelectedTrackRecording();
  }

  /**
   * Forces update location. Needs to be synchronized because the trackDataHub
   * can be accessed by multiple threads.
   */
  private synchronized void forceUpdateLocation() {
    if (trackDataHub != null) {
      trackDataHub.forceUpdateLocation();
    }
    if (courseDataHub != null) {
      courseDataHub.forceUpdateLocation();
    }
  }

  /**
   * Updates the current location and zoom to it if necessary.
   */
  private void updateCurrentLocation() {
    getActivity().runOnUiThread(new Runnable() {
      public void run() {
        if (!isResumed() || googleMap == null //|| onLocationChangedListener == null
            || currentLocation == null) {
          return;
        }
        //onLocationChangedListener.onLocationChanged(currentLocation);
        if (zoomToCurrentLocation
            || (keepCurrentLocationVisible && !isLocationVisible(currentLocation))) {
          LatLong LatLong = new LatLong(currentLocation.getLatitude(), currentLocation.getLongitude());
          mapViewPos.animateTo(LatLong);
          zoomToCurrentLocation = false;
        }
      };
    });
  }

  /**
   * Sets the camera over a track.
   */
  private void showTrack() {
    showTrack(currentTrack);
  }
  
  private void showTrack(final Track track) {
    getActivity().runOnUiThread(new Runnable() {
        @Override
      public void run() {
          
        @SuppressWarnings("hiding")
        Track currentTrack = track;
        if (!isResumed() || googleMap == null || currentTrack == null
            || currentTrack.getNumberOfPoints() < 2) {
          return;
        }

        /**
         * Check that mapView is valid.
         */
        if (mapView == null || mapView.getWidth() == 0 || mapView.getHeight() == 0) {
          return;
        }

        TripStatistics tripStatistics = currentTrack.getTripStatistics();
        int latitudeSpanE6 = tripStatistics.getTop() - tripStatistics.getBottom();
        int longitudeSpanE6 = tripStatistics.getRight() - tripStatistics.getLeft();
        if (latitudeSpanE6 > 0 && latitudeSpanE6 < 180E6 && longitudeSpanE6 > 0
            && longitudeSpanE6 < 360E6) {
          LatLong southWest = new LatLong(
              tripStatistics.getBottomDegrees(), tripStatistics.getLeftDegrees());
          LatLong northEast = new LatLong(
              tripStatistics.getTopDegrees(), tripStatistics.getRightDegrees());

          BoundingBox bounds = new BoundingBox(southWest.latitude, southWest.longitude, northEast.latitude, northEast.longitude);
          

          mapViewPos.setMapLimit(bounds);
          zoomForBounds(googleMap.getDimension(), bounds, googleMap.getModel().displayModel.getTileSize());
          mapViewPos.animateTo(bounds.getCenterPoint());
        }
        //redrawCourseOverlay();
      }
    });
  }

  /**
   * Sets the camera over a marker.
   * 
   * @param id the marker id
   */
  private void showMarker(final long id) {
    getActivity().runOnUiThread(new Runnable() {
        @Override
      public void run() {
        if (!isResumed() || googleMap == null) {
          return;
        }
        MyTracksProviderUtils MyTracksProviderUtils = getProvider();
        Waypoint waypoint = MyTracksProviderUtils.getWaypoint(id);
        if (waypoint != null && waypoint.getLocation() != null) {
          Location location = waypoint.getLocation();
          LatLong latLong = new LatLong(location.getLatitude(), location.getLongitude());
          keepCurrentLocationVisible = false;
          zoomToCurrentLocation = false;
          mapViewPos.animateTo(latLong);
        }
        //redrawCourseOverlay();
      }

    });
  }

  /**
   * Gets the default LatLong.
   */
  private LatLong getDefaultLatLong() {
    MyTracksProviderUtils myTracksProviderUtils = getProvider();
    Location location = myTracksProviderUtils.getLastValidTrackPoint();
    if (location != null) {
      return new LatLong(location.getLatitude(), location.getLongitude());
    } else {
      return new LatLong(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
    }
  }
  
  private MyTracksProviderUtils getProvider() {
    if (mUseCourseProvider) {
      return new MyTracksCourseProviderUtils(this.getActivity().getContentResolver());
    }
    return Factory.get(getActivity());
  }

  /**
   * Returns true if the location is visible. Needs to run on the UI thread.
   * 
   * @param location the location
   */
  private boolean isLocationVisible(Location location) {
    if (location == null || googleMap == null) {
      return false;
    }
    LatLong latLong = new LatLong(location.getLatitude(), location.getLongitude());
    Dimension dimension = googleMap.getModel().mapViewDimension.getDimension();
    int tileSize = googleMap.getModel().displayModel.getTileSize();
    BoundingBox bounds = MapPositionUtil.getBoundingBox(mapViewPos.getMapPosition(), dimension, tileSize);
    return bounds.contains(latLong);
  }
}