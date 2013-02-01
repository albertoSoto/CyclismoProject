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

package org.cowboycoders.cyclisimo.settings;

import org.cowboycoders.cyclisimo.R;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import org.cowboycoders.cyclisimo.util.PreferencesUtils;

/**
 * An activity for accessing map settings.
 * 
 * @author Jimmy Shih
 */
public class CourseSetupActivity extends AbstractSettingsActivity {

  private static final String TAG = CourseSetupActivity.class.getSimpleName();



  @SuppressWarnings("deprecation")
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    addPreferencesFromResource(R.xml.course_settings);
    ListPreference trackColorModeListPreference = (ListPreference) findPreference(
        getString(R.string.course_mode));
    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
        @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateUiByTrackColorMode((String) newValue);
        return true;
      }

        private void updateUiByTrackColorMode(String newValue) {
          // TODO Auto-generated method stub
          
        }
    };
    String trackColorModeValue = PreferencesUtils.getString(
        this, R.string.course_mode, PreferencesUtils.COURSE_MODE_DEFAULT);
    configurePreference(trackColorModeListPreference,
        getResources().getStringArray(R.array.course_mode_select_options),
        getResources().getStringArray(R.array.course_mode_select_values),
        R.string.settings_courses_mode_summary, trackColorModeValue, listener);


  }


}
