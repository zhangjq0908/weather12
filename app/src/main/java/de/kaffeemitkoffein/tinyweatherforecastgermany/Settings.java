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
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.*;
import android.widget.Toast;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;

    private CheckBoxPreference useBackgroundLocation;

    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            if ((sharedPreferences != null) && (s != null)) {
                updateValuesDisplay();
                if (s.equals(WeatherSettings.PREF_LOG_TO_LOGCAT)) {
                    WeatherSettings ws = new WeatherSettings(context);
                    if (ws.log_to_logcat) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(context.getResources().getString(R.string.alertdialog_1_title));
                        builder.setMessage(context.getResources().getString(R.string.alertdialog_1_text));
                        builder.setIcon(R.mipmap.ic_warning_white_24dp);
                        builder.setPositiveButton(context.getResources().getString(R.string.alertdialog_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        builder.setNegativeButton(context.getResources().getString(R.string.alertdialog_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                WeatherSettings weatherSettings = new WeatherSettings(context);
                                weatherSettings.applyPreference(WeatherSettings.PREF_LOG_TO_LOGCAT, false);
                                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_LOG_TO_LOGCAT);
                                if (checkBoxPreference != null) {
                                    checkBoxPreference.setChecked(false);
                                    dialogInterface.dismiss();
                                }
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
                        alertDialog.show();
                    }
                }
                if ((s.equals(WeatherSettings.PREF_USE_METERED_NETWORKS) && (!WeatherSettings.useMeteredNetworks(context))) ||
                        ((s.equals(WeatherSettings.PREF_USE_WIFI_ONLY)) && (WeatherSettings.useWifiOnly(context)))
                                && ((WeatherSettings.notifyWarnings(context) || WeatherSettings.displayWarningsInWidget(context)))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(context.getResources().getString(R.string.alertdialog_1_title));
                    builder.setMessage(context.getResources().getString(R.string.preference_meterednetwork_notice));
                    builder.setIcon(R.mipmap.warning_icon);
                    builder.setPositiveButton(context.getResources().getString(R.string.alertdialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
                    alertDialog.show();
                }
                if (s.equals(WeatherSettings.PREF_SERVE_GADGETBRIDGE)) {
                    setAlarmSettingAllowed();
                }
                if (s.equals(WeatherSettings.PREF_WARNINGS_DISABLE)) {
                    setShowWarningsInWidgetAllowed();
                    setNotifyWarnings();
                    setNotifySeverity();
                }
                if (s.equals(WeatherSettings.PREF_WIDGET_DISPLAYWARNINGS)) {
                    setShowWarningsInWidgetAllowed();
                    WidgetRefresher.refresh(context);
                }
                if (s.equals(WeatherSettings.PREF_NOTIFY_WARNINGS)) {
                    setNotifyWarnings();
                    setNotifySeverity();
                }
                if (s.equals(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_MINMAXUSE) ||
                        (s.equals(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_MIN)) ||
                        (s.equals(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_MAX))) {
                    setUseMinMax();
                    WidgetRefresher.refreshChartWidget(context);
                }
                if (s.equals(WeatherSettings.PREF_THEME)) {
                    WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.VIEWS);
                    WidgetRefresher.refresh(context);
                    recreate();
                }
                if (s.equals(WeatherSettings.PREF_ROTATIONMODE)) {
                    recreate();
                }
                // values with impact on main activity & widgets
                if (s.equals(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_DAYS)) {
                    WidgetRefresher.refreshChartWidget(context);
                }
                if (s.equals(WeatherSettings.PREF_WIDGET_SHOWDWDNOTE) || (s.equals(WeatherSettings.PREF_WIDGET_OPACITY))){
                    WidgetRefresher.refresh(context.getApplicationContext());
                }
                // invalidate weather display because the display options have changed
                if (s.equals(WeatherSettings.PREF_DISPLAY_TYPE) || (s.equals(WeatherSettings.PREF_DISPLAY_BAR)) || (s.equals(WeatherSettings.PREF_DISPLAY_PRESSURE)) ||
                        (s.equals(WeatherSettings.PREF_DISPLAY_VISIBILITY)) || (s.equals(WeatherSettings.PREF_DISPLAY_SUNRISE)) || (s.equals(WeatherSettings.PREF_DISPLAY_DISTANCE_UNIT)) ||
                        s.equals(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART) || s.equals(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_DAYS) || (s.equals(WeatherSettings.PREF_VIEWMODEL)) ||
                        (s.equals(WeatherSettings.PREF_UVHI_MAINDISPLAY)) || (s.equals(WeatherSettings.PREF_DISPLAY_STATION_GEO)) ||
                        (s.equals(WeatherSettings.PREF_DISPLAY_WIND_TYPE)) || (s.equals(WeatherSettings.PREF_DISPLAY_WIND_UNIT))){
                        WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.DATA);
                    // invalidate weather display and widgets
                    if ((s.equals(WeatherSettings.PREF_DISPLAY_WIND_TYPE)) || (s.equals(WeatherSettings.PREF_DISPLAY_WIND_UNIT))){
                        WidgetRefresher.refresh(getApplicationContext());
                    }
                }
                if (s.equals(WeatherSettings.PREF_UVHI_FETCH_DATA)){
                    if (!Weather.hasUVHIData(context)){
                            forcedWeatherUpdate();
                    }
                }
                if (s.equals(WeatherSettings.PREF_USE_BACKGROUND_LOCATION)){
                    if (WeatherSettings.useBackgroundLocation(context)){
                        if (!WeatherLocationManager.hasLocationPermission(context)){
                            requestBackgroundLocationPermission();
                        }
                    }
                }
                if (s.equals(WeatherSettings.PREF_REPLACE_BY_MUNICIPALITY)){
                    WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.VIEWS);
                }
                if (s.equals(WeatherSettings.PREF_DISPLAY_WIND_IN_CHARTS) || s.equals(WeatherSettings.PREF_DISPLAY_WIND_IN_CHARTS_MAX)){
                    WeatherSettings.setWeatherUpdatedFlag(context,WeatherSettings.UpdateType.VIEWS);
                }
            }
        }
    };

    public void forcedWeatherUpdate(){
        ArrayList<String> updateTasks = new ArrayList<String>();
        updateTasks.add(DataUpdateService.SERVICEEXTRAS_UPDATE_WEATHER);
        updateTasks.add(DataUpdateService.SERVICEEXTRAS_UPDATE_WARNINGS);
        UpdateAlarmManager.updateAndSetAlarmsIfAppropriate(getApplicationContext(), UpdateAlarmManager.UPDATE_FROM_ACTIVITY,updateTasks,null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle bundle) {
        setTheme(ThemePicker.GetTheme(this));
        super.onCreate(bundle);
        context = this;
        WeatherSettings.setRotationMode(this);
        addPreferencesFromResource(R.xml.preferences);
        updateValuesDisplay();
        // reset notifications option
        Preference resetNotifications = (Preference) findPreference(WeatherSettings.PREF_CLEARNOTIFICATIONS);
        if (resetNotifications != null) {
            resetNotifications.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(context, CancelNotificationBroadcastReceiver.class);
                    intent.setAction(CancelNotificationBroadcastReceiver.CLEAR_NOTIFICATIONS_ACTION);
                    sendBroadcast(intent);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, context.getResources().getString(R.string.preference_clearnotifications_message), Toast.LENGTH_LONG).show();
                        }
                    });
                    return true;
                }
            });
        }
        // action bar layout
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
    }

    @SuppressWarnings("deprecation")
    public void disableLogCatLogging() {
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_LOG_TO_LOGCAT);
        if (checkBoxPreference != null) {
            checkBoxPreference.setChecked(false);
            checkBoxPreference.setEnabled(false);
            checkBoxPreference.setShouldDisableView(true);
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("PREF_screen_logging");
            if (preferenceScreen != null) {
                preferenceScreen.removePreference(checkBoxPreference);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void disableClearNotifications() {
        Preference preference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_CLEARNOTIFICATIONS);
        if (preference != null) {
            preference.setEnabled(false);
            preference.setShouldDisableView(true);
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("PREF_screen_logging");
            if (preferenceScreen != null) {
                preferenceScreen.removePreference(preference);
            }
        }
    }


    @SuppressWarnings("deprecation")
    public void disableTLSOption() {
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_DISABLE_TLS);
        if (checkBoxPreference != null) {
            checkBoxPreference.setChecked(false);
            checkBoxPreference.setEnabled(false);
            checkBoxPreference.setShouldDisableView(true);
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("PREF_screen_logging");
            if (preferenceScreen != null) {
                preferenceScreen.removePreference(checkBoxPreference);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void setAlarmSettingAllowed() {
        WeatherSettings weatherSettings = new WeatherSettings(context);
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_SETALARM);
        if (checkBoxPreference != null) {
            checkBoxPreference.setEnabled(!weatherSettings.serve_gadgetbridge);
            checkBoxPreference.setShouldDisableView(true);
            if (weatherSettings.serve_gadgetbridge) {
                checkBoxPreference.setSummary(context.getResources().getString(R.string.preference_setalarm_summary) + System.getProperty("line.separator") + context.getResources().getString(R.string.preference_setalarm_notice));
            } else {
                checkBoxPreference.setSummary(context.getResources().getString(R.string.preference_setalarm_summary));
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void setShowWarningsInWidgetAllowed() {
        WeatherSettings weatherSettings = new WeatherSettings(context);
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_WIDGET_DISPLAYWARNINGS);
        if (checkBoxPreference != null) {
            checkBoxPreference.setEnabled(!weatherSettings.warnings_disabled);
            checkBoxPreference.setShouldDisableView(true);
        }
    }

    public void setNotifyWarnings() {
        WeatherSettings weatherSettings = new WeatherSettings(context);
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_NOTIFY_WARNINGS);
        if (checkBoxPreference != null) {
            checkBoxPreference.setEnabled(!weatherSettings.warnings_disabled);
            checkBoxPreference.setShouldDisableView(true);
        }
    }

    public void setNotifySeverity() {
        WeatherSettings weatherSettings = new WeatherSettings(context);
        ListPreference listPreference = (ListPreference) findPreference(WeatherSettings.PREF_WARNINGS_NOTIFY_SEVERITY);
        if (listPreference != null) {
            if ((weatherSettings.warnings_disabled) || (!weatherSettings.notify_warnings)) {
                listPreference.setEnabled(false);
                listPreference.setShouldDisableView(true);
            } else {
                listPreference.setEnabled(true);
                listPreference.setShouldDisableView(false);
            }
        }
    }

    public void setNotifyLED() {
        CheckBoxPreference ledNotifications = (CheckBoxPreference) findPreference(WeatherSettings.PREF_WARNINGS_NOTIFY_LED);
        LEDColorPreference ledColorPreference = (LEDColorPreference) findPreference(WeatherSettings.PREF_LED_COLOR);
        if ((ledNotifications != null) && (ledColorPreference != null)) {
            if (!WeatherSettings.notifyWarnings(context) || (WeatherSettings.areWarningsDisabled(context))) {
                ledNotifications.setEnabled(false);
                ledNotifications.setShouldDisableView(true);
            } else {
                ledNotifications.setEnabled(true);
                ledNotifications.setShouldDisableView(false);
            }
            if ((!WeatherSettings.LEDEnabled(context)) || (WeatherSettings.areWarningsDisabled(context)) || (!WeatherSettings.notifyWarnings(context))) {
                ledColorPreference.setEnabled(false);
                ledColorPreference.setShouldDisableView(true);
            } else {
                ledColorPreference.setEnabled(true);
                ledColorPreference.setShouldDisableView(false);
            }
        }
    }

    public void setUseMinMax() {
        CheckBoxPreference checkBoxPreferenceChartUseMinMax = (CheckBoxPreference) findPreference(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_MINMAXUSE);
        NumberPickerPreference numberPickerPreferenceChartRangeMin = (NumberPickerPreference) findPreference(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_MIN);
        NumberPickerPreference numberPickerPreferenceChartRangeMax = (NumberPickerPreference) findPreference(WeatherSettings.PREF_DISPLAY_OVERVIEWCHART_MAX);
        if ((checkBoxPreferenceChartUseMinMax != null) && (numberPickerPreferenceChartRangeMin != null) && (numberPickerPreferenceChartRangeMax != null)) {
            if (!checkBoxPreferenceChartUseMinMax.isChecked()) {
                numberPickerPreferenceChartRangeMin.setEnabled(false);
                numberPickerPreferenceChartRangeMin.setShouldDisableView(true);
                numberPickerPreferenceChartRangeMax.setEnabled(false);
                numberPickerPreferenceChartRangeMax.setShouldDisableView(true);
                numberPickerPreferenceChartRangeMin.setSummary(context.getResources().getString(R.string.preference_screen_overviewchart_min_summary) + " -");
                numberPickerPreferenceChartRangeMax.setSummary(context.getResources().getString(R.string.preference_screen_overviewchart_max_summary) + " -");
            } else {
                numberPickerPreferenceChartRangeMin.setEnabled(true);
                numberPickerPreferenceChartRangeMin.setShouldDisableView(false);
                numberPickerPreferenceChartRangeMax.setEnabled(true);
                numberPickerPreferenceChartRangeMax.setShouldDisableView(false);
                numberPickerPreferenceChartRangeMin.setSummary(context.getResources().getString(R.string.preference_screen_overviewchart_min_summary) + " " + WeatherSettings.getOverviewChartMin(context));
                numberPickerPreferenceChartRangeMax.setSummary(context.getResources().getString(R.string.preference_screen_overviewchart_max_summary) + " " + WeatherSettings.getOverviewChartMax(context));
            }
        }
    }

    public boolean disableIfUnchecked(Preference target, final CheckBoxPreference source) {
        if ((target != null) && (source != null)) {
            if (source.isChecked()) {
                target.setEnabled(true);
                target.setShouldDisableView(false);
            } else {
                target.setEnabled(false);
                target.setShouldDisableView(true);
            }
            return true;
        }
        return false;
    }

    public void setUVHIdisplayMain() {
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(WeatherSettings.PREF_UVHI_MAINDISPLAY);
        if (!WeatherSettings.UVHIfetchData(context)) {
            WeatherSettings.setUVHImainDisplay(context,false);
            checkBoxPreference.setChecked(false);
            checkBoxPreference.setEnabled(false);
            checkBoxPreference.setShouldDisableView(true);
        } else {
            checkBoxPreference.setEnabled(true);
            checkBoxPreference.setShouldDisableView(false);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onResume(){
        super.onResume();
        context = this;
        // the following handles an aborted action to grant permissions in app settings; brings the preferences
        // in line with the permission not changed/granted.
        if (useBackgroundLocation==null){
            useBackgroundLocation = (CheckBoxPreference) findPreference(WeatherSettings.PREF_USE_BACKGROUND_LOCATION);
        }
        if (!WeatherLocationManager.hasBackgroundLocationPermission(context)){
            useBackgroundLocation.setChecked(false);
        } else {
            useBackgroundLocation.setChecked(true);
        }
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onPause(){
        super.onPause();
        if (listener!=null){
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    @SuppressWarnings("deprecation")
    private void updateValuesDisplay(){
        if (!WeatherSettings.appReleaseIsUserdebug()){
            disableLogCatLogging();
            //disableClearNotifications();
        }
        if (!WeatherSettings.isTLSdisabled(context)){
            disableTLSOption();
        }
        if (WeatherSettings.getViewModel(context).equals(WeatherSettings.ViewModel.EXTENDED)){
            // do something
        }
        // allow changing alarm state?
        setAlarmSettingAllowed();
        // allow warnings in widget setting?
        setShowWarningsInWidgetAllowed();
        setNotifyWarnings();
        setNotifySeverity();
        setUseMinMax();
        setNotifyLED();
        //setUVHIdisplayMain();
        SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(this);
        String gadgetbridge_packagename = sp.getString(WeatherSettings.PREF_GADGETBRIDGE_PACKAGENAME,WeatherSettings.PREF_GADGETBRIDGE_PACKAGENAME_DEFAULT);
        if (gadgetbridge_packagename.equals("")){
            gadgetbridge_packagename = WeatherSettings.PREF_GADGETBRIDGE_PACKAGENAME_DEFAULT;
            SharedPreferences.Editor preferences_editor = sp.edit();
            preferences_editor.putString(WeatherSettings.PREF_GADGETBRIDGE_PACKAGENAME, gadgetbridge_packagename);
            preferences_editor.commit();
            Toast.makeText(this,getResources().getString(R.string.preference_gadgetbridge_package_reset_toast),Toast.LENGTH_LONG).show();
            finish();
        }
        CheckBoxPreference warningsInWidget = (CheckBoxPreference) findPreference(WeatherSettings.PREF_WIDGET_DISPLAYWARNINGS);
        if (warningsInWidget!=null){
            warningsInWidget.setSummary(getResources().getString(R.string.preference_displaywarninginwidget_summary)+" "+getResources().getString(R.string.battery_and_data_hint));
        }
        CheckBoxPreference notifyWarnings = (CheckBoxPreference) findPreference(WeatherSettings.PREF_NOTIFY_WARNINGS);
        if (notifyWarnings!=null){
            notifyWarnings.setSummary(getResources().getString(R.string.preference_notify_warnings_summary)+" "+getResources().getString(R.string.battery_and_data_hint));
        }
        ListPreference displayRotation = (ListPreference) findPreference(WeatherSettings.PREF_ROTATIONMODE);
        if (displayRotation!=null){
            displayRotation.setSummary(WeatherSettings.getDeviceRotationString(this));
        }
        Preference resetPreferences = (Preference) findPreference("PREF_reset");
        if (resetPreferences!=null){
            resetPreferences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MainActivity.askDialog(context,null,getResources().getString(R.string.alertdialog_3_title),
                            new String[] {getResources().getString(R.string.alertdialog_3_text1),"",getResources().getString(R.string.alertdialog_3_text2)},
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    WeatherSettings.resetPreferencesToDefault(context);
                                    Intent intent = new Intent(context, WelcomeActivity.class);
                                    Toast.makeText(context,getResources().getString(R.string.alertdialog_3_toast),Toast.LENGTH_LONG).show();
                                    // need to set this, because otherwise the WelcomeActivity might get called also from
                                    // a re-launched MainActivity.
                                    WeatherSettings.setAppLaunchedFlag(context);
                                    // force a replay
                                    intent.putExtra("mode","replay");
                                    startActivity(intent);
                                }
                            });
                    return true;
                };
            });
        }
        LEDColorPreference ledColorPreference = (LEDColorPreference) findPreference(WeatherSettings.PREF_LED_COLOR);
        if (ledColorPreference!=null){
            ledColorPreference.setColorItem(WeatherSettings.getLEDColorItem(context));
            ledColorPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    LEDColorPicker ledColorPicker = new LEDColorPicker(context);
                    ledColorPicker.setOnColorPickedListener(new LEDColorPicker.OnColorPickedListener() {
                        @Override
                        public void onColorSelected(int colorItem) {
                            WeatherSettings.setLEDColorItem(context,colorItem);
                        }
                    });
                    ledColorPicker.show();
                    return true;
                }
            });
        }
        /*
         See DataUpdateService.java, suitableNetworkAvailable(Context context):
         for api below 23, we can offer a check to use Wi-Fi only,
         for api above 22 we check for metered/unmetered networks.
         One of the preferences always needs to be removed.
         */
        PreferenceCategory preferenceCategoryGeneral = (PreferenceCategory) findPreference(WeatherSettings.PREF_CATEGORY_GENERAL);
        CheckBoxPreference wifiOnly = (CheckBoxPreference) findPreference(WeatherSettings.PREF_USE_WIFI_ONLY);
        CheckBoxPreference useMeteredNetworks = (CheckBoxPreference) findPreference(WeatherSettings.PREF_USE_METERED_NETWORKS);
        if (Build.VERSION.SDK_INT < 23){
            if (useMeteredNetworks!=null){
                preferenceCategoryGeneral.removePreference(useMeteredNetworks);
            }
        } else {
            if (wifiOnly!=null){
                preferenceCategoryGeneral.removePreference(wifiOnly);
            }
        }
        NumberPickerPreference locationsToShare = (NumberPickerPreference) findPreference(WeatherSettings.PREF_MAX_LOCATIONS_IN_SHARED_WARNINGS);
        if (locationsToShare!=null){
            locationsToShare.setSummary(getResources().getString(R.string.preference_max_loc_in_shared_warnings_summary)+" "+WeatherSettings.getMaxLocationsInSharedWarnings(context));
        }
        useBackgroundLocation = (CheckBoxPreference) findPreference(WeatherSettings.PREF_USE_BACKGROUND_LOCATION);
        EditTextPreference maxWindScaleInChartsPreference = (EditTextPreference) findPreference(WeatherSettings.PREF_DISPLAY_WIND_IN_CHARTS_MAX);
        if (maxWindScaleInChartsPreference!=null){
            maxWindScaleInChartsPreference.setSummary(context.getResources().getString(R.string.preference_display_wind_in_charts_max_summary)+ " "+ WeatherSettings.getWindInChartsMaxKmh(context)+ " km/h");
        }
     }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        updateValuesDisplay();
    }

    private void requestLocationPermission(int callback){
        // below SDK 23, permissions are granted at app install.
        if (android.os.Build.VERSION.SDK_INT >=23){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},callback);
            WeatherSettings.setAskedLocationFlag(context,WeatherSettings.AskedLocationFlag.LOCATION);
        }
    }

    public void requestBackgroundLocationPermission(){
        if (WeatherLocationManager.hasBackgroundLocationPermission(context)){
            return;
        }
        if (WeatherLocationManager.hasLocationPermission(context)){
            // on sdk < 29 background permission is already granted/does not exist
            if (android.os.Build.VERSION.SDK_INT>=29){
                // on sdk 29 and above try the permission dialog exactly once
                if (WeatherSettings.getAskedForLocationFlag(context)<WeatherSettings.AskedLocationFlag.BACKGROUND_LOCATION) {
                    // ask exactly once
                    requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},MainActivity.PERMISSION_CALLBACK_BACKGROUND_LOCATION);
                    // remember that we asked for it
                    WeatherSettings.setAskedLocationFlag(context,WeatherSettings.AskedLocationFlag.BACKGROUND_LOCATION);
                } else {
                    // go to the settings if already asked once
                    openPermissionSettings();
                }

                // on sdk above 29, always go directly to the settings
                /*
                if (android.os.Build.VERSION.SDK_INT>29) {
                    openPermissionSettings();
                }
                 */
            }
        } else {
            // ask for simple foreground location permission, since this is missing.
            // on sdk below 28, this is enoungh.
            // on sdk 29 and above, we need to return here from onRequestPermissionResult
            requestLocationPermission(MainActivity.PERMISSION_CALLBACK_LOCATION_BEFORE_BACKGROUND);
            WeatherSettings.setAskedLocationFlag(context,WeatherSettings.AskedLocationFlag.LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permRequestCode, String[] perms, int[] grantRes){
        boolean hasLocationPermission = false;
        boolean hasBackgroundLocationPermission = false;
        for (int i=0; i<grantRes.length; i++){
            if ((perms[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) && (grantRes[i]== PackageManager.PERMISSION_GRANTED)){
                hasLocationPermission = true;
            }
            if ((perms[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) && (grantRes[i]== PackageManager.PERMISSION_GRANTED)){
                hasLocationPermission = true;
            }
            if ((perms[i].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) && (grantRes[i]==PackageManager.PERMISSION_GRANTED)){
                hasBackgroundLocationPermission = true;
            }
        }
        // on sdk below 29, background permission is not present/always true if normal, foreground permission was granted.
        // the above loop will result in "false" for sdk below 29, and this needs to be fixed.
        if (Build.VERSION.SDK_INT<29){
            if (hasLocationPermission){
                hasBackgroundLocationPermission=true;
            }
        }
        if (permRequestCode == MainActivity.PERMISSION_CALLBACK_LOCATION){
            if (hasLocationPermission){
                // do nothing
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if ((shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) || (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION))){
                        showPermissionsRationale(Manifest.permission.ACCESS_FINE_LOCATION,MainActivity.PERMISSION_CALLBACK_LOCATION);
                    } else {
                        if (WeatherSettings.getAskedForLocationFlag(context)>=WeatherSettings.AskedLocationFlag.LOCATION){
                            showPermissionsRationale(MainActivity.LOCATION_DENIED,MainActivity.PERMISSION_CALLBACK_LOCATION);
                        }
                    }
                }
            }
        }
        if (permRequestCode == MainActivity.PERMISSION_CALLBACK_LOCATION_BEFORE_BACKGROUND){
            if (Build.VERSION.SDK_INT >= 29) {
                if (hasLocationPermission){
                    requestBackgroundLocationPermission();
                } else {
                    showPermissionsRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION,MainActivity.PERMISSION_CALLBACK_LOCATION_BEFORE_BACKGROUND);
                }
            } else {
                // nothing to do, background permission is always granted with location permission
                WeatherSettings.setAskedLocationFlag(context,WeatherSettings.AskedLocationFlag.BACKGROUND_LOCATION);
            }
        }
        if (permRequestCode == MainActivity.PERMISSION_CALLBACK_BACKGROUND_LOCATION){
            if (hasBackgroundLocationPermission){
                PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (!powerManager.isIgnoringBatteryOptimizations(context.getApplicationContext().getPackageName())) {
                        openBatteryOptimizationSettings(context);
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= 29) {
                    showPermissionsRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION, MainActivity.PERMISSION_CALLBACK_BACKGROUND_LOCATION);
                }
            }
        }
    }

    public void openPermissionSettings(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        builder.setTitle(getApplicationContext().getResources().getString(R.string.geoinput_title));
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_INFO_OUTLINE,false));
        builder.setIcon(drawable);
        String text = getApplicationContext().getResources().getString(R.string.backgroundGPS_settingshint)+" \""+
                getApplicationContext().getResources().getString(R.string.always_allow)+"\".";
        if (android.os.Build.VERSION.SDK_INT>=30){
            String label = (String) getPackageManager().getBackgroundPermissionOptionLabel();
            text = getApplicationContext().getResources().getString(R.string.backgroundGPS_settingshint)+" \""+
                    label+"\".";
        }
        builder.setMessage(text);
        builder.setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package",getPackageName(),null));
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                useBackgroundLocation.setChecked(false);
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
        alertDialog.show();
    }

    public static void openBatteryOptimizationSettings(final Context context){
        // only open on api 23 and higher
        if (Build.VERSION.SDK_INT>=23){
            AlertDialog.Builder builder = new AlertDialog.Builder(context,0);
            builder.setTitle(context.getApplicationContext().getResources().getString(R.string.geoinput_title));
            Drawable drawable = new BitmapDrawable(context.getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_INFO_OUTLINE,false));
            builder.setIcon(drawable);
            String text = context.getApplicationContext().getResources().getString(R.string.disable_battery_saving_rationale);
            builder.setMessage(text);
            builder.setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    WeatherSettings.setBatteryOptimiziatonFlag(context,WeatherSettings.BatteryFlag.AGREED);
                    Intent i3 = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i3.setData(Uri.fromParts("package",context.getPackageName(),null));
                    i3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i3);
                }
            });
            builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    WeatherSettings.setBatteryOptimiziatonFlag(context,WeatherSettings.BatteryFlag.REJECTED);
                    dialogInterface.dismiss();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setBackgroundDrawable(ThemePicker.getWidgetBackgroundDrawable(context));
            alertDialog.show();
        }
    }

    private void showPermissionsRationale(final String permission, final int callback){
        String text = getApplicationContext().getResources().getString(R.string.geoinput_rationale);
        if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
            text = getApplicationContext().getResources().getString(R.string.backgroundGPS_rationale);
        }
        if (permission.equals(MainActivity.LOCATION_DENIED)){
            text = getApplicationContext().getResources().getString(R.string.geoinput_settingshint);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this,0);
        Drawable drawable = new BitmapDrawable(getResources(),WeatherIcons.getIconBitmap(context,WeatherIcons.IC_INFO_OUTLINE,false));
        builder.setIcon(drawable);
        builder.setMessage(text);
        builder.setNegativeButton(R.string.geoinput_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    WeatherSettings.setBatteryOptimiziatonFlag(context,WeatherSettings.BatteryFlag.REJECTED);
                    useBackgroundLocation.setChecked(false);
                }
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton(getApplicationContext().getResources().getString(R.string.allow), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)){
                    requestLocationPermission(callback);
                }
                if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                    WeatherSettings.setBatteryOptimiziatonFlag(context,WeatherSettings.BatteryFlag.AGREED);
                    dialogInterface.dismiss();
                    openPermissionSettings();
                }
                if (permission.equals(MainActivity.LOCATION_DENIED)){
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
    }


}


