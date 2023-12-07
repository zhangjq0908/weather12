/**
 * This file is part of TinyWeatherForecastGermany.
 *
 * Copyright (c) 2020, 2021, 2022, 2023 Pawel Dube
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.kaffeemitkoffein.tinyweatherforecastgermany;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.*;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.*;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private final static String SIS_ABOUT_DIALOG_STATE="ABOUT_DIALOG_VISIBLE";
    private final static String SIS_WHATSNEW_DIALOG_STATE="WHATSNEW_DIALOG_VISIBLE";
    public final static String MAINAPP_CUSTOM_REFRESH_ACTION = "MAINAPP_CUSTOM_ACTION_REFRESH";
    public final static String MAINAPP_SSL_ERROR = "MAINAPP_SSL_ERROR";
    public final static String MAINAPP_SHOW_PROGRESS = "SHOW_PROGRESS";
    public final static String MAINAPP_HIDE_PROGRESS = "HIDE_PROGRESS";
    public final static String MAINAPP_AREADB_PROGRESS = "AREADB_PROGRESS";
    public final static String MAINAPP_AREADB_READY = "AREADB_READY";

    public final static String EXTRA_AREADB_PROGRESS_VALUE = "AREADB_PRGS_VALUE";
    public final static String EXTRA_AREADB_PROGRESS_TEXT = "AREADB_PRGS_TEXT";

    public final static boolean API_TESTING_ENABLED = false;
    private int test_position = 0;

    private ForecastAdapter forecastAdapter;

    Context context;
    StationsManager stationsManager;
    StationFavorites stationFavorites;
    Spinner spinner;
    ListView weatherList;
    AutoCompleteTextView autoCompleteTextView;
    StationSearchEngine stationSearchEngine;

    public static final SimpleDateFormat hourMinuteSecondMilliSecDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");

    CurrentWeatherInfo weatherCard;

    long last_updateweathercall = Calendar.getInstance().getTimeInMillis();

    private AlertDialog aboutDialog;
    private boolean aboutDiaglogVisible=false;

    private boolean forceWeatherUpdateFlag = false;

    private AlertDialog whatsNewDialog;
    private boolean whatsNewDialogVisible=false;

    Executor executor;

    ArrayList<WeatherWarning> localWarnings;

    WeatherLocationManager weatherLocationManager;

    long foreCastAdapterLastClickOnItem = 0;

    PopupWindow hintPopupWindow = null;

    private boolean performingFirstAppLaunch = false;

    private Activity thisActivity;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            final String errorText = DataUpdateService.StopReason.getStopReasonErrorText(context,intent);
            if ((errorText!=null) && (forceWeatherUpdateFlag)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, errorText, Toast.LENGTH_LONG).show();
                    }
                });
            }
            if (intent.getAction().equals(MAINAPP_CUSTOM_REFRESH_ACTION)){
                PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"received broadcast => custom refresh action");
                displayWeatherForecast();
                forceWeatherUpdateFlag = false;
                if (API_TESTING_ENABLED){
                    test_position ++;
                    testAPI_Call();
                }
            }
            if (intent.getAction().equals(MAINAPP_SSL_ERROR)){
                PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.WARN,"received broadcast => ssl error intent received by main app.");
                if ((!WeatherSettings.isTLSdisabled(context)) && (Build.VERSION.SDK_INT < 28)){
                    AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity,0);
                    builder.setTitle(context.getResources().getString(R.string.connerror_title));
                    Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_ANNOUNCEMENT,false));
                    builder.setIcon(drawable);
                    builder.setMessage(context.getResources().getString(R.string.connerror_message));
                    builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // do nothing
                            dialogInterface.dismiss();
                        }
                    });
                    builder.setPositiveButton(R.string.alertdialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            WeatherSettings.setDisableTLS(getApplicationContext(),true);
                            // re-launch the weather update via http
                            PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.WARN,"SSL disabled permanently due to errors.");
                            PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"re-launching weather update.");
                            forcedWeatherUpdate();
                            dialogInterface.dismiss();
                        }
                    });
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
                    alertDialog.show();
                    ThemePicker.tintAlertDialogButtons(context,alertDialog);
                }
            }
            if (intent.getAction().equals(MainActivity.MAINAPP_SHOW_PROGRESS)){
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.main_progressbar);
                if (progressBar!=null){
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
            if (intent.getAction().equals(MainActivity.MAINAPP_HIDE_PROGRESS)){
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.main_progressbar);
                if (progressBar!=null){
                    progressBar.setVisibility(View.INVISIBLE);
                }
                forceWeatherUpdateFlag = false;
            }
            if (intent.getAction().equals(WeatherWarningActivity.WEATHER_WARNINGS_UPDATE)){
                PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"broadcast received => new weather warnings.");
                displayWeatherForecast();
                if (forceWeatherUpdateFlag){
                    forceWeatherUpdateFlag = false;
                }
            }
            if (intent.getAction().equals(MAINAPP_AREADB_PROGRESS)){
                if (intent.hasExtra(MainActivity.EXTRA_AREADB_PROGRESS_TEXT) && (intent.hasExtra(MainActivity.EXTRA_AREADB_PROGRESS_VALUE))){
                    Bundle bundle = intent.getExtras();
                    showAreaDatabaseProgress(intent.getIntExtra(MainActivity.EXTRA_AREADB_PROGRESS_VALUE,0),intent.getStringExtra(MainActivity.EXTRA_AREADB_PROGRESS_TEXT));
                }
            }
            if (intent.getAction().equals(MAINAPP_AREADB_READY)){
                loadStationsData();
                hideAreaDatabaseProgress();
            }
        }
    };

    final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
            // weather settings must be read at the time of selection!
            WeatherSettings weatherSettings = new WeatherSettings(context);
            /*
             * We found a bug; compare to https://developer.android.com/reference/android/widget/AdapterView.OnItemClickListener.
             * pos is the same as id, returning the position of the clicked item from top like shown on the screen, but
             * NOT the position in the adapter. We therefore have to get it manually from our own StationsManager class.
             */
            TextView tv = (TextView) view.findViewById(R.id.dropdown_textitem);
            String station_description = tv.getText().toString();
            Integer station_pos = stationsManager.getPositionFromDescription(station_description,true);
            if (station_pos != null) {
                if (!weatherSettings.station_name.equals(stationsManager.getName(station_pos)) && (last_updateweathercall + 3000 < Calendar.getInstance().getTimeInMillis())) {
                    newWeatherRegionSelected(stationsManager.getLocationFromDescription(station_description));
                    if (autoCompleteTextView != null) {
                        autoCompleteTextView.setText("");
                        autoCompleteTextView.clearListSelection();
                    }
                    try{
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(),0);
                        imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                    } catch (Exception e){
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"Warning: hiding soft keyboard failed.");
                    }
                }
            } else {
                final ArrayList<Weather.WeatherLocation> stations = stationsManager.getStations();
                Location startLocation = stationSearchEngine.getCentroidLocationFromArea(station_description);
                if (startLocation!=null){
                    calcualateClosestStations(stations,startLocation);
                } else {
                    try {
                        Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getText(R.string.station_does_not_exist),Toast.LENGTH_LONG).show();
                    } catch (Exception e){
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error: station does not exist.");
                    }
                }
            }
        }
    };

    final AdapterView.OnItemLongClickListener weatherItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            startDetailsActivity(adapterView,view,i,l);
            return true;
        }
    };

    final AdapterView.OnItemClickListener weatherItemDoubleClickListener = new AdapterView.OnItemClickListener() {
        boolean isDoubleClick=false;
        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
            long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime<foreCastAdapterLastClickOnItem+ViewConfiguration.getDoubleTapTimeout()){
                isDoubleClick=true;
                onDoubleClick(adapterView, view, i, l);
            } else view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onSingleClick(adapterView, view, i, l);
                }
            }, Math.round(ViewConfiguration.getDoubleTapTimeout()*1.2f));
            foreCastAdapterLastClickOnItem = Calendar.getInstance().getTimeInMillis();
        }

        public void onSingleClick(AdapterView<?> adapterView, View view, int i, long l){
            if (!isDoubleClick){
              // do something
            }
            // reset for next use
            isDoubleClick = false;
        }

        public void onDoubleClick(AdapterView<?> adapterView, View view, int i, long l){
            startDetailsActivity(adapterView,view,i,l);
        }
    };

    final View.OnLongClickListener infoTextClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if (weatherCard!=null){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, String.valueOf(context.getResources().getString(R.string.issued)+": "+weatherCard.getHumanReadableIssueTime()), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return false;
        }
    };

    private void performTextSearch(){
    // weather settings must be read at the time of selection!
            WeatherSettings weatherSettings = new WeatherSettings(context);
            String station_description = autoCompleteTextView.getText().toString();
            Integer station_pos = stationsManager.getPositionFromDescription(station_description,true);
            if (station_pos!=null){
                if (!weatherSettings.station_name.equals(stationsManager.getName(station_pos))){
                    newWeatherRegionSelected(stationsManager.getLocationFromDescription(station_description));
                }
            } else {
                final ArrayList<Weather.WeatherLocation> stations = stationsManager.getStations();
                Location startLocation = stationSearchEngine.getCentroidLocationFromArea(station_description);
                if (startLocation!=null){
                    calcualateClosestStations(stations,startLocation);
                } else {
                    try {
                      Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getText(R.string.station_does_not_exist),Toast.LENGTH_LONG).show();
                    } catch (Exception e){
                      PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error: station does not exist.");
                    }
                }
            }
        }


    final View.OnClickListener searchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            performTextSearch();
        }
    };

    final View.OnKeyListener searchOnEnterListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if ((keyEvent.getAction()==KeyEvent.ACTION_DOWN) && (keyCode==KeyEvent.KEYCODE_ENTER)){
                performTextSearch();
                return true;
            } else {
                return false;
            }
        }
    };

    @Override
    protected void onPause(){
        cancelAnyOpenDialogs();
        unregisterReceiver(receiver);
        weatherLocationManager.stopGPSLocationSearch();
        PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"app paused.");
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putBoolean(SIS_ABOUT_DIALOG_STATE,aboutDiaglogVisible);
        savedInstanceState.putBoolean(SIS_WHATSNEW_DIALOG_STATE,whatsNewDialogVisible);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle restoreInstanceState){
        PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"restoring instance state.");
        aboutDiaglogVisible   = restoreInstanceState.getBoolean(SIS_ABOUT_DIALOG_STATE);
        whatsNewDialogVisible = restoreInstanceState.getBoolean(SIS_WHATSNEW_DIALOG_STATE);
        if (aboutDiaglogVisible){
            showAboutDialog();
        }
        if (whatsNewDialogVisible){
            showWhatsNewDialog();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume(){
        PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"app resumed.");
        registerForBroadcast();
        if (WeatherSettings.GPSAuto(context)){
            weatherLocationManager.checkLocation();
        } else {
            if (WeatherSettings.useBackgroundLocation(context)){
                if (WeatherLocationManager.checkForBackgroundLocation(context)){
                    WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.STATION);
                }
            }
        }
        final Context applicationContext = this;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                loadCurrentWeather();
                spinner.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                checkForBatteryOptimization(applicationContext);
                            }
                        });
                    }
                },6000);
            }
        });
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    loadStationsData();
                } catch (Exception e){
                    PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error loading stations data!");
                }
            }
        });
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.INFO,"app instance destroyed.");
        cancelAnyOpenDialogs();
        getApplication().unregisterActivityLifecycleCallbacks(weatherLocationManager);
    }

    private void cancelAnyOpenDialogs(){
        if (aboutDialog!=null){
            if (aboutDialog.isShowing()){
                aboutDialog.dismiss();
            }
        }
        if (whatsNewDialog != null){
            if (whatsNewDialog.isShowing()){
                whatsNewDialog.dismiss();
            }
        }
        if (hintPopupWindow!=null){
            if (hintPopupWindow.isShowing()){
                WeatherSettings.setHintCounter1(context,WeatherSettings.getHintCounter1(context)-1);
                hintPopupWindow.dismiss();
            }
        }
    }

    long launchTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (WeatherSettings.isFirstAppLaunch(this)){
            super.onCreate(savedInstanceState);
            context = getApplicationContext();
            // init Preference
            WeatherSettings weatherSettings = new WeatherSettings(context);
            weatherSettings.savePreferences();
            startWelcomeActivity();
            finish();
        } else {
            launchTimer = Calendar.getInstance().getTimeInMillis();
            ThemePicker.SetTheme(this);
            WeatherSettings.setRotationMode(this);
            super.onCreate(savedInstanceState);
            // check from intent if the WelcomeActivity tells us this is the first app launch
            Intent intent = getIntent();
            if (intent!=null){
                if (intent.hasExtra(WelcomeActivity.WA_EXTRA_ISFIRSTAPPLAUNCH)){
                    performingFirstAppLaunch = intent.getBooleanExtra(WelcomeActivity.WA_EXTRA_ISFIRSTAPPLAUNCH,false);
                }
            }
            context = getApplicationContext();
            thisActivity = this;
            executor = Executors.newSingleThreadExecutor();
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"Main activity started.");
            setContentView(R.layout.activity_main);
            weatherList = (ListView) findViewById(R.id.main_listview);
            stationsManager = new StationsManager(context);
            autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.actionbar_textview);
            // disable log to logcat if release is not a userdebug
            disableLogToLogcatIfNotUserDebug();
            // force a database access at the beginning to check for a needed database upgrade
            // debug code
            // WeatherWarnings.clearAllNotified(context);
            try {
                int v = WeatherContentManager.checkForDatabaseUpgrade(context);
            } catch (Exception e){
                PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error checking/upgrading database!");
            }
            // action bar layout
            // View actionBarView = getLayoutInflater().inflate(R.layout.actionbar,null);
            // actionBar.setCustomView(actionBarView);
            ActionBar actionBar = getActionBar();
            actionBar.setCustomView(R.layout.actionbar);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM|ActionBar.DISPLAY_SHOW_HOME);
            // anchor long click update
            try {
                // prepareAreaDatabase(context);
            } catch (Exception e){
                // ignore
            }
            // check for update procedures if not first install
            if ((WeatherSettings.getLastAppVersionCode(context) != BuildConfig.VERSION_CODE) &&
                    (WeatherSettings.getLastAppVersionCode(context)>WeatherSettings.PREF_LAST_VERSION_CODE_DEFAULT)){
                // remove old databases if previous version was older than 30
                if (WeatherSettings.getLastAppVersionCode(context)<30){
                    // remove abandoned forecast database file
                    if (deleteDatabase("weatherforecast.db")) {
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"remove abandoned forecast database file");
                    } else PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"no abandoned forecast database file to remove!");
                    // remove abandoned warnings database file
                    if (deleteDatabase("weatherwarnings.db")){
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"remove abandoned weather warnings database file");
                    } else PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"no abandoned weather warnings database file to remove!");
                    // remove abandoned texts database file
                    if (deleteDatabase("textforecasts.db")){
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"remove abandoned texts database file");
                    } else PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"no abandoned texts database file to remove!");
                    // remove abandoned areas database file
                    if (deleteDatabase("areas.db")){
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"remove abandoned areas database file");
                    } else PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"no abandoned areas database file to remove!");
                }
                // remove shared preferences on app update if installed app is lower than build 20
                if (WeatherSettings.getLastAppVersionCode(context)<20){
                    WeatherSettings.resetStationToDefault(getApplicationContext());
                    showWarning(R.mipmap.ic_warning_white_24dp,getResources().getString(R.string.warning_stationreset_title),getResources().getString(R.string.warning_stationreset_text));
                }
                // the station name K2226 has been changed to 10522, effective from 25.04.2022 9:00
                // the old name does not exist in stations4.txt any more.
                if (WeatherSettings.getLastAppVersionCode(context)<34){
                    Weather.WeatherLocation currentStation = WeatherSettings.getSetStationLocation(this);
                    if (currentStation.getName().equals("K2226")){
                        currentStation.setName("10522");
                        WeatherSettings.setStation(this,currentStation);
                    }
                }
                // fix possible (but very unlikely) unique notification ID in reserved area introduced in version 35
                if (WeatherSettings.getLastAppVersionCode(context)<35){
                    WeatherSettings.fixUniqueNotificationIdentifier(context);
                }
                // delete favorites and restore current station from settings, because the order of the flattened
                // strings has changed in the Weather.WeatherLocation class.
                if (WeatherSettings.getLastAppVersionCode(context)<46){
                    StationFavorites.deleteList(context);
                }
                showWhatsNewDialog();
                WeatherSettings.setCurrentAppVersionFlag(getApplicationContext());
            }
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        loadStationsSpinner();
                    }
                });
            } catch (Exception e){
                PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error loading StationSpinner!");
            }
            RelativeLayout infoTextLayout = (RelativeLayout) findViewById(R.id.main_infotext_layout);
            infoTextLayout.setOnLongClickListener(infoTextClickListener);
            // register the GPS methods
            // debug only WeatherSettings.saveGPSfixtime(context,0);
            weatherLocationManager = new WeatherLocationManager(context){
                @Override
                public void newLocation(Location location){
                    launchStationSearchByLocation(location);
                    super.newLocation(location);
                }
            };
            getApplication().registerActivityLifecycleCallbacks(weatherLocationManager);
            weatherLocationManager.setView((RelativeLayout) findViewById(R.id.gps_progress_holder));
            weatherLocationManager.registerCancelButton((Button) findViewById(R.id.cancel_gps));
            if (WeatherSettings.GPSAuto(context) && (!WeatherLocationManager.hasLocationPermission(context))){
                requestLocationPermission(PERMISSION_CALLBACK_LOCATION);
            }
            // test API
            if (API_TESTING_ENABLED){
                testAPI_Init();
            }
            // register view to clear favorites
            ImageView reset_favorites_imageview = (ImageView) findViewById(R.id.main_reset_favorites);
            if (reset_favorites_imageview!=null){
                reset_favorites_imageview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clearFavorites();
                        Toast.makeText(context,getApplicationContext().getResources().getString(R.string.favorites_cleared),Toast.LENGTH_LONG).show();
                    }
                });
            }
            // check if a geo intent was sent

            Location intentLocation = getLocationForGeoIntent(getIntent());
            if (intentLocation!=null){
                launchStationSearchByLocation(intentLocation);
            }
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"App launch finished.");
            popupHint();
            // create pollen area database
            if (!PollenArea.IsPollenAreaDatabaseComplete(context)){
                PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"Pollen areas database is empty or corrupt and needs to be fetched from the geoserver.");
                if (DataUpdateService.suitableNetworkAvailable(context)){
                    APIReaders.PollenAreaReader pollenAreaReader = new APIReaders.PollenAreaReader(context){
                        @Override
                        public void onFinished() {
                            // PollenArea pollenArea = PollenArea.FindPollenArea(context,weatherLocation);
                            // todo: refresh view for pollen
                            super.onFinished();
                        }
                    };
                    executor.execute(pollenAreaReader);
                } else {
                    PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"Cannot fetch pollen areas because no suitable network connection found.");
                }
            }
            // Prefetch maps for better performance. This may be turned off later by the user.
            // This will be done at a maximum of once per hour, see WeatherSettings.preFetchMaps.
            if (((performingFirstAppLaunch) || (WeatherSettings.preFetchMaps(context))) && DataUpdateService.suitableNetworkAvailable(context)){
                // read pollen data to be able to pre-generate pollen maps
                final APIReaders.PollenReader pollenReader = new APIReaders.PollenReader(context){
                    @Override
                    public void onFinished(boolean success) {
                        if (success) {
                            ArrayList<WeatherLayer> weatherLayers = WeatherLayer.getLayers(context);
                            final APIReaders.getLayerImages getLayerImages = new APIReaders.getLayerImages(context, weatherLayers) {
                                @Override
                                public void onStart() {
                                    super.onStart();
                                }

                                @Override
                                public void onProgress(WeatherLayer weatherLayer) {
                                    super.onProgress(weatherLayer);
                                    if (weatherLayer.isPollen()) {
                                        if (weatherLayer.isPollenLayerCacheFileOutdated(context)) {
                                            Bitmap bitmap = weatherLayer.getLayerBitmap(context);
                                            if (bitmap != null) {
                                                weatherLayer.saveLayerBitmapToCache(context, bitmap);
                                            }
                                        } else {
                                            // do nothing
                                        }
                                    }
                                }

                                @Override
                                public void onFinished(boolean success) {
                                    if (success) {
                                        WeatherSettings.setPrefetchMapsTime(context);
                                    }
                                }
                            };
                            getLayerImages.run();
                        }
                    }
                };
                weatherList.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        executor.execute(pollenReader);
                    }
                },15000);
            } else {
                // do nothing
            }
            // TESTING
        }
    }

    /**
     * Returns a location for a geo intent.
     * <p>
     * Checks if an intent's scheme is "geo". If so, and the intent data is following the pattern
     * "latitude,longitude[optional parameters with non-numeric separator]", the content is used to populate a new location
     * object. If it's a geo intent, but it's not following the mentioned pattern, the user is
     * informed about the mismatch via a toast and a log message is written.
     *
     * @param intent the intent to analyze
     * @return a new Location object with latitude/longitude from the intent or null if the intent is not a valid geo scheme intent.
     */
    private Location getLocationForGeoIntent(Intent intent) {
        if (intent==null)
        {
            return null;
        }
        String intent_action = intent.getAction();

        if (!Objects.equals(intent_action, Intent.ACTION_VIEW))
        {
            return null;
        }

        String intent_scheme = intent.getScheme();
        if (intent_scheme==null || !intent_scheme.equalsIgnoreCase("geo"))
        {
            return null;
        }

        String received_geolocation = intent.getData().toString();
        if (received_geolocation!=null) {
            try {
                Pattern pattern_lat_long = Pattern.compile("geo:(?<latitude>-?[\\d]*\\.?[\\d]*),(?<longitude>-?[\\d]*\\.?[\\d]*)", Pattern.CASE_INSENSITIVE);
                Matcher m = pattern_lat_long.matcher(received_geolocation);
                m.find();
                String received_latitude = m.group(1); //Can be replaced with 'm.group("latitude")' for better readability as soon as min API level is 26
                String received_longitude = m.group(2); //Can be replaced with 'm.group("longitude")' for better readability as soon as min API level is 26

                Location own_location = new Location("manual");
                own_location.setTime(Calendar.getInstance().getTimeInMillis());
                double latitude = Location.convert(standardizeGeo(received_latitude));
                double longitude = Location.convert(standardizeGeo(received_longitude));
                own_location.setLatitude(latitude);
                own_location.setLongitude(longitude);
                return own_location;
                }
            catch (Exception e) {
                // invalid geo-string (uri)
                PrivateLog.log(getApplicationContext(), PrivateLog.MAIN, PrivateLog.ERR, "received geo intent, but unable to read it: " + e.getMessage());
                PrivateLog.log(getApplicationContext(), PrivateLog.MAIN, PrivateLog.ERR, "geo content was: " + received_geolocation);
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.georeceive_error), Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }

    private void newWeatherRegionSelected(final Weather.WeatherLocation weatherLocation){
        if (!weatherLocation.getName().equals(WeatherSettings.getSetStationLocation(context).getName())){
            final Context context = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getText(R.string.new_station)+" "+weatherLocation.getDescription(context),Toast.LENGTH_LONG).show();
                    } catch (Exception e){
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.WARN,"Warning: new station message failed.");
                    }
                }
            });
            // invalidate current weather data
            weatherCard = null; localWarnings = null;
            WeatherSettings.setStation(this,weatherLocation);
            WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.STATION);
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"-----------------------------------");
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"New sensor: "+weatherLocation.getDescription(context));
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"-----------------------------------");
            last_updateweathercall = Calendar.getInstance().getTimeInMillis();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    addToSpinner(weatherLocation);
                    if (autoCompleteTextView!=null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    autoCompleteTextView.setText("");
                                    autoCompleteTextView.clearListSelection();
                                } catch (Exception e) {
                                    PrivateLog.log(context, PrivateLog.MAIN, PrivateLog.ERR, "unable to clear autoCompleteTextView");
                                }
                            }
                        });
                    }
                    loadCurrentWeather();
                }
            });
        }
    }

    public static class SpinnerListener implements View.OnTouchListener, AdapterView.OnItemSelectedListener{
        private boolean user_touched_spinner = false;
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            user_touched_spinner = true;
            return false;
        }
        public void handleItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            // to the stuff
            user_touched_spinner = false;
        }
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (user_touched_spinner){
                handleItemSelected(adapterView, view,  i, l);
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }


    private void loadStationsSpinner() {
        // spinner code
        spinner = (Spinner) findViewById(R.id.stations_spinner);
        if (stationFavorites==null){
            stationFavorites = new StationFavorites(context);
        }
        // check if alternate description exists, and find it if not.
        final ArrayList<Weather.WeatherLocation> spinnerItems = StationFavorites.getFavorites(context);
        final ArrayList<String> spinnerDescriptions = Weather.WeatherLocation.getDescriptions(context,spinnerItems);
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_spinner_item, spinnerDescriptions);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.custom_spinner_item);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setAdapter(spinnerArrayAdapter);
            }
        });
        final Context context = this;
        // for the spinner
        final SpinnerListener spinnerListener = new SpinnerListener() {
            @Override
            public void handleItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                // weather settings must be read at the time of selection!
                WeatherSettings weatherSettings = new WeatherSettings(context);
                TextView tv = (TextView) view.findViewById(R.id.spinner_textitem);
                String station_description = tv.getText().toString();
                String stationOriginalDescription = null;
                for (int i=0; i<spinnerDescriptions.size(); i++){
                    if (spinnerDescriptions.get(i).equals(station_description)){
                        stationOriginalDescription = spinnerItems.get(i).getOriginalDescription();
                    }
                }
                if (stationOriginalDescription!=null){
                    Integer station_pos = stationsManager.getPositionFromDescription(stationOriginalDescription);
                    if (station_pos != null) {
                        if (!weatherSettings.station_name.equals(stationsManager.getName(station_pos))) {
                            newWeatherRegionSelected(stationsManager.getLocationFromDescription(stationOriginalDescription));
                        }
                    } else {
                        PrivateLog.log(context, PrivateLog.MAIN,PrivateLog.WARN, "Station from favorites not found! (1)");
                        loadStationsSpinner();
                    }
                } else {
                    PrivateLog.log(context, PrivateLog.MAIN,PrivateLog.WARN, "Station from favorites not found! (2)");
                    loadStationsSpinner();
                }
                super.handleItemSelected(adapterView, view, pos, l);
            }
        };
        spinner.setOnItemSelectedListener(spinnerListener);
        spinner.setOnTouchListener(spinnerListener);
    }

    private void addToSpinner(Weather.WeatherLocation weatherLocation){
        if (stationFavorites==null){
            stationFavorites = new StationFavorites(context);
        }
        if (!weatherLocation.hasAlternateDescription()){
            String newDescriptionAlternate = WeatherLocationManager.getDescriptionAlternate(context,weatherLocation);
            if (newDescriptionAlternate!=null){
                weatherLocation.setDescriptionAlternate(newDescriptionAlternate);
                WeatherSettings.setDescriptionAlternate(context,newDescriptionAlternate);
            }
        }
        stationFavorites.addFavorite(weatherLocation);
        loadStationsSpinner();
    }

    private void clearFavorites(){
        StationFavorites.deleteList(context);
        loadStationsSpinner();
    }

    public void loadStationsData(){
        StationsManager.StationsReader stationsReader = new StationsManager.StationsReader(getApplicationContext()) {
            @Override
            public void onLoadingListFinished(ArrayList<Weather.WeatherLocation> stations) {
                super.onLoadingListFinished(stations);
                if (stationsManager == null){
                    stationsManager = new StationsManager(context);
                }
                stationsManager.stations = stations;
                stationSearchEngine = new StationSearchEngine(context,executor,null,stationsManager){
                    @Override
                    public void newEntries(ArrayList<String> newEntries){
                        super.newEntries(newEntries);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getBaseContext(), R.layout.custom_dropdown_item, entries);
                                if (autoCompleteTextView==null){
                                    autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.actionbar_textview);
                                }
                                autoCompleteTextView.setAdapter(stringArrayAdapter);
                                autoCompleteTextView.setCompletionHint(context.getResources().getString(R.string.actionbar_textinput_hint));
                                autoCompleteTextView.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
                                autoCompleteTextView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                                autoCompleteTextView.setOnItemClickListener(clickListener);
                                // anchor search icon to search
                                ImageView search_icon = (ImageView) findViewById(R.id.actionbar_search_icon);
                                search_icon.setOnClickListener(searchListener);
                                // set listener to search field
                                autoCompleteTextView.setOnKeyListener(searchOnEnterListener);
                            }
                        });
                    }
                };
            }
        };
        executor.execute(stationsReader);
    }

    private void testAPI_Worker(){
        if (test_position<stationsManager.getStationCount()){
            final WeatherSettings weatherSettings = new WeatherSettings(this);
            weatherSettings.station_name = stationsManager.getName(test_position);
            weatherSettings.applyPreference(WeatherSettings.PREF_STATION_NAME,weatherSettings.station_name);
            final String name = stationsManager.getName(test_position);
            final String description = stationsManager.getDescription(test_position);
            Handler handler = new Handler();
            //Log.v(Tag.MAIN,"Waiting.");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                   //Log.v(Tag.MAIN,"------------------------------------------");
                   //Log.v(Tag.MAIN,"Testing station # "+test_position+" named "+name+ " described as "+description);
                   //Log.v(Tag.MAIN,"-------------------------------------------");
                    weatherCard = Weather.getCurrentWeatherInfo(context,UpdateAlarmManager.UPDATE_FROM_ACTIVITY);
                    if ((weatherCard == null) || (API_TESTING_ENABLED)){
                        ArrayList<String> updateTasks = new ArrayList<String>();
                        updateTasks.add(DataUpdateService.SERVICEEXTRAS_UPDATE_WEATHER);
                        UpdateAlarmManager.updateAndSetAlarmsIfAppropriate(getApplicationContext(), UpdateAlarmManager.UPDATE_FROM_ACTIVITY,updateTasks,weatherCard);
                    } else {
                        displayWeatherForecast();
                    }
                }
            },4000);
        } else {
             //Log.v(Tag.MAIN,"Testing finished.");
        }
    }

    private void testAPI_Call(){
        final Context context = this;
        registerForBroadcast();
        if (stationsManager.getStationCount()==0){
            StationsManager.StationsReader stationsReader = new StationsManager.StationsReader(context) {
                @Override
                public void onLoadingListFinished(ArrayList<Weather.WeatherLocation> stations) {
                    super.onLoadingListFinished(stations);
                    testAPI_Worker();
                }
            };
            executor.execute(stationsReader);
        } else {
            testAPI_Worker();
        }
    }

    private void testAPI_Init(){
        // reset preferences
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();
        // set start position
        final WeatherSettings weatherSettings = new WeatherSettings(this);
        // disable gadgetbridge support for testing und set start position in settings
        weatherSettings.serve_gadgetbridge = false;
        weatherSettings.station_name = stationsManager.getName(test_position);
        weatherSettings.applyPreference(WeatherSettings.PREF_SERVE_GADGETBRIDGE,false);
        weatherSettings.applyPreference(WeatherSettings.PREF_STATION_NAME,weatherSettings.station_name);
        testAPI_Call();
    }

    public void displayUpdateTime(final CurrentWeatherInfo currentWeatherInfo){
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd.MM.yyyy, HH:mm:ss");
        String updatetime = simpleDateFormat.format(new Date(currentWeatherInfo.polling_time));
        TextView textView_update_time = (TextView) findViewById(R.id.main_update_time);
        textView_update_time.setText(getApplicationContext().getResources().getString(R.string.main_updatetime)+" "+updatetime);
        TextView textView_station_geo = (TextView) findViewById(R.id.main_station_geo);
        if (WeatherSettings.displayStationGeo(context)){
            try {
                Weather.WeatherLocation stationLocation = WeatherSettings.getSetStationLocation(context);
                String stationInfoString = getApplicationContext().getResources().getString(R.string.station)+
                        " Lat.: "+new DecimalFormat("0.00").format(stationLocation.latitude)+
                        " Long.: "+new DecimalFormat("0.00").format(stationLocation.longitude)+
                        " Alt.: "+new DecimalFormat("0.00").format(stationLocation.altitude);
                if (stationLocation.type==RawWeatherInfo.Source.DMO){
                    stationInfoString=stationInfoString+ " (DMO)";
                }
                textView_station_geo.setText(stationInfoString);
            } catch (Exception e){
                PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.ERR,"parsing geo coordinates of station failed: "+e.getMessage());
                textView_station_geo.setText("-");
            }
        } else {
            textView_station_geo.setVisibility(View.GONE);
            textView_station_geo.invalidate();
        }
    }

    private void displayOverviewChart(final CurrentWeatherInfo currentWeatherInfo){
        final ImageView overviewChartImageView = (ImageView) findViewById(R.id.main_overview_chart);
        final LinearLayout main_leftcontainer = (LinearLayout) findViewById(R.id.main_leftcontainer);
        final boolean isLandscape = (main_leftcontainer!=null);
        if (overviewChartImageView!=null){
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            if (WeatherSettings.displayOverviewChart(context)){
                overviewChartImageView.setVisibility(View.VISIBLE);
            }
            if (!isLandscape){
                if (WeatherSettings.displayOverviewChart(context)){
                    Bitmap overViewChartBitmap = ForecastBitmap.getOverviewChart(context,displayMetrics.widthPixels,displayMetrics.heightPixels/10,currentWeatherInfo.forecast1hourly,localWarnings);
                    if (overViewChartBitmap!=null){
                        overviewChartImageView.setImageBitmap(overViewChartBitmap);
                    }
                } else {
                    overviewChartImageView.setVisibility(View.GONE);
                }
            } else {
                LinearLayout main_landscape_mainlinearlayout = (LinearLayout) findViewById(R.id.main_landscape_mainlinearlayout);
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        overviewChartImageView.measure(0,0);
                        final Bitmap overViewChartBitmap = ForecastBitmap.getOverviewChart(context,overviewChartImageView.getWidth(),overviewChartImageView.getHeight(),currentWeatherInfo.forecast1hourly,localWarnings);
                        if (overViewChartBitmap!=null){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    overviewChartImageView.setImageBitmap(overViewChartBitmap);
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    private int get24passedPosition(CurrentWeatherInfo currentWeatherInfo,int lasthourlypostion){
        if ((currentWeatherInfo.forecast1hourly.size()>lasthourlypostion) && (lasthourlypostion>0)){
            long last1hourtime = currentWeatherInfo.forecast1hourly.get(lasthourlypostion).getTimestamp();
            if (currentWeatherInfo.forecast6hourly.size()>0){
                int position = 0;
                long time = currentWeatherInfo.forecast6hourly.get(position).getTimestamp();
                while (position<currentWeatherInfo.forecast6hourly.size() && time<last1hourtime){
                    position++;
                    time = currentWeatherInfo.forecast6hourly.get(position).getTimestamp();
                }
                return position;
            }
        }
        return 0;
    }

    private ArrayList<Weather.WeatherInfo> getCustomForecastWeatherInfoArray(CurrentWeatherInfo weatherCard){
        WeatherSettings weatherSettings = new WeatherSettings(getApplicationContext());
        if (weatherSettings.getDisplayType() == WeatherSettings.DISPLAYTYPE_24HOURS){
            return weatherCard.forecast24hourly;
        }
        if (weatherSettings.getDisplayType() == WeatherSettings.DISPLAYTYPE_6HOURS){
            return weatherCard.forecast6hourly;
        }
        if (weatherSettings.getDisplayType() == WeatherSettings.DISPLAYTYPE_1HOUR){
            return weatherCard.forecast1hourly;
        }
        ArrayList<Weather.WeatherInfo> weatherInfos = new ArrayList<Weather.WeatherInfo>();
        for (int i=0; i<24 && i<weatherCard.forecast1hourly.size(); i++){
            weatherInfos.add(weatherCard.forecast1hourly.get(i));
        }
        for (int i=get24passedPosition(weatherCard,weatherInfos.size()-1); i<weatherCard.forecast6hourly.size(); i++){
            weatherInfos.add(weatherCard.forecast6hourly.get(i));
        }
        return weatherInfos;
    }

    private void loadCurrentWeather(){
        if ((WeatherSettings.hasWeatherUpdatedFlag(context,WeatherSettings.UpdateType.STATION)) ||
                (WeatherSettings.hasWeatherUpdatedFlag(context,WeatherSettings.UpdateType.DATA))){
            // in case of new station add it to the spinner list and update the spinner display
            if (WeatherSettings.hasWeatherUpdatedFlag(context,WeatherSettings.UpdateType.STATION)){
                addToSpinner(WeatherSettings.getSetStationLocation(context));
            }
            // force a re-load of weather data using the station from settings
            weatherCard = null;
            // set back the flag as update was done.
            WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.NONE);
        }
        if (WeatherSettings.hasWeatherUpdatedFlag(context,WeatherSettings.UpdateType.VIEWS)){
            // set back the flag before recreate occurs
            WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.NONE);
            // notify widgets (& Gadgetbridge), since such view changes may also affect widgets, e.g. overview chart
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    UpdateAlarmManager.updateAppViews(context,weatherCard);
                }
            });
            // recreate the whole view
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recreate();
                }
            });
            return;
        }
        // trigger one update unconditionally here
        executor.execute(new Runnable() {
            @Override
            public void run() {
                UpdateAlarmManager.updateAndSetAlarmsIfAppropriate(context,UpdateAlarmManager.UPDATE_FROM_ACTIVITY,null,null);
            }
        });
        // display weather without any update mode active, but only if adapter is empty
        displayWeatherForecast();
    }

    public void displayWeatherForecast() {
        boolean dataChanged = false;
        // check if weather forecast in memory is outdated, fetch from database if necessary
        if (weatherCard==null){
            weatherCard = Weather.getCurrentWeatherInfo(context,UpdateAlarmManager.UPDATE_FROM_ACTIVITY);
            dataChanged = true;
        } else {
            if (Weather.getPollingTime(context) > weatherCard.polling_time) {
                weatherCard = Weather.getCurrentWeatherInfo(context,UpdateAlarmManager.UPDATE_FROM_ACTIVITY);
                dataChanged = true;
            }
        }
        boolean reloadWarningsFromDatabase = false;
        // re-load warnings if warnings null
        if (localWarnings==null){
            reloadWarningsFromDatabase = true;
        } else {
            // local warnings not null
            // it might be necessary to reload warnings if they were updated while app was in background
            if (localWarnings.size()>0){
                // no adapter OR adapter creation older than warnings polling_time
                if ((forecastAdapter==null) || (forecastAdapter.creationTime<localWarnings.get(0).polling_time)){
                    reloadWarningsFromDatabase = true;
                }
            }
        }
        // reload warnings and display adapter after re-load
        if (reloadWarningsFromDatabase){
            WeatherWarnings.getWarningsForLocationRunnable getWarningsForLocationRunnable = new WeatherWarnings.getWarningsForLocationRunnable(context,null,null){
                @Override
                public void onResult(final ArrayList<WeatherWarning> result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            localWarnings = result;
                            displayAdapter(weatherCard);
                        }
                    });

                }
            };
            executor.execute(getWarningsForLocationRunnable);
        } else {
            // no need to re-load warnings, check if we need to display the adapter
            // refresh adapter if null OR data changed OR adapter older than weather data
            if ((forecastAdapter==null) || (dataChanged) || (forecastAdapter.creationTime<weatherCard.polling_time)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayAdapter(weatherCard);
                    }
                });
            }
        }

    }

    private void displayAdapter(final CurrentWeatherInfo weatherCard){
        if (weatherCard!=null){
            displayUpdateTime(weatherCard);
            forecastAdapter = new ForecastAdapter(getApplicationContext(),getCustomForecastWeatherInfoArray(weatherCard),weatherCard.forecast1hourly,weatherCard.weatherLocation);
            forecastAdapter.setWarnings(localWarnings);
            // weatherList.setFastScrollEnabled(true);
            weatherList.setAdapter(forecastAdapter);
            forecastAdapter.notifyDataSetChanged();
            if (WeatherSettings.loggingEnabled(this)){
                float time = (Calendar.getInstance().getTimeInMillis()-launchTimer)/1000f;
                DecimalFormat decimalFormat = new DecimalFormat("000.000");
                PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"Timer: adapter set "+decimalFormat.format(time)+" sec from app launch.");
            }
            weatherList.setOnItemLongClickListener(weatherItemLongClickListener);
            weatherList.setOnItemClickListener(weatherItemDoubleClickListener);
            // display overview chart after adapter and queue it
            weatherList.post(new Runnable() {
                @Override
                public void run() {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    displayOverviewChart(weatherCard);
                                }
                            });
                        }
                    });
                }
            });
        }
   }

    public void forcedWeatherUpdate(){
        ArrayList<String> updateTasks = new ArrayList<String>();
        updateTasks.add(DataUpdateService.SERVICEEXTRAS_UPDATE_WEATHER);
        updateTasks.add(DataUpdateService.SERVICEEXTRAS_UPDATE_WARNINGS);
        UpdateAlarmManager.updateAndSetAlarmsIfAppropriate(getApplicationContext(), UpdateAlarmManager.UPDATE_FROM_ACTIVITY,updateTasks,weatherCard);
        forceWeatherUpdateFlag = true;
    }

    public static int getColorFromResource(Context context, int id){
        TypedValue typedValue = new TypedValue();
        context.getApplicationContext().getTheme().resolveAttribute(id,typedValue,true);
        return typedValue.data;
    }

    @SuppressWarnings("deprecation")
    public static void setOverflowMenuItemColor(Context context, Menu menu, int id, int string_id){
        String s = context.getApplicationContext().getResources().getString(string_id);
        MenuItem menuItem = menu.findItem(id);
        if (menuItem.isVisible()){
            SpannableString spannableString = new SpannableString(s);
            int color = ThemePicker.getWidgetTextColor(context);
            spannableString.setSpan(new ForegroundColorSpan(color),0,s.length(),0);
            menuItem.setTitle(spannableString);
        } else {
            // do nothing
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.main_activity,menu);
        if (!WeatherSettings.forceNoMenuIcons(getApplicationContext())){
            // try to show icons in drop-down menu
            if (menu.getClass().getSimpleName().equals("MenuBuilder")){
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible",Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu,true);
                } catch (Exception e){
                    // this is a hack to disable the icon-in-menu-feature permanently if it fails on some devices.
                    // A flag is set in the settings and the app is force-restarted with an intent.
                    // this should only happen once at the first app launch, if ever.
                    PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.FATAL,"The icon-in-menu feature failed.");
                    PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.FATAL,"DISABLING this feature permanently!");
                    PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.FATAL,"=> This needs an app relaunch, this is being triggered now.");
                    WeatherSettings.setForceNoMenuIconsFlag(getApplicationContext(),true);
                    Intent i = new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(i);
                    finish();
                }
            }
            // omit the first two, as they are not in the menu
            //setOverflowMenuItemColor(menu,R.id.menu_refresh,R.string.warnings_update, R.attr.colorText);
            //setOverflowMenuItemColor(menu,R.id.menu_warnings,R.string.warnings_button, R.attr.colorText);
            setOverflowMenuItemColor(this,menu,R.id.menu_maps,R.string.wm_maps);
            setOverflowMenuItemColor(this,menu,R.id.menu_texts,R.string.texts_button);
            setOverflowMenuItemColor(this,menu,R.id.menu_settings,R.string.settings_button);
            setOverflowMenuItemColor(this,menu,R.id.menu_geoinput,R.string.geoinput_button);
            setOverflowMenuItemColor(this,menu,R.id.menu_travelupdate,R.string.travel_button);
            setOverflowMenuItemColor(this,menu,R.id.menu_about,R.string.about_button);
            setOverflowMenuItemColor(this,menu,R.id.menu_license,R.string.license_button);
            setOverflowMenuItemColor(this,menu,R.id.menu_whatsnew,R.string.whatsnew_button);
        }
        // disable weather warnings if desired by user
        if (WeatherSettings.areWarningsDisabled(context)){
            for (int i=0; i<menu.size(); i++){
                if (menu.getItem(i).getItemId()==R.id.menu_warnings){
                    menu.getItem(i).setVisible(false);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureID, Menu menu){
        return super.onMenuOpened(featureID,menu);
    }


    @Override
    public void onOptionsMenuClosed(Menu menu){
        super.onOptionsMenuClosed(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi){
        int item_id = mi.getItemId();
        if (item_id == R.id.menu_refresh){
            PrivateLog.log(this,PrivateLog.MAIN,PrivateLog.INFO,"user requests update => force update");
            forcedWeatherUpdate();
            return true;
        }
        if (item_id == R.id.menu_warnings) {
            Intent i = new Intent(this, WeatherWarningActivity.class);
            startActivity(i);
            return true;
        }
        if (item_id == R.id.menu_settings) {
            Intent i = new Intent(this, Settings.class);
            startActivity(i);
            return true;
        }
        if (item_id == R.id.menu_geoinput) {
            startGeoinput();
            return true;
        }
        if (item_id == R.id.menu_travelupdate) {
            ArrayList<String> updateTasks = new ArrayList<String>();
            updateTasks.add(DataUpdateService.SERVICEEXTRAS_UPDATE_LOCATIONSLIST);
            UpdateAlarmManager.updateAndSetAlarmsIfAppropriate(context, UpdateAlarmManager.UPDATE_FROM_ACTIVITY,updateTasks,weatherCard);
            return true;
        }
        if (item_id==R.id.menu_license) {
            Intent i = new Intent(this, LicenseInfo.class);
            i.putExtra(LicenseInfo.DATA_TITLE, getResources().getString(R.string.license_title));
            i.putExtra(LicenseInfo.DATA_TEXTRESOURCE, "license");
            i.putExtra(LicenseInfo.DATA_BUTTONTEXT,getResources().getString(R.string.button_continue));
            startActivity(i);
            return true;
        }
        if (item_id == R.id.menu_about) {
            showAboutDialog();
            return true;
        }
        if (item_id == R.id.menu_whatsnew) {
            showWhatsNewDialog();
            return true;
        }
        if (item_id == R.id.menu_texts) {
            Intent i = new Intent(this, TextForecastListActivity.class);
            startActivity(i);
        }
        if (item_id == R.id.menu_maps) {
            Intent i = new Intent(this, WeatherLayersActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(mi);
    }

    public void showAboutDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        builder.setCancelable(true);
        LayoutInflater layoutInflater = this.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.aboutdialog,null,false);
        builder.setView(view);
        builder.setTitle(getResources().getString(R.string.app_name));
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_INFO_OUTLINE,false));
        builder.setIcon(drawable);
        builder.setNeutralButton(getApplicationContext().getResources().getString(R.string.alertdialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                aboutDiaglogVisible=false;
                dialogInterface.dismiss();
            }
        });
        aboutDialog = builder.create();
        aboutDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        aboutDialog.show();
        ThemePicker.tintAlertDialogButtons(context,aboutDialog);
        aboutDiaglogVisible=true;
        String versioning = BuildConfig.VERSION_NAME + " (build "+BuildConfig.VERSION_CODE+")";
        TextView textView = (TextView) aboutDialog.findViewById(R.id.about_textview);
        String textfile = "about";
        InputStream inputStream = getResources().openRawResource(getResources().getIdentifier(textfile,"raw",getApplicationContext().getPackageName()));
        try {
            int size = inputStream.available();
            byte[] textdata = new byte[size];
            inputStream.read(textdata);
            inputStream.close();
            String text = new String(textdata);
            text = text.replace("[VERSION]",versioning);
            if (WeatherSettings.appReleaseIsUserdebug()){
                text = text + PrivateLog.getDebugInfoString(getApplicationContext());
                text = text + PrivateLog.getInfoString(getApplicationContext());
                text = text + PrivateLog.getCurrentStationInfoString(getApplicationContext());
            }
            textView.setText(text);
        } catch (IOException e) {
            textView.setText("Error: "+e.getMessage());
            PrivateLog.log(getApplicationContext(),PrivateLog.MAIN, PrivateLog.ERR,"Showing the about dialog failed.");
        }
    }

    private SpannableStringBuilder readTextFileFromResources(String textfile){
        try {
            InputStream inputStream = getResources().openRawResource(getResources().getIdentifier(textfile,"raw",getApplicationContext().getPackageName()));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
            while ((line=bufferedReader.readLine())!=null){
                line = line.replace("[VERSION]",BuildConfig.VERSION_NAME);
                if (line.length()>0){
                    if (line.charAt(0) == '-'){
                        if (line.length()>1){
                            line=line.substring(2);
                        }
                        int start = spannableStringBuilder.length();
                        spannableStringBuilder.append(line).append("\n");
                        int end = spannableStringBuilder.length();
                        BulletSpan bulletSpan = new BulletSpan(20);
                        if (Build.VERSION.SDK_INT>28){
                            bulletSpan = new BulletSpan(20, ThemePicker.getColor(getApplicationContext(),ThemePicker.ThemeColor.TEXTLIGHT),10);
                        }
                        spannableStringBuilder.setSpan(bulletSpan,start,end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannableStringBuilder.setSpan(new ForegroundColorSpan(ThemePicker.getColor(getApplicationContext(),ThemePicker.ThemeColor.TEXTLIGHT)),start,end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        spannableStringBuilder.append(line).append("\n");
                    }
                } else {
                    spannableStringBuilder.append("\n");
                }
            }
            bufferedReader.close();
            inputStream.close();
            return spannableStringBuilder;
        } catch (Exception e){
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error: "+e.getMessage());
            return null;
        }
    }

    public void showWhatsNewDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        builder.setCancelable(true);
        LayoutInflater layoutInflater = this.getLayoutInflater();
        final View view = layoutInflater.inflate(R.layout.whatsnewdialog,null,false);
        builder.setView(view);
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_ANNOUNCEMENT,false));
        builder.setIcon(drawable);
        builder.setTitle(getResources().getString(R.string.app_name));
        builder.setNeutralButton(getApplicationContext().getResources().getString(R.string.alertdialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                whatsNewDialogVisible=false;
                // update version code in preferences so that this dialog is not shown anymore in this version
                final WeatherSettings weatherSettings = new WeatherSettings(getApplicationContext());
                weatherSettings.applyPreference(WeatherSettings.PREF_LAST_VERSION_CODE,BuildConfig.VERSION_CODE);
                dialogInterface.dismiss();
            }
        });
        whatsNewDialog = builder.create();
        whatsNewDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        whatsNewDialog.show();
        ThemePicker.tintAlertDialogButtons(context,whatsNewDialog);
        whatsNewDialogVisible=true;
        TextView textView = (TextView) whatsNewDialog.findViewById(R.id.whatsnew_textview);
        textView.setText(readTextFileFromResources("whatsnew"));
    }

    public static void deleteAreaDatabase(Context context){
        PrivateLog.log(context.getApplicationContext(),PrivateLog.MAIN, PrivateLog.WARN,"Area database has been cleared.");
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(WeatherContentManager.AREA_URI_ALL,null,null);
    }

    RelativeLayout areaProgress_relativeLayout;
    ProgressBar areaProgress_progressBar;
    TextView areaProgress_progressTextView;

    private void showAreaDatabaseProgress(int progress, String text){
        String dataText = getApplicationContext().getString(R.string.areadatabasecreator_title);
        if ((areaProgress_relativeLayout==null) || (areaProgress_progressBar==null) || (areaProgress_progressTextView==null)) {
            areaProgress_relativeLayout = (RelativeLayout) findViewById(R.id.main_area_progress_holder);
            areaProgress_progressBar = (ProgressBar) findViewById(R.id.main_area_progress_bar);
            areaProgress_progressTextView = (TextView) findViewById(R.id.main_area_progress_text);
            // Lock screen rotation during database processing to prevent activity being destroyed
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            areaProgress_relativeLayout.setVisibility(View.VISIBLE);
        }
        areaProgress_progressBar.setProgress(progress);
        areaProgress_progressTextView.setText(dataText+": "+ text);
    }

    private void hideAreaDatabaseProgress(){
        if (areaProgress_relativeLayout==null){
            areaProgress_relativeLayout = (RelativeLayout) findViewById(R.id.main_area_progress_holder);
        }
        // Allow screen rotation within this app again
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        areaProgress_relativeLayout.setVisibility(View.GONE);
        // release views as indicator that no database creation is running
        areaProgress_relativeLayout = null; areaProgress_progressBar = null; areaProgress_progressTextView = null;
    }

    public static boolean prepareAreaDatabase(final Context context){
        if (WeatherSettings.areWarningsDisabled(context)){
            deleteAreaDatabase(context);
            PrivateLog.log(context.getApplicationContext(),PrivateLog.MAIN, PrivateLog.WARN,"Area database will not be prepared because weather warnings are disabled.");
            return false;
        }
        // delete area database if it is too big due to some database errors. It will be
        // recreated then.
        if (Areas.getAreaDatabaseSize(context.getApplicationContext())>Areas.AreaDatabaseCreator.DATABASE_SIZE){
            PrivateLog.log(context.getApplicationContext(),PrivateLog.MAIN, PrivateLog.ERR,"Area database size is: "+Areas.getAreaDatabaseSize(context.getApplicationContext())+", expected: "+Areas.AreaDatabaseCreator.DATABASE_SIZE);
            PrivateLog.log(context.getApplicationContext(),PrivateLog.MAIN, PrivateLog.ERR,"Area database will be cleared...");
            deleteAreaDatabase(context.getApplicationContext());
        }
        // update area database if:
        // a) database does not exist
        // b) if sql database is outdated
        if ((!Areas.doesAreaDatabaseExist(context.getApplicationContext())) || (!Areas.AreaDatabaseCreator.areAreasUpToDate(context.getApplicationContext()))) {
            return true;
        }
        return false;
    }

    private void showWarning(int icon, String title, String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        builder.setIcon(icon);
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setNeutralButton(getApplicationContext().getResources().getString(R.string.alertdialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        alertDialog.show();
        ThemePicker.tintAlertDialogButtons(context,alertDialog);
    }

    public static void askDialog(Context context, Integer icon, String title, String[] text,  DialogInterface.OnClickListener positiveListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context,0);
        if (icon!=null){
            builder.setIcon(icon);
        }
        if (title!=null){
            builder.setTitle(title);
        }
        if (text!=null){
            StringBuilder stringBuilder= new StringBuilder();
            for (int i=0; i<text.length; i++){
                stringBuilder.append(text[i]);
                stringBuilder.append(System.getProperty("line.separator"));
            }
            builder.setMessage(stringBuilder.toString());
        }
        if (positiveListener!=null){
            builder.setPositiveButton(context.getResources().getString(R.string.alertdialog_yes), positiveListener);
            builder.setNegativeButton(context.getResources().getString(R.string.alertdialog_no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        } else {
            builder.setNeutralButton(context.getResources().getString(R.string.alertdialog_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        alertDialog.show();
        ThemePicker.tintAlertDialogButtons(context,alertDialog);
    }

    private void registerForBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(MAINAPP_CUSTOM_REFRESH_ACTION);
        filter.addAction(MAINAPP_SSL_ERROR);
        filter.addAction(MAINAPP_SHOW_PROGRESS);
        filter.addAction(MAINAPP_HIDE_PROGRESS);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(WeatherWarningActivity.WEATHER_WARNINGS_UPDATE);
        filter.addAction(MAINAPP_AREADB_READY);
        filter.addAction(MAINAPP_AREADB_PROGRESS);
        registerReceiver(receiver,filter);
    }

    private void disableLogToLogcatIfNotUserDebug(){
        WeatherSettings weatherSettings = new WeatherSettings(getApplicationContext());
        if (!WeatherSettings.appReleaseIsUserdebug()){
            if (weatherSettings.log_to_logcat){
                AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
                builder.setIcon(R.mipmap.ic_warning_white_24dp);
                builder.setTitle(getApplicationContext().getResources().getString(R.string.alertdialog_2_title));
                builder.setMessage(getApplicationContext().getResources().getString(R.string.alertdialog_2_text));
                final Context context = getApplicationContext();
                builder.setNeutralButton(getApplicationContext().getResources().getString(R.string.alertdialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"Logging to logcat is being disabled...");
                        WeatherSettings weatherSettings = new WeatherSettings(context);
                        weatherSettings.applyPreference(WeatherSettings.PREF_LOG_TO_LOGCAT,WeatherSettings.PREF_LOG_TO_LOGCAT_DEFAULT);
                        Toast.makeText(context,context.getResources().getString(R.string.alertdialog_2_toast),Toast.LENGTH_LONG).show();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
                alertDialog.show();
                ThemePicker.tintAlertDialogButtons(context,alertDialog);
            }
        }
    }

    private void startDetailsActivity(AdapterView<?> adapterView, View view, int i, long l){
        Weather.WeatherInfo weatherInfo = (Weather.WeatherInfo) adapterView.getItemAtPosition(i);
        // determine position in 1h array
        int position = 0;
        if (weatherCard!=null){
            position = weatherCard.forecast1hourly.size()-1;
            long targetTime = weatherInfo.getTimestamp();
            if (weatherInfo.getForecastType()== Weather.WeatherInfo.ForecastType.HOURS_6){
                targetTime = targetTime - 1000*60*60*6;
            }
            if (weatherInfo.getForecastType()== Weather.WeatherInfo.ForecastType.HOURS_24){
                targetTime = targetTime - 1000*60*60*24;
            }
            while (position>0 && weatherCard.forecast1hourly.get(position).getTimestamp()>targetTime){
                position--;
            }
        }
        Intent intent = new Intent(this, WeatherDetailsActivity.class);
        intent.putExtra(WeatherDetailsActivity.INTENT_EXTRA_POSITION,position);
        startActivity(intent);
    }

    public void popupHint(){
        final int[] hintTimes = {20,3,6,9};
        final int count = WeatherSettings.getHintCounter1(context);
        if ((count==hintTimes[1]) || (count==hintTimes[2]) || (count==hintTimes[3])){
            final RelativeLayout anchorView = (RelativeLayout) findViewById(R.id.main_layout);
            if (anchorView!=null){
                anchorView.post(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DisplayMetrics displayMetrics = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                                int displayWidth  = Math.round(displayMetrics.widthPixels);
                                int displayHeight = Math.round(displayMetrics.heightPixels);
                                final boolean isLandscape = displayWidth>displayHeight;
                                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                final View popupView = layoutInflater.inflate(R.layout.popup_hint1,null);
                                // set correct theme textcolors
                                TextView textView1 = (TextView) popupView.findViewById(R.id.hint1_text);
                                textView1.setTextColor(Color.WHITE);
                                // register click callbacks
                                Button bottonOk = (Button) popupView.findViewById(R.id.hint1_button);
                                bottonOk.setTextColor(Color.WHITE);
                                bottonOk.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (hintPopupWindow!=null){
                                            hintPopupWindow.dismiss();
                                        }
                                    }
                                });
                                CheckBox checkNo = (CheckBox) popupView.findViewById(R.id.hint1_checkbox);
                                checkNo.setTextColor(Color.WHITE);
                                checkNo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                                        if (checked){
                                            WeatherSettings.setHintCounter1(context,hintTimes[0]);
                                            WeatherSettings.setHintCounter2(context,hintTimes[0]);
                                        } else {
                                            WeatherSettings.setHintCounter1(context,0);
                                            WeatherSettings.setHintCounter2(context,0);
                                        }
                                    }
                                });
                                ImageView imageView = (ImageView) popupView.findViewById(R.id.hint1_image);
                                if (count==hintTimes[2]){
                                    textView1.setText(context.getResources().getString(R.string.welcome_s3_text4));
                                    imageView.setImageResource(R.drawable.bar_hint);
                                }
                                if (count==hintTimes[3]){
                                    textView1.setText(context.getResources().getString(R.string.welcome_s3_text3));
                                    imageView.setImageResource(R.drawable.widget_preview);
                                }
                                int width  = Math.round(displayWidth * 0.8f);
                                int height = Math.round(displayHeight * 0.26f);
                                if (isLandscape){
                                    height = Math.round(displayHeight * 0.4f);
                                }
                                hintPopupWindow = new PopupWindow(popupView,width,height,true);
                                hintPopupWindow.showAtLocation(anchorView,Gravity.CENTER,0,0);
                            }
                        });
                    }
                });
            }
        }
        if (count<hintTimes[0]){
            int newCount = count + 1;
            WeatherSettings.setHintCounter1(context,newCount);
        }
    }

    private void startGeoinput(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        builder.setTitle(getApplicationContext().getResources().getString(R.string.geoinput_title));
        LayoutInflater layoutInflater = this.getLayoutInflater();
        final View view = layoutInflater.inflate(R.layout.geoinput,null,false);
        final CheckBox useGPS = (CheckBox) view.findViewById(R.id.geoinput_check_gps);
        final TextView checkBoxText = (TextView) view.findViewById(R.id.geoinput_text_gps);
        if (!WeatherLocationManager.hasLocationPermission(context) && WeatherSettings.getAskedForLocationFlag(context)>WeatherSettings.AskedLocationFlag.NONE){
            // user actively denied permissions
            useGPS.setChecked(false);
            checkBoxText.setTextColor(ThemePicker.getColor(context,ThemePicker.ThemeColor.TEXTDARK));
            WeatherSettings.setUSEGPSFlag(getApplicationContext(),false);
        }
        final EditText text_latitude = view.findViewById(R.id.geoinput_edit_latitude);
        final EditText text_longitude = view.findViewById(R.id.geoinput_edit_longitude);
        // a tag of "null" means that the values were modified manually by the user
        final Location location = WeatherLocationManager.getLastKnownLocation(context);
        if (location!=null){
            // fake last location to hamburg for debugging
            // location.setLatitude(53.57530); location.setLongitude(10.01530);
            text_longitude.setText(new DecimalFormat("000.00000").format(location.getLongitude()));
            text_latitude.setText(new DecimalFormat("00.00000").format(location.getLatitude()));
            TextView gps_known_knote = view.findViewById(R.id.geoinput_known_note);
            gps_known_knote.setVisibility(View.VISIBLE);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss");
            gps_known_knote.setText(getApplicationContext().getResources().getString(R.string.geoinput_known_note)+" "+simpleDateFormat.format(location.getTime()));
        }
        text_latitude.setTag("autofill"); text_longitude.setTag("autofill");
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                text_latitude.setTag(null);
                text_longitude.setTag(null);
            }
        };
        text_latitude.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }
                @Override
                public void afterTextChanged(Editable editable) {
                    text_latitude.setTag(null);
                }
            });
        text_longitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                text_longitude.setTag(null);
            }
        });
        useGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    text_latitude.setEnabled(false);
                    text_longitude.setEnabled(false);
                    WeatherSettings.setUSEGPSFlag(getApplicationContext(),true);
                } else {
                    text_latitude.setEnabled(true);
                    text_longitude.setEnabled(true);
                    WeatherSettings.setUSEGPSFlag(getApplicationContext(),false);
                }
            }
        });
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_GPS_FIXED,false));
        builder.setIcon(drawable);
        builder.setView(view);
        builder.setPositiveButton(R.string.geoinput_search, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (useGPS.isChecked()){
                    if (!WeatherLocationManager.hasLocationPermission(context)){
                        requestLocationPermission(PERMISSION_CALLBACK_LOCATION);
                    } else {
                        weatherLocationManager.startGPSLocationSearch();
                    }
                } else {
                    Location own_location = new Location(Weather.WeatherLocation.CUSTOMPROVIDER);
                    if (location!=null){
                        // copy location known location if it exists to preserve timestamp from when it is
                        own_location = new Location(location);
                    }
                    // check if latitude was modified by the user
                    if (text_latitude.getTag()==null){
                        own_location.setTime(0);
                        try {
                            double latitude = Location.convert(standardizeGeo(text_latitude.getText().toString()));
                            own_location.setLatitude(latitude);
                        } catch (Exception e) {
                            PrivateLog.log(context.getApplicationContext(),PrivateLog.MAIN, PrivateLog.ERR,"Error parsing geo input: "+e.getMessage());
                            showSimpleLocationAlert(getApplicationContext().getResources().getString(R.string.geoinput_wrongformat));
                        }
                    }
                    // check if longitude was modified by the user
                    if (text_longitude.getTag()==null){
                        own_location.setTime(0);
                        try {
                            double longitude = Location.convert(standardizeGeo(text_longitude.getText().toString()));
                            own_location.setLongitude(longitude);
                        } catch (Exception e) {
                            PrivateLog.log(context.getApplicationContext(),PrivateLog.MAIN, PrivateLog.ERR,"Error parsing geo input: "+e.getMessage());
                            showSimpleLocationAlert(getApplicationContext().getResources().getString(R.string.geoinput_wrongformat));
                        }
                    }
                    if ((own_location.getLatitude()>=-90) && (own_location.getLatitude()<=90) && (own_location.getLongitude()>=-180) && (own_location.getLongitude()<=180)) {
                        launchStationSearchByLocation(own_location);
                    } else {
                        showSimpleLocationAlert(getApplicationContext().getResources().getString(R.string.geoinput_wrongvalue));
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        alertDialog.show();
        ThemePicker.tintAlertDialogButtons(context,alertDialog);
        useGPS.setChecked(WeatherSettings.getUseGPSFlag(getApplicationContext()));
    }

    private void launchStationSearchByLocation(final Location own_location){
        // load stations
        final ArrayList<Weather.WeatherLocation> stations = stationsManager.getStations();
        if (stations.size()>0){
            calcualateClosestStations(stations,own_location);
        } else {
            StationsManager.StationsReader stationsReader = new StationsManager.StationsReader(context) {
                @Override
                public void onLoadingListFinished(ArrayList<Weather.WeatherLocation> new_stations) {
                    super.onLoadingListFinished(new_stations);
                    calcualateClosestStations(stations, own_location);
                }
            };
            executor.execute(stationsReader);
        }
    }

    private void calcualateClosestStations(ArrayList<Weather.WeatherLocation> stations, final Location own_location){
        weatherLocationManager.stopGPSLocationSearch();
        stations = StationsManager.sortStationsByDistance(stations,own_location);
        Bundle bundle = own_location.getExtras();
        int items_count = 20;
        if (bundle!=null) {
            items_count = bundle.getInt(Weather.WeatherLocation.EXTRAS_ITEMS_TO_SHOW, 20);
        }
        if (WeatherSettings.GPSManual(context)){
            ArrayList<String> stationDistanceList = new ArrayList<String>();
            for (int i=0; (i<stations.size()) && (i<items_count); i++) {
                String areaString="";
                if (stations.get(i).type==RawWeatherInfo.Source.DMO){
                    areaString="Area ";
                }
                String s = stations.get(i).getOriginalDescription()+" ["+areaString+new DecimalFormat("0.0").format(stations.get(i).distance/1000) + " km]";
                stationDistanceList.add(s);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
            builder.setTitle(getApplicationContext().getResources().getString(R.string.geoinput_title));
            Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_GPS_FIXED,false));
            builder.setIcon(drawable);
            LayoutInflater layoutInflater = this.getLayoutInflater();
            final View view = layoutInflater.inflate(R.layout.geochoice,null,false);
            TextView textView_long = (TextView) view.findViewById(R.id.geochoice_reference_longitude);
            TextView textView_lat  = (TextView) view.findViewById(R.id.geochoice_reference_latitude);
            TextView textView_time = (TextView) view.findViewById(R.id.geochoice_reference_time);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd.MM.yyyy, HH:mm:ss");
            textView_long.setText(getApplicationContext().getResources().getString(R.string.geoinput_reflocation_longitude)+" "+new DecimalFormat("000.00000").format(own_location.getLongitude()));
            textView_lat.setText(getApplicationContext().getResources().getString(R.string.geoinput_reflocation_latitude)+" "+new DecimalFormat("00.00000").format(own_location.getLatitude()));
            long time = own_location.getTime();
            if (time!=0){
                textView_time.setText(getApplicationContext().getResources().getString(R.string.geoinput_reflocation_time)+" "+simpleDateFormat.format(new Date(time)));
            } else {
                // do not display time if coordinates were entered manually
                if (bundle!=null){
                    String s = bundle.getString(Weather.WeatherLocation.EXTRAS_NAME,null);
                    if (s!=null){
                        textView_time.setVisibility(View.VISIBLE);
                        textView_time.setText(s);
                    } else {
                        textView_time.setVisibility(View.GONE);
                    }
                }
            }
            builder.setView(view);
            builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            final AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
            alertDialog.show();
            ThemePicker.tintAlertDialogButtons(context,alertDialog);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,R.layout.geochoice_item,R.id.geochoiceitem_text,stationDistanceList);
            ListView listView = view.findViewById(R.id.geochoice_listview);
            listView.setAdapter(arrayAdapter);
            final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    WeatherSettings weatherSettings = new WeatherSettings(getApplicationContext());
                    TextView textView = view.findViewById(R.id.geochoiceitem_text);
                    String station_description = textView.getText().toString();
                    station_description = station_description.substring(0,station_description.indexOf(" ["));
                    Weather.WeatherLocation newWeatherLocation = stationsManager.getLocationFromDescription(station_description);
                    if (newWeatherLocation!=null){
                        if (!weatherSettings.station_name.equals(newWeatherLocation.getName())){
                            newWeatherRegionSelected(newWeatherLocation);
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getText(R.string.station_does_not_exist),Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    alertDialog.dismiss();
                }
            };
            listView.setOnItemClickListener(clickListener);
        } else {
            if (stations.size()>0){
                Weather.WeatherLocation newWeatherLocation = new Weather.WeatherLocation();
                newWeatherLocation.altitude = own_location.getAltitude();
                newWeatherLocation.latitude = own_location.getLatitude();
                newWeatherLocation.longitude = own_location.getLongitude();
                newWeatherLocation.setName(stations.get(0).getName());
                newWeatherLocation.setDescription(stations.get(0).getOriginalDescription());
                newWeatherRegionSelected(newWeatherLocation);
            }
        }
    }

    public static final int PERMISSION_CALLBACK_LOCATION                   = 121;
    public static final int PERMISSION_CALLBACK_LOCATION_BEFORE_BACKGROUND = 122;
    public static final int PERMISSION_CALLBACK_BACKGROUND_LOCATION        = 123;
    public static final String LOCATION_DENIED                             = "android.permission.LOCATION_DENIED";

    private void requestLocationPermission(int callback){
        // below SDK 23, permissions are granted at app install.
        if (android.os.Build.VERSION.SDK_INT >=23){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},callback);
            WeatherSettings.setAskedLocationFlag(context,WeatherSettings.AskedLocationFlag.LOCATION);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int permRequestCode, String perms[], int[] grantRes){
        boolean hasLocationPermission = false;
        boolean hasBackgroundLocationPermission = false;
        for (int i=0; i<grantRes.length; i++){
            if ((perms[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) && (grantRes[i]==PackageManager.PERMISSION_GRANTED)){
                hasLocationPermission = true;
            }
            if ((perms[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) && (grantRes[i]==PackageManager.PERMISSION_GRANTED)){
                hasLocationPermission = true;
            }
            if ((perms[i].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) && (grantRes[i]==PackageManager.PERMISSION_GRANTED)){
                hasLocationPermission = true;
            }
        }
        // on sdk below 29, background permission is not present/always true if normal, foreground permission was granted.
        // the above loop will result in "false" for sdk below 29, and this needs to be fixed.
        if (android.os.Build.VERSION.SDK_INT<=29){
            if (hasLocationPermission){
                hasBackgroundLocationPermission=true;
            }
        }
        if (permRequestCode == PERMISSION_CALLBACK_LOCATION){
            if (hasLocationPermission){
                weatherLocationManager.startGPSLocationSearch();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    showPermissionsRationale(Manifest.permission.ACCESS_FINE_LOCATION,PERMISSION_CALLBACK_LOCATION);
                } else {
                    if (WeatherSettings.getAskedForLocationFlag(context)>=WeatherSettings.AskedLocationFlag.LOCATION){
                        showPermissionsRationale(LOCATION_DENIED,PERMISSION_CALLBACK_LOCATION);
                    }
                }
            }
        }
    }

    @Override
    public void onTrimMemory(final int level) {

        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                if (forecastAdapter != null) {
                    forecastAdapter.clearBitmapCache();
                }
                break;
        }
    }

    private void showSimpleLocationAlert(String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        builder.setTitle(getApplicationContext().getResources().getString(R.string.geoinput_title));
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_INFO_OUTLINE,false));
        builder.setIcon(drawable);
        builder.setMessage(text);
        builder.setPositiveButton(R.string.alertdialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        alertDialog.show();
        ThemePicker.tintAlertDialogButtons(context,alertDialog);
    }

    private void showPermissionsRationale(final String permisson, final int callback){
        String text = getApplicationContext().getResources().getString(R.string.geoinput_rationale);
        if (permisson.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
            text = getApplicationContext().getResources().getString(R.string.backgroundGPS_rationale);
        }
        if (permisson.equals(LOCATION_DENIED)){
            text = getApplicationContext().getResources().getString(R.string.geoinput_settingshint);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_INFO_OUTLINE,false));
        builder.setIcon(drawable);
        builder.setMessage(text);
        builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton(getApplicationContext().getResources().getString(R.string.allow), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (permisson.equals(Manifest.permission.ACCESS_FINE_LOCATION)){
                    requestLocationPermission(callback);
                }
                if (permisson.equals(LOCATION_DENIED)){
                    // jump immediately to the settings screen
                    dialogInterface.dismiss();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package",getPackageName(),null));
                    startActivity(intent);
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        alertDialog.show();
        ThemePicker.tintAlertDialogButtons(context,alertDialog);
    }

    public String standardizeGeo(final String s){
        return s.replace(",",".");
    }

    private void startWelcomeActivity() {
        WeatherSettings.setHintCounter1(context,0);
        WeatherSettings.setHintCounter2(context,0);
        Intent i = new Intent(this, WelcomeActivity.class);
        startActivity(i);
        finish();
    }

    public void checkForBatteryOptimization(final Context context){
        PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.getApplicationContext().getPackageName());
            if ((!isIgnoringBatteryOptimizations) && (WeatherSettings.getBatteryOptimiziatonFlag(context)==WeatherSettings.BatteryFlag.AGREED)){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Settings.openBatteryOptimizationSettings(context);
                    }
                });
            }
        }
    }

}



