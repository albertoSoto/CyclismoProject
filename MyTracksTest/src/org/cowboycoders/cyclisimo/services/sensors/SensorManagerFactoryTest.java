package org.cowboycoders.cyclisimo.services.sensors;

import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import org.cowboycoders.cyclisimo.Constants;
import org.cowboycoders.cyclisimo.services.sensors.PolarSensorManager;
import org.cowboycoders.cyclisimo.services.sensors.SensorManager;
import org.cowboycoders.cyclisimo.services.sensors.SensorManagerFactory;
import org.cowboycoders.cyclisimo.services.sensors.ZephyrSensorManager;
import org.cowboycoders.cyclisimo.util.ApiAdapterFactory;
import org.cowboycoders.cyclisimo.util.PreferencesUtils;

public class SensorManagerFactoryTest extends AndroidTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    SharedPreferences sharedPreferences = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    // Let's use default values.
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(sharedPreferences.edit().clear());
  }

  @SmallTest
  public void testDefaultSettings() throws Exception {
    assertNull(SensorManagerFactory.getSystemSensorManager(getContext()));
  }

  @SmallTest
  public void testCreateZephyr() throws Exception {
    assertClassForName(ZephyrSensorManager.class, R.string.sensor_type_value_zephyr);
  }

  @SmallTest
  public void testCreatePolar() throws Exception {
    assertClassForName(PolarSensorManager.class, R.string.sensor_type_value_polar);
  }

  private void assertClassForName(Class<?> c, int i) {
    PreferencesUtils.setString(getContext(), R.string.sensor_type_key, getContext().getString(i));
    SensorManager sm = SensorManagerFactory.getSystemSensorManager(getContext());
    assertNotNull(sm);
    assertTrue(c.isInstance(sm));
    SensorManagerFactory.releaseSystemSensorManager();
  }
}
