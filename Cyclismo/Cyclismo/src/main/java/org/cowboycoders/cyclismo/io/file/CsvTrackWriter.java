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
 * Copyright 2010 Google Inc.
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
package org.cowboycoders.cyclismo.io.file;

import android.content.Context;
import android.location.Location;

import org.cowboycoders.cyclismo.R;
import org.cowboycoders.cyclismo.content.MyTracksLocation;
import org.cowboycoders.cyclismo.content.Sensor;
import org.cowboycoders.cyclismo.content.Sensor.SensorData;
import org.cowboycoders.cyclismo.content.Sensor.SensorDataSet;
import org.cowboycoders.cyclismo.content.Track;
import org.cowboycoders.cyclismo.content.Waypoint;
import org.cowboycoders.cyclismo.io.file.TrackWriterFactory.TrackFileFormat;
import org.cowboycoders.cyclismo.util.LocationUtils;
import org.cowboycoders.cyclismo.util.StringUtils;
import org.cowboycoders.cyclismo.util.UnitConversions;
import org.fluxoid.utils.LatLongAlt;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;

public class CsvTrackWriter implements TrackFormatWriter {

  private static final NumberFormat SHORT_FORMAT = NumberFormat.getInstance();

  static {
    SHORT_FORMAT.setMaximumFractionDigits(4);
  }

  private final Context context;
  private PrintWriter printWriter;
  private Track track;
  private int segmentIndex;
  private int pointIndex;
  private Location previousLocation;

  public CsvTrackWriter(Context context) {
    this.context = context;
  }

  @Override
  public String getExtension() {
    return TrackFileFormat.CSV.getExtension();
  }

  @Override
  public void prepare(Track aTrack, OutputStream out) {
    track = aTrack;
    printWriter = new PrintWriter(out);
    segmentIndex = 0;
    pointIndex = 0;
  }

  @Override
  public void close() {
    printWriter.close();
  }

  @Override
  public void writeHeader() {
    writeCommaSeparatedLine(context.getString(R.string.generic_name),
        context.getString(R.string.track_edit_activity_type_hint),
        context.getString(R.string.generic_description));
    writeCommaSeparatedLine(track.getName(), track.getCategory(), track.getDescription());
    writeCommaSeparatedLine();
  }

  @Override
  public void writeFooter() {
    // Do nothing
  }

  @Override
  public void writeBeginWaypoints() {
    writeCommaSeparatedLine(context.getString(R.string.generic_name),
        context.getString(R.string.marker_edit_marker_type_hint),
        context.getString(R.string.generic_description),
        context.getString(R.string.description_location_latitude),
        context.getString(R.string.description_location_longitude),
        context.getString(R.string.description_location_altitude),
        context.getString(R.string.description_location_bearing),
        context.getString(R.string.description_location_accuracy),
        context.getString(R.string.description_location_speed),
        context.getString(R.string.description_time));
  }

  @Override
  public void writeEndWaypoints() {
    writeCommaSeparatedLine();
  }

  @Override
  public void writeWaypoint(Waypoint waypoint) {
    Location location = waypoint.getLocation();
    writeCommaSeparatedLine(waypoint.getName(),
        waypoint.getCategory(),
        waypoint.getDescription(),
        Double.toString(location.getLatitude()),
        Double.toString(location.getLongitude()),
        Double.toString(location.getAltitude()),
        Double.toString(location.getBearing()),
        SHORT_FORMAT.format(location.getAccuracy()),
        SHORT_FORMAT.format(location.getSpeed()),
        StringUtils.formatDateTimeIso8601(location.getTime()));
  }

  @Override
  public void writeBeginTrack(Location firstPoint) {
    writeCommaSeparatedLine(context.getString(R.string.description_track_segment),
        context.getString(R.string.description_track_point),
        context.getString(R.string.description_location_latitude),
        context.getString(R.string.description_location_longitude),
        context.getString(R.string.description_location_altitude),
        context.getString(R.string.description_location_bearing),
        context.getString(R.string.description_location_accuracy),
        context.getString(R.string.description_time),
        context.getString(R.string.description_location_speed),
        context.getString(R.string.description_spheroid_speed),
        context.getString(R.string.description_sensor_distance_meters),
        context.getString(R.string.description_sensor_power),
        context.getString(R.string.description_sensor_cadence),
        context.getString(R.string.description_sensor_heart_rate));
  }

  @Override
  public void writeEndTrack(Location lastPoint) {
    // Do nothing
  }

  @Override
  public void writeOpenSegment() {
    segmentIndex++;
    pointIndex = 0;
  }

  @Override
  public void writeCloseSegment() {
    // Do nothing
  }

  @Override
  public void writeLocation(Location location) {
    String distance = "";
    String power = "";
    String cadence = "";
    String heartRate = "";
    String spheroidSpeed = "";
    if (location instanceof MyTracksLocation) {
      SensorDataSet sensorDataSet = ((MyTracksLocation) location).getSensorDataSet();
      if (sensorDataSet != null) {
        if (sensorDataSet.hasDistance()) {
          SensorData sensorData = sensorDataSet.getDistance();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            distance = Float.toString(sensorData.getValue());
          }
        }
        if (sensorDataSet.hasPower()) {
          SensorData sensorData = sensorDataSet.getPower();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            power = Float.toString(sensorData.getValue());
          }
        }
        if (sensorDataSet.hasCadence()) {
          SensorData sensorData = sensorDataSet.getCadence();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            cadence = Float.toString(sensorData.getValue());
          }
        }
        if (sensorDataSet.hasHeartRate()) {
          SensorData sensorData = sensorDataSet.getHeartRate();
          if (sensorData.hasValue() && sensorData.getState() == Sensor.SensorState.SENDING) {
            heartRate = Float.toString(sensorData.getValue());
          }
        }
      }
    }

    if (previousLocation != null) {
        spheroidSpeed = Double.toString(getSpheroidSpeed(previousLocation, location));
    }

    writeCommaSeparatedLine(Integer.toString(segmentIndex),
        Integer.toString(pointIndex++),
        Double.toString(location.getLatitude()),
        Double.toString(location.getLongitude()),
        Double.toString(location.getAltitude()),
        Double.toString(location.getBearing()),
        SHORT_FORMAT.format(location.getAccuracy()),
        //StringUtils.formatDateTimeIso8601(location.getTime()), // Eg. 2015-08-09T12:35:57.880Z
        Long.toString(location.getTime()), // ms since the epoch
        SHORT_FORMAT.format(location.getSpeed()),
        spheroidSpeed,
        distance,
        power,
        cadence,
        heartRate);

    // Used for calculating the speed from lat longs as is done on some analysis tools.
    previousLocation = location;
  }

  /**
   * Calculates the average speed whilst travelling from the source to the destination across a
   * spheroidal Earth. This method is used by some tools for calculating the speed from TCX files
   * etc.
   *
   * @param src is the starting location.
   * @param dst is the finishing location.
   * @return speed in m/s.
   */
  private static double getSpheroidSpeed(Location src, Location dst) {
    LatLongAlt prevLatLong = LocationUtils.locationToLatLongAlt(src);
    LatLongAlt latLong = LocationUtils.locationToLatLongAlt(dst);
    double dist = org.fluxoid.utils.LocationUtils.getGradientCorrectedDistance(prevLatLong, latLong);
    double time_delta = (dst.getTime() - src.getTime()) / UnitConversions.S_TO_MS;
    return dist / time_delta;
  }

  /**
   * Writes a single line of a CSV file.
   *
   * @param values the values to be written as CSV
   */
  private void writeCommaSeparatedLine(String... values) {
    StringBuilder builder = new StringBuilder();
    boolean isFirst = true;
    for (String value : values) {
      if (!isFirst) {
        builder.append(',');
      }
      isFirst = false;

      builder.append('"');
      if (value != null) {
        builder.append(value.replaceAll("\"", "\"\""));
      }
      builder.append('"');
    }
    printWriter.println(builder.toString());
  }
}
