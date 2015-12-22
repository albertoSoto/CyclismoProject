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
package org.cowboycoders.cyclismo.stats;

import junit.framework.TestCase;

/**
 * Tests for {@link TripStatistics}.
 * This only tests non-trivial pieces of that class.
 *
 * @author Rodrigo Damazio
 */
public class TripStatisticsTest extends TestCase {

  private static final double DELTA = 1e-15;

  private TripStatistics statistics;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    statistics = new TripStatistics();
  }

  public void testSetBounds() {
    // This is not a trivial setter, conversion happens in it
    statistics.setBounds(12345, -34567, 56789, -98765);
    assertEquals(12345, statistics.getLeft());
    assertEquals(-34567, statistics.getTop());
    assertEquals(56789, statistics.getRight());
    assertEquals(-98765, statistics.getBottom());
  }

  public void testMerge() {
    TripStatistics statistics2 = new TripStatistics();
    statistics.setStartTime(1000L);  // Resulting start time
    statistics.setStopTime(2500L);
    statistics2.setStartTime(3000L);
    statistics2.setStopTime(4000L);  // Resulting stop time
    statistics.setTotalTime(1500L);
    statistics2.setTotalTime(1000L);  // Result: 1500+1000
    statistics.setMovingTime(700L);
    statistics2.setMovingTime(600L);  // Result: 700+600
    statistics.setTotalDistance(750.0);
    statistics2.setTotalDistance(350.0);  // Result: 750+350
    statistics.setTotalElevationGain(50.0);
    statistics2.setTotalElevationGain(850.0);  // Result: 850+50
    statistics.setMaxSpeed(60.0);  // Resulting max speed
    statistics2.setMaxSpeed(30.0);
    statistics.setMaxElevation(1250.0);
    statistics.setMinElevation(1200.0);  // Resulting min elevation
    statistics2.setMaxElevation(3575.0);  // Resulting max elevation
    statistics2.setMinElevation(2800.0);
    statistics.setMaxGrade(15.0);
    statistics.setMinGrade(-25.0);  // Resulting min grade
    statistics2.setMaxGrade(35.0);  // Resulting max grade
    statistics2.setMinGrade(0.0);
    statistics.setTotalWorkDone(350.0);
    statistics2.setTotalWorkDone(300.0);
    statistics.setTotalCrankRotations(14.0);
    statistics2.setTotalCrankRotations(12.0);
    statistics.setTotalHeartBeats(7.0);
    statistics2.setTotalHeartBeats(6.0);

    // Resulting bounds: -10000, 35000, 30000, -40000
    statistics.setBounds(-10000, 20000, 30000, -40000);
    statistics2.setBounds(-5000, 35000, 0, 20000);

    statistics.merge(statistics2);

    assertEquals(1000L, statistics.getStartTime());
    assertEquals(4000L, statistics.getStopTime());
    assertEquals(2500L, statistics.getTotalTime());
    assertEquals(1300L, statistics.getMovingTime());
    assertEquals(1100.0, statistics.getTotalDistance(), DELTA);
    assertEquals(900.0, statistics.getTotalElevationGain(), DELTA);
    assertEquals(60.0, statistics.getMaxSpeed(), DELTA);
    assertEquals(-10000, statistics.getLeft());
    assertEquals(30000, statistics.getRight());
    assertEquals(35000, statistics.getTop());
    assertEquals(-40000, statistics.getBottom());
    assertEquals(1200.0, statistics.getMinElevation(), DELTA);
    assertEquals(3575.0, statistics.getMaxElevation(), DELTA);
    assertEquals(-25.0, statistics.getMinGrade(), DELTA);
    assertEquals(35.0, statistics.getMaxGrade(), DELTA);
    assertEquals(650.0, statistics.getTotalWorkDone(), DELTA);
    assertEquals(13.0, statistics.getTotalHeartBeats(), DELTA);
    assertEquals(26.0, statistics.getTotalCrankRotations(), DELTA);
  }

  public void testGetMovingTimeSeconds() {
    statistics.setMovingTime(1000L);
    assertEquals(1.0, statistics.getMovingTimeSeconds());
  }

  public void testGetMovingTimeMinutes() {
    statistics.setMovingTime(120 * 1000L);
    assertEquals(2.0, statistics.getMovingTimeMinutes());
  }

  public void _testGetAverageSpeed() {
    statistics.setTotalDistance(1000.0);
    statistics.setTotalTime(1L); // Average is calculated only for finite total times
    statistics.setMovingTime(1000L); // in milliseconds
    assertEquals(1000.0, statistics.getAverageSpeed(), DELTA);

    statistics.setTotalTime(0L);
    assertEquals(0.0, statistics.getAverageSpeed(), DELTA);
  }

  public void testGetAverageMovingSpeed() {
    statistics.setTotalDistance(1000.0);
    statistics.setMovingTime(20000);  // in milliseconds
    assertEquals(50.0, statistics.getAverageMovingSpeed(), DELTA);
  }

  public void testGetAverageMovingPower() {
    statistics.setMovingTime(1000);
    statistics.setTotalWorkDone(350);
    assertEquals(350.0, statistics.getAverageMovingPower(), DELTA);
  }

  public void testGetAverageMovingCadence() {
    statistics.setMovingTime(1000);
    statistics.setTotalCrankRotations(1.2);
    assertEquals(72.0, statistics.getAverageMovingCadence(), DELTA);
  }

  public void testGetAverageMovingHeartRate() {
    statistics.setMovingTime(1000);
    statistics.setTotalHeartBeats(3.0);
    assertEquals(180.0, statistics.getAverageMovingHeartRate(), DELTA);
  }
}
