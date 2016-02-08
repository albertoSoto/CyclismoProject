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
package org.cowboycoders.cyclismo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

import org.cowboycoders.cyclismo.Constants;
import org.cowboycoders.cyclismo.R;
import org.cowboycoders.cyclismo.content.Bike;
import org.cowboycoders.cyclismo.content.CyclismoProviderUtils;
import org.cowboycoders.cyclismo.content.MyTracksCourseProviderUtils;
import org.cowboycoders.cyclismo.content.MyTracksProviderUtils;
import org.cowboycoders.cyclismo.content.Track;
import org.cowboycoders.cyclismo.content.User;
import org.cowboycoders.cyclismo.settings.AbstractSettingsFragment;
import org.cowboycoders.cyclismo.util.PreferencesUtils;

import java.util.ArrayList;
import java.util.List;

public class CourseSetupFragment extends AbstractSettingsFragment {
  private final static String TAG = CourseSetupFragment.class.getSimpleName();

  public static final int PICK_COURSE_REQUEST = 1567;

  private OnSharedPreferenceChangeListener sharedListener;
  private SharedPreferences sharedPreferences;
  private UpdateSummaryCaller updateTrackIdSummaryCaller;
  private UpdateSummaryCaller updateBikeSummaryCaller;
  private List<CourseSetupObserver> observers = new ArrayList<>();
  private Preference courseTrackIdPreference;
  private Preference bikeSelectPreference;

  public CourseSetupFragment addObserver(CourseSetupObserver observer) {
    observers.add(observer);
    return this;
  }

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Context context = this.getActivity();
    Log.i(TAG, "fragment onCreate");
    addPreferencesFromResource(R.xml.course_settings);

    //this is set early as it required for updateUiByCourseMode
    this.courseTrackIdPreference = (Preference) findPreference(
        getString(R.string.course_track_id));

    this.bikeSelectPreference = (Preference) findPreference(
        getString(R.string.course_bike_id));

    ListPreference courseModeListPreference = (ListPreference) findPreference(
        getString(R.string.course_mode));

    OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        updateUiByCourseMode((String) newValue);
        return true;
      }
    };
    String courseModeValue = PreferencesUtils.getString(
        this.getActivity(), R.string.course_mode, getString(R.string.settings_courses_mode_simulation_value));
    this.
        configurePreference(courseModeListPreference,
            getResources().getStringArray(R.array.course_mode_select_options),
            getResources().getStringArray(R.array.course_mode_select_values),
            R.string.settings_courses_mode_summary, courseModeValue, listener, null);

    if (courseModeListPreference.getValue() == null) {
      courseModeListPreference.setValueIndex(0);
    }

    courseModeListPreference.setDefaultValue(PreferencesUtils.COURSE_MODE_DEFAULT);

    Long courseTrackIdValue = PreferencesUtils.getLong(context, R.string.course_track_id);
    Long bikeSelectValue = PreferencesUtils.getLong(context, R.string.settings_select_bike_current_selection_key);

    bikeSelectPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        startActivityForResult(preference.getIntent(), R.string.bike_select_request_code);
        return true;
      }
    });

    PreferencesUtils.SettingsSelectionSummarizer bikeSelectSummarizer = new PreferencesUtils.SettingsSelectionSummarizer() {

      @Override
      public String summarize(Object value) {
        return summarizeBikeSelection(value);
      }

    };

    this.updateBikeSummaryCaller = new UpdateSummaryCaller(
        bikeSelectPreference, null, null, R.string.settings_courses_bike_select_summary, bikeSelectSummarizer);

    // initial trackId update
    updateUiByBikeSelect(bikeSelectValue);

    courseTrackIdPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        startActivityForResult(preference.getIntent(), PICK_COURSE_REQUEST);
        return true;
      }
    });

    PreferencesUtils.SettingsSelectionSummarizer summarizer = new PreferencesUtils.SettingsSelectionSummarizer() {

      @Override
      public String summarize(Object value) {
        return summarizeTrackIdSelection(value);
      }

    };

    this.updateTrackIdSummaryCaller = new UpdateSummaryCaller(
        courseTrackIdPreference, null, null, R.string.settings_courses_route_summary, summarizer);

    // initial trackId update
    updateUiByCourseTrackId(courseTrackIdValue);

    this.sharedPreferences = this.getActivity().getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    this.sharedListener = new OnSharedPreferenceChangeListener() {

      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferencesIn, String key) {
        if (key.equals(PreferencesUtils.getKey(getActivity(), R.string.course_track_id))) {
          Long newValue = sharedPreferences.getLong(key, -1l);
          Log.i(TAG, "detected new track: " + newValue);
          updateUiByCourseTrackId(newValue);
        } else if (key.equals(PreferencesUtils.getKey(getActivity(), R.string.settings_select_bike_current_selection_key))) {
          Long newValue = sharedPreferences.getLong(key, -1l);
          Log.i(TAG, "detected new bike: " + newValue);
          updateUiByBikeSelect(newValue);
        }

      }

    };

    for (CourseSetupObserver observer : observers) {
      observer.onCourseModeUpdate(courseModeValue);
      observer.onTrackIdUpdate(courseTrackIdValue);
      updateUiByBikeSelect(bikeSelectValue);
    }
  }

  /* (non-Javadoc)
   * @see android.preference.PreferenceFragment#onStart()
   */
  @Override
  public void onStart() {
    super.onStart();
    this.sharedPreferences.registerOnSharedPreferenceChangeListener(sharedListener);
  }

  @Override
  public void onResume() {
    super.onResume();
    //this.sharedPreferences.registerOnSharedPreferenceChangeListener(sharedListener);
  }

  @Override
  public void onStop() {
    super.onStop();
    //sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedListener);
  }

  @Override
  public void onDestroy() {
    super.onStop();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedListener);
  }

  protected String summarizeTrackIdSelection(Object value) {
    Long trackId = (Long) value;
    Log.i(TAG, "Selected track id: " + trackId);
    String noneSelected = this.getString(R.string.course_not_selected);
    if (trackId == -1) {
      return noneSelected;
    }
    Track track = new MyTracksCourseProviderUtils(this.getActivity().getContentResolver()).getTrack(trackId);
    if (track == null) {
      Log.w(TAG, "track invalid : shared preference mismatch");
      PreferencesUtils.setLong(this.getActivity(), R.string.course_track_id, -1L);
      return noneSelected;
    }
    return track.getName();
  }

  private static User getUser(Context context) {
    final CyclismoProviderUtils providerUtils = MyTracksProviderUtils.Factory.getCyclimso(context);
    final long currentUserId = PreferencesUtils.getLong(context, R.string.settings_select_user_current_selection_key);
    User user = providerUtils.getUser(currentUserId);
    return user;
  }

  private void updateUiByBikeSelect(final Long newValue) {
    Log.i(TAG, "new bike selected with id: " + newValue);
    this.updateBikeSummaryCaller.updateSummary(newValue);
    for (final CourseSetupObserver observer : observers) {
      new AsyncTask<Object, Integer, Bike>() {

        @Override
        protected Bike doInBackground(Object... params) {
          long bikeId = getCurrentBikeId();
          if (bikeId == -1L) {
            observer.onBikeUpdate(null);
            return null;
          }
          Bike bike = loadBike(bikeId);
          observer.onBikeUpdate(bike);
          return bike;
        }

      }.execute(new Object());
    }
  }

  private long getCurrentBikeId() {
    User currentUser = getUser(CourseSetupFragment.this.getActivity());
    long bikeId = currentUser.getCurrentlySelectedBike();
    return bikeId;
  }

  private Bike loadBike(long id) {
    return MyTracksProviderUtils.Factory.getCyclimso(this.getActivity()).getBike(id);
  }

  protected String summarizeBikeSelection(Object value) {
    Long bikeId = (Long) value;
    String noneSelected = this.getString(R.string.course_not_selected);
    // the bikeid coming is from the shared preference / get current users bike
    bikeId = getCurrentBikeId();
    if (bikeId == -1L) {
      return noneSelected;
    }
    Bike bike = loadBike(bikeId);
    if (bike == null) {
      Log.w(TAG, "bike invalid : shared preference mismatch");
      PreferencesUtils.setLong(this.getActivity(), R.string.settings_select_bike_current_selection_key, -1L);
      return noneSelected;
    }
    return bike.getName();
  }

  private void updateUiByCourseTrackId(Long newValue) {
    Log.i(TAG, "new track selected with id: " + newValue);
    this.updateTrackIdSummaryCaller.updateSummary(newValue);
    for (CourseSetupObserver observer : observers) {
      observer.onTrackIdUpdate(newValue);
    }
  }

  private void updateUiByCourseMode(String newValue) {
    Log.i(TAG, "course mode: " + newValue);
    for (CourseSetupObserver observer : observers) {
      observer.onCourseModeUpdate(newValue);
    }
    if (!newValue.equals(getString(R.string.settings_courses_mode_simulation_value))) {
      courseTrackIdPreference.setEnabled(false);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == PICK_COURSE_REQUEST) {
      if (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED) {
        if (data != null) {
          Long trackId = data.getLongExtra(getString(R.string.course_track_id), -1L);
          updateUiByCourseTrackId(trackId);
        } else {
          Log.d(TAG, "onActivityResult : data was null");
          updateUiByCourseTrackId(-1L);
        }
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  public interface CourseSetupObserver {
    /**
     * @param trackId the new trackID
     */
    void onTrackIdUpdate(Long trackId);
    void onBikeUpdate(Bike bike);
    void onCourseModeUpdate(String modeString);
  }
}