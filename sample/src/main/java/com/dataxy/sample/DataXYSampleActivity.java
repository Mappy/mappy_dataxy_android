package com.dataxy.sample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dataxy.Configuration;
import com.dataxy.DataXY;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

/**
 * DataXYSampleActivity : Sample Activity
 */
public class DataXYSampleActivity extends FragmentActivity {

    // Constant
    private static final String TAG = DataXYSampleActivity.class.toString();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // Declaration View
    private Button mAskForPermissionButton;
    private Button mToggleGPSButton;
    private Button mProfilingButton;
    private Button mStudyButton;
    private Button mConfigureButton;
    private EditText mInterval;
    private Button mIntervalButton;
    private EditText mMinimumPoints;
    private Button mMinimumPointsButton;
    private EditText mServerTier;
    private Button mUrlServerTierButton;
    private EditText mMaxTimeInterval;
    private Button mMaxTimeIntervalButton;

    // Declaration Variable
    private boolean enable = false;
    private LocationManager mLocationManager;
    private boolean mLocationEnabled = false;

    // Location Listener
    LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.d(TAG, location.getLatitude() + ";" + location.getLongitude());
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Data XY is registered from the DataXYApplication
        if (DataXY.isRegistered()) {
            Toast.makeText(DataXYSampleActivity.this, "DataXY is registered", Toast.LENGTH_SHORT).show();
        }

        // set application version name
        DataXY.setApplicationVersion("2.0");

        // add consent
        DataXY.addConsent("myconsent", true);

        // init View
        mAskForPermissionButton = findViewById(R.id.main_ask_for_permission);
        mToggleGPSButton = findViewById(R.id.main_enable_gps);
        mProfilingButton = findViewById(R.id.main_enable_profiling);
        mStudyButton = findViewById(R.id.main_enable_study);
        mConfigureButton = findViewById(R.id.main_enable_configure);
        mInterval = findViewById(R.id.main_edt_interval);
        mIntervalButton = findViewById(R.id.main_btn_interval);
        mMinimumPoints = findViewById(R.id.main_edt_minPoint);
        mMinimumPointsButton = findViewById(R.id.main_btn_minPoint);
        mUrlServerTierButton = findViewById(R.id.main_btn_url_server_tier);
        mServerTier = findViewById(R.id.main_url_server_tier);
        mMaxTimeInterval = findViewById(R.id.main_edt_maxTimeInterval);
        mMaxTimeIntervalButton = findViewById(R.id.main_btn_maxTimeInterval);

        // location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // enable the configuration of DataXY Worker
        DataXY.configure(this, new Configuration.Builder()
                .enable(enable)
                .build());
    }

    @Override
    protected void onResume() {
        // init Action View
        mAskForPermissionButton.setOnClickListener(this::onClickAskForPermission);
        mToggleGPSButton.setOnClickListener(this::onClickToggleGPS);
        mProfilingButton.setText(DataXY.isProfilingEnabled() ? "Disable Profiling" : "Enable Profiling");
        mProfilingButton.setOnClickListener(this::onClickProfiling);

        mStudyButton.setText(DataXY.isAnonymousStudiesLogEnabled() ? "Disable Anonymous Studies Log" : "Enable Anonymous Studies Log");
        mStudyButton.setOnClickListener(this::onClickAnonymousStudiesLog);

        mUrlServerTierButton.setOnClickListener(this::onClickUrlServerTier);

        // enable the configuration of DataXY
        mConfigureButton.setText(enable ? "Disable Configure" : "Enable Configure");
        mConfigureButton.setOnClickListener(this::onClickConfigure);

        // set Interval time in milliseconds of configuration Job DataXY
        mIntervalButton.setOnClickListener(this::onClickIntervalTime);

        // set max Interval time in milliseconds of configuration Job DataXY
        mMaxTimeIntervalButton.setOnClickListener(this::onClickMaxIntervalTime);

        // set minimum number of points of configuration Job Data XY
        mMinimumPointsButton.setOnClickListener(this::onClickMinimumPoints);
        super.onResume();
    }

    /**
     * Action on Component onClickUrlServerTier : change url server tier
     *
     * @param v : the view
     */
    private void onClickUrlServerTier(View v) {
        String text = mServerTier.getText().toString();
        if (text.isEmpty()) {
            text = null;
        }
        DataXY.enableServerTier(v.getContext(), text != null);
        boolean result = DataXY.setUrlServerTier(v.getContext(), text);
        if (result) {
            Toast.makeText(v.getContext(), "Server Tier Url is now " + text, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(v.getContext(), "the url is not right, must be https://*.*", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Action on Component AskForPermission : check if the permission is granted
     *
     * @param v : the view
     */
    private void onClickAskForPermission(View v) {
        if (askForFineLocationPermission()) {
            Toast.makeText(v.getContext(), "Fine Location Permission is already granted", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Action on Component Configure : enable or disable this feature
     *
     * @param v : the view
     */
    private void onClickConfigure(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            enable = !enable;
            DataXY.configure(v.getContext(), new Configuration.Builder()
                    .enable(enable)
                    .build());
            mConfigureButton.setText(enable ? "Disable Configure" : "Enable Configure");
            Toast.makeText(v.getContext(), "configure " + (enable ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(v.getContext(), "Need LOLLIPOP", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Action on Component ToggleGPS : enable or disable this feature
     *
     * @param v : the view
     */
    private void onClickToggleGPS(View v) {
        final boolean isLocationEnabled = toggleGPS();
        mToggleGPSButton.setText(isLocationEnabled ? "Disable GPS" : "Enable GPS");
        Toast.makeText(v.getContext(), "GPS is now " + (isLocationEnabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    /**
     * Action on Anonymous Studies Log : enable or disable this feature
     *
     * @param v : the view
     */
    private void onClickAnonymousStudiesLog(View v) {
        DataXY.enableAnonymousStudiesLog(v.getContext(), !DataXY.isAnonymousStudiesLogEnabled());
        boolean enable = DataXY.isAnonymousStudiesLogEnabled();
        mStudyButton.setText(enable ? "Disable Anonymous Studies Log" : "Enable Anonymous Studies Log");
        Toast.makeText(v.getContext(), "Anonymous Studies Log is now " + (enable ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    /**
     * Action on Profiling : enable or disable this feature
     *
     * @param v : the view
     */
    private void onClickProfiling(View v) {
        DataXY.enableProfiling(v.getContext(), !DataXY.isProfilingEnabled());
        boolean enable = DataXY.isProfilingEnabled();
        mProfilingButton.setText(enable ? "Disable Profiling" : "Enable Profiling");
        Toast.makeText(v.getContext(), "Profiling is now " + (enable ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }

    /**
     * Action on Interval Time : set the interval time in milliseconds in configure
     *
     * @param v : the view
     */
    private void onClickIntervalTime(View v) {
        String text = mInterval.getText().toString();
        try {
            int time = Integer.parseInt(text);
            if (time > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DataXY.configure(v.getContext(), new Configuration.Builder()
                            .setInterval(time)
                            .build());
                    Toast.makeText(v.getContext(), "Time Interval set : " + time, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "Need LOLLIPOP", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(v.getContext(), "Time Interval error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Action on Max Interval Time : set the interval time in milliseconds in configure
     *
     * @param v : the view
     */
    private void onClickMaxIntervalTime(View v) {
        String text = mMaxTimeInterval.getText().toString();
        try {
            int time = Integer.parseInt(text);
            if (time > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DataXY.configure(v.getContext(), new Configuration.Builder()
                            .setMaximumInterval(time)
                            .build());
                    Toast.makeText(v.getContext(), "Max time Interval set : " + time, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "Need LOLLIPOP", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(v.getContext(), "Time Interval error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Action on component minimumPoint : set the minimum number of points of configure
     *
     * @param v : the view
     */
    private void onClickMinimumPoints(View v) {
        String text = mMinimumPoints.getText().toString();
        try {
            int minimumNbPoint = Integer.parseInt(text);
            if (minimumNbPoint >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    DataXY.configure(v.getContext(), new Configuration.Builder()
                            .setMinimumPoints(minimumNbPoint)
                            .build());
                    Toast.makeText(v.getContext(), "Minimum Points set : " + minimumNbPoint, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), "Need LOLLIPOP", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(v.getContext(), "Minimum Points error : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        // reset Action View
        mAskForPermissionButton.setOnClickListener(null);
        mToggleGPSButton.setOnClickListener(null);
        mProfilingButton.setOnClickListener(null);
        mStudyButton.setOnClickListener(null);
        mIntervalButton.setOnClickListener(null);
        mMinimumPointsButton.setOnClickListener(null);

        // reset GPS by default for sample
        disableGPS();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * toggle GPS, enable/disable
     *
     * @return the status of GPS
     */
    @UiThread
    private boolean toggleGPS() {
        if (mLocationEnabled) {
            disableGPS();
        } else {
            enableGPS();
        }
        return mLocationEnabled;
    }

    /**
     * Enable GPS
     */
    private void enableGPS() {
        // check permission and add the listener in locationManager
        if (checkFineLocationPermissionGranted()) {
            for (String provider : mLocationManager.getAllProviders()) {
                //noinspection MissingPermission since check is done in #checkFineLocationPermissionGranted
                mLocationManager.requestLocationUpdates(provider, 0, 0, mLocationListener);
            }
            mLocationEnabled = true;
        } else {
            Toast.makeText(DataXYSampleActivity.this, "Location Permission is not granted", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Disable GPS
     */
    private void disableGPS() {
        if (checkFineLocationPermissionGranted()) {
            this.mLocationManager.removeUpdates(mLocationListener);
        }
        mLocationEnabled = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (DataXY.onRequestPermissionsResult(this)) {
            Toast.makeText(DataXYSampleActivity.this, "DataXY is registered", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @return true if fine location permission is granted, false otherwise
     */
    private boolean checkFineLocationPermissionGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * <p>Manage permission request for fine location permission</p>
     *
     * @return true if permission is already granted, false otherwise
     */
    private boolean askForFineLocationPermission() {
        String permissionName = Manifest.permission.ACCESS_FINE_LOCATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkFineLocationPermissionGranted()) {
                return true;
            }

            ActivityCompat.requestPermissions(this, new String[]{permissionName}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }

        final boolean isPermissionInManifest = getPackageManager().checkPermission(permissionName, getPackageName()) == PackageManager.PERMISSION_GRANTED;
        if (!isPermissionInManifest) {
            Log.e(TAG, "You have not granted '" + permissionName + "' permission in your manifest");
        }

        return true;
    }
}
