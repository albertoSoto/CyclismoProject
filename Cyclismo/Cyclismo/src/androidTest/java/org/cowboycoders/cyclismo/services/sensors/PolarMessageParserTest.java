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
package org.cowboycoders.cyclismo.services.sensors;

import junit.framework.TestCase;

import org.cowboycoders.cyclismo.content.Sensor;
import org.cowboycoders.cyclismo.util.ApiAdapterFactory;

public class PolarMessageParserTest extends TestCase {

  PolarMessageParser parser = new PolarMessageParser();
  // A complete and valid Polar HxM packet
  //   FE08F701D1001104FE08F702D1001104
  private final byte[] originalBuf =
    {(byte) 0xFE, 0x08, (byte) 0xF7, 0x01, (byte) 0xD1, 0x00, 0x11, 0x04, (byte) 0xFE, 0x08,
     (byte) 0xF7, 0x02, (byte) 0xD1, 0x00, 0x11, 0x04};
  private byte[] buf;

  public void setUp() {
    buf = ApiAdapterFactory.getApiAdapter().copyByteArray(originalBuf, 0, originalBuf.length);
  }

  public void testIsValid() {
	assertTrue(parser.isValid(buf));
  }
  
  public void testIsValid_invalidHeader() {
	// Invalidate header.
    buf[0] = 0x03;
    assertFalse(parser.isValid(buf));
  }
  
  public void testIsValid_invalidCheckbyte() {
	// Invalidate checkbyte.
    buf[2] = 0x03;
    assertFalse(parser.isValid(buf));
  }
  
  public void testIsValid_invalidSequence() {
    // Invalidate sequence.
    buf[3] = 0x11;
    assertFalse(parser.isValid(buf));
  }

  public void testParseBuffer() {
    buf[5] = 70;
    Sensor.SensorDataSet sds = parser.parseBuffer(buf);
    assertTrue(sds.hasHeartRate());
    assertTrue(sds.getHeartRate().getState() == Sensor.SensorState.SENDING);
    assertEquals(70, sds.getHeartRate().getValue());
  }

  public void testFindNextAlignment_offset() {
	// The first 4 bytes are garbage
	buf = new byte[originalBuf.length + 4];
	buf[0] = 4;
	buf[1] = 2;
	buf[2] = 4;
	buf[3] = 2;
	
	// Then the valid message.
	System.arraycopy(originalBuf, 0, buf, 4, originalBuf.length);
    assertEquals(4, parser.findNextAlignment(buf));
  }

  public void testFindNextAlignment_invalid() {
    buf[0] = 0;
    assertEquals(-1, parser.findNextAlignment(buf));
  }
}
