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

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.os.Bundle;
import android.widget.TextView;
import android.content.Intent;
import java.io.IOException;
import java.io.InputStream;


public class LicenseInfo extends Activity implements View.OnClickListener {
    private Button button_back;
    private Context context;
    public static final String DATA_TITLE="DATA_TITLE";
    public static final String DATA_TEXTRESOURCE="DATA_TEXTRESOURCE";
    public static final String DATA_BUTTONTEXT="DATA_BUTTONTEXT";

    @Override
    protected void onCreate (Bundle bundle){
        context = getApplicationContext();
        PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.INFO,"LicenseInfo activity started.");
        try {
            ThemePicker.SetTheme(this);
        } catch (Exception e){
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.ERR,"Error setting theme in LicenseInfo activity.");
        }
        super.onCreate(bundle);
        displayInfo();
    }

    private void displayInfo(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WeatherSettings.setRotationMode(this);
        setContentView(R.layout.licenseinfo);
        TextView title = (TextView) findViewById(R.id.infoTitle);
        TextView textview = (TextView) findViewById(R.id.infoTextView);
        button_back = (Button) findViewById(R.id.info_button_back);
        Intent intent = getIntent();
        title.setText(intent.getExtras().getString(DATA_TITLE,""));
        button_back.setText(intent.getExtras().getString(DATA_BUTTONTEXT,""));
        button_back.setOnClickListener(this);
        String textfile = intent.getExtras().getString(DATA_TEXTRESOURCE,"");
        InputStream inputStream = getResources().openRawResource(getResources().getIdentifier(textfile,"raw",getApplicationContext().getPackageName()));
        try {
            int size = inputStream.available();
            byte[] textdata = new byte[size];
            inputStream.read(textdata);
            inputStream.close();
            String text = new String(textdata);
            textview.setText(text);
        } catch (IOException e) {
            PrivateLog.log(context,PrivateLog.MAIN,PrivateLog.FATAL,"Error reading data in LicenseInfo activity. Forcing finish.");
            finish();
        }
    }

    @Override
    public void onClick(View view){
        if (view==button_back){
            finish();
        }
    }

}
