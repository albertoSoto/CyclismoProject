package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.fragments.CourseSetupFragment;
import com.google.android.apps.mytracks.fragments.CourseSetupFragment.CourseSetupObserver;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.turbo.TurboService;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class CourseSetupActivity extends Activity {
  
  public CourseSetupActivity() {
    super();
  }
  
  public CourseSetupActivity(String test) {
    super();
  }
  
  private static final String TAG = "CourseSetupActivty";
  private CourseSetupObserver courseSetupObserver;
  
  /**
   * long reference assignment non atomic
   * @return the trackId
   */
  private synchronized Long getTrackId() {
    return trackId;
  }

  /**
   * long reference assignment non atomic
   * @param trackId the trackId to set
   */
  private synchronized void setTrackId(Long trackId) {
    Log.d(TAG, "setTrackID"  + trackId);
    this.trackId = trackId;
  }

  /**
   * @return the modeString
   */
  private String getModeString() {
    return modeString;
  }

  /**
   * @param modeString the modeString to set
   */
  private  void setModeString(String modeString) {
    this.modeString = modeString;
  }

  protected Long trackId;
  protected String modeString;
  private Button goButton;
  private boolean mIsBound;
  
  private ServiceConnection mConnection = new ServiceConnection() {

    public void onServiceConnected(ComponentName className, IBinder binder) {
      TurboService s = ((TurboService.TurboBinder) binder).getService();
      s.start(getTrackId(),CourseSetupActivity.this);
      Toast.makeText(CourseSetupActivity.this, "Connected to turbo service",
          Toast.LENGTH_SHORT).show();
      // no longer needed
      doUnbindService();
    }

    public void onServiceDisconnected(ComponentName className) {
      Toast.makeText(CourseSetupActivity.this, "Disconnected from turbo service",
          Toast.LENGTH_SHORT).show();
    }
  };

  
  void doBindService() {
    bindService(new Intent(this, TurboService.class), mConnection,
        Context.BIND_AUTO_CREATE);
    mIsBound = true;
  }
  
  void doUnbindService() {
    if (mIsBound) {
        // Detach our existing connection.
        unbindService(mConnection);
        mIsBound = false;
    }
}
  
  private void startServiceInBackround() {
    Intent intent = new Intent(this, TurboService.class);
    this.startService(intent);
  }
  
  
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  
  private Runnable bindChangedCallback = new Runnable() {

    @Override
    public void run() {
      
      boolean success = true;
      
      if (!startNewRecording) {
        return;
      }

      ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
      if (service == null) {
        Log.d(TAG, "service not available to start a new recording");
        return;
      }
      try {
        long id = service.startNewTrack();
        startNewRecording = false;
        Intent intent = IntentUtils.newIntent(CourseSetupActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, id)
            .putExtra(TrackDetailActivity.EXTRA_USE_COURSE_PROVIDER, false)
            .putExtra(TrackDetailActivity.EXTRA_COURSE_TRACK_ID, trackId);
        startActivity(intent);
        Toast.makeText(
            CourseSetupActivity.this, R.string.track_list_record_success, Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        Toast.makeText(CourseSetupActivity.this, R.string.track_list_record_error, Toast.LENGTH_LONG)
            .show();
        Log.e(TAG, "Unable to start a new recording.", e);
        success = false;
      } 
      
      CourseSetupActivity.this.finish(success);
      
    }
    
    
    
  };
  private boolean startNewRecording = false;

  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
      setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
      this.setContentView(R.layout.course_select);
      
      trackRecordingServiceConnection = new TrackRecordingServiceConnection(
          this, bindChangedCallback);
      
      
      this.goButton = (Button) this.findViewById(R.id.course_select_go);
      
      Button cancelButton = (Button) this.findViewById(R.id.course_select_cancel);
      
      cancelButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          CourseSetupActivity.this.finish(false);
        }
        
      });
      
      goButton.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          startServiceInBackround();
          doBindService();
          startRecording();
          
        }
        
      });
      
      
      this.courseSetupObserver = new CourseSetupFragment.CourseSetupObserver() {
         
        @Override
        public void onTrackIdUpdate(Long trackIdIn) {
          setTrackId(trackIdIn);
          validate();
        }
        
        @Override
        public void onCourseModeUpdate(String modeStringIn) {
          setModeString(modeStringIn);
          validate();
          
        }
      };
      
       getFragmentManager().beginTransaction().replace(R.id.course_select_preferences,
       new CourseSetupFragment(courseSetupObserver)).commit();
  }
  
  
  
  
  /* (non-Javadoc)
   * @see android.app.Activity#onStart()
   */
  @Override
  protected void onStart() {
    super.onStart();
    
        
  }

  protected void finish(boolean trackStarted) {
    Intent resultData = new Intent();
    if (trackStarted) {
      setResult(Activity.RESULT_OK, resultData);
    } else {
      setResult(Activity.RESULT_CANCELED, resultData);
    }
    finish();
    
  }
  
  


  /**
   * this disables/enables the go button
   */
  private void validate() {
    String mode = getModeString();
    
    boolean valid = true;
    
    if (mode.equals(getString(R.string.settings_courses_mode_simulation_value))) {
   
      if (!validateSimulationMode()) {
        valid = false;
      }
    }
    
    updateUi(valid);
    
  }
  
  @Override
  public void finish() {
    
    super.finish();
  }
  
  /**
   * Starts a new recording.
   */
  private void startRecording() {
    startNewRecording = true;
    trackRecordingServiceConnection.startAndBind();

    /*
     * If the binding has happened, then invoke the callback to start a new
     * recording. If the binding hasn't happened, then invoking the callback
     * will have no effect. But when the binding occurs, the callback will get
     * invoked.
     */
    bindChangedCallback.run();
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    trackRecordingServiceConnection.unbind();
  }

  private void updateUi(boolean valid) {
    Log.d(TAG,"updating ui: " + valid);
    goButton.setEnabled(valid);
  }

  private boolean validateSimulationMode() {
    Long localTrackId = getTrackId();
    
    if(localTrackId == null) {
      return false;
    }
    else if (localTrackId.equals(-1L)) {
      return false;
    }
    return true;
  }

  
  
 
}    