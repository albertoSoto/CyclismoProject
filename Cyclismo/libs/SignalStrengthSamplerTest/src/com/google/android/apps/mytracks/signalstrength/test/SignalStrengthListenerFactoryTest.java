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
package org.cowboycoders.cyclismo.signalstrength.test;

import org.cowboycoders.cyclismo.signalstrength.SignalStrengthListener;
import org.cowboycoders.cyclismo.signalstrength.SignalStrengthListener.SignalStrengthCallback;
import org.cowboycoders.cyclismo.signalstrength.SignalStrengthListenerCupcake;
import org.cowboycoders.cyclismo.signalstrength.SignalStrengthListenerEclair;
import org.cowboycoders.cyclismo.signalstrength.SignalStrengthListenerFactory;
import com.google.android.testing.mocking.AndroidMock;

import android.test.AndroidTestCase;

/**
 * Tests for {@link SignalStrengthListenerFactory}.
 * These tests require Eclair+ (API level 7) to run.
 *
 * @author Rodrigo Damazio
 */
public class SignalStrengthListenerFactoryTest extends AndroidTestCase {
  private boolean hasModernSignalStrength;
  private TestableSignalStrengthListenerFactory factory;
  private SignalStrengthCallback callback;

  private class TestableSignalStrengthListenerFactory extends SignalStrengthListenerFactory {
    @Override
    protected boolean hasModernSignalStrength() {
      return hasModernSignalStrength;
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    factory = new TestableSignalStrengthListenerFactory();
    callback = AndroidMock.createMock(SignalStrengthCallback.class);
  }

  public void testCreate_eclair() {
    hasModernSignalStrength = true;

    SignalStrengthListener listener = factory.create(getContext(), callback);
    assertTrue(listener.getClass().getName(),
        listener instanceof SignalStrengthListenerEclair);
  }

  public void testCreate_legacy() {
    hasModernSignalStrength = false;

    SignalStrengthListener listener = factory.create(getContext(), callback);
    assertTrue(listener.getClass().getName(),
        listener instanceof SignalStrengthListenerCupcake);
  }
}
