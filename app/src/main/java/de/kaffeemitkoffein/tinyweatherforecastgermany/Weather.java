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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import org.astronomie.info.Astronomy;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class Weather {

    public final static double KelvinConstant = 273.15;
    public final static int MILLIS_IN_HOUR = 60*60*1000;
    public final static int DELTA_T = 69;

    public final static int PROB_OF_PRECIPITATION_ITEM_COUNT=12;

    public static class WeatherLocation implements Comparator<WeatherLocation>, Parcelable {

        public static final String EXTRAS_NAME="NAME";
        public static final String EXTRAS_ITEMS_TO_SHOW="ITEMS_TO_SHOW";
        public static final String PARCELABLE_NAME = "de.kaffeemitkoffein.tinyweatherforecastgermany.WHEATHERLOCATION";
        public static final float ACCURACY_UNKNOWN = -1f;
        private static final String SEPARATOR = "|";
        public static final String EMPTYVALUE = "";
        public static final String CUSTOMPROVIDER="manual";

        private String description = EMPTYVALUE;
        private String name = EMPTYVALUE;
        private String description_alternate = EMPTYVALUE;
        double latitude = 0d;
        double longitude = 0d;
        double altitude = 0d;
        float distance = 0f;
        float accuracy = ACCURACY_UNKNOWN;
        int type = RawWeatherInfo.Source.UNKNOWN;
        long time = 0l;

        public WeatherLocation(){
        }

        public WeatherLocation(String description, String description_alternate, String name, int type, double latitude, double longitude, double altitude){
            this.description = description;
            this.description_alternate = description_alternate;
            this.name = name;
            this.type = type;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }

        public WeatherLocation(WeatherLocation weatherLocation){
            this.description = weatherLocation.description;
            this.description_alternate = weatherLocation.description_alternate;
            this.name = weatherLocation.name;
            this.type = weatherLocation.type;
            this.latitude = weatherLocation.latitude;
            this.longitude = weatherLocation.longitude;
            this.altitude = weatherLocation.altitude;
        }

        public WeatherLocation(Location location){
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
            if (location.hasAltitude()){
                this.altitude = location.getAltitude();
            }
            if (location.hasAccuracy()){
                this.accuracy = location.getAccuracy();
            } else {
                this.accuracy = ACCURACY_UNKNOWN;
            }
            this.time = location.getTime();
        }

        protected WeatherLocation(Parcel in) {
            description = in.readString();
            description_alternate = in.readString();
            name = in.readString();
            type = in.readInt();
            latitude = in.readDouble();
            longitude = in.readDouble();
            altitude = in.readDouble();
            distance = in.readFloat();
            accuracy = in.readFloat();
            type = in.readInt();
            time = in.readLong();
        }

        public String serializeToString(){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(description); stringBuilder.append(SEPARATOR);
            stringBuilder.append(description_alternate); stringBuilder.append(SEPARATOR);
            stringBuilder.append(name); stringBuilder.append(SEPARATOR);
            stringBuilder.append(type); stringBuilder.append(SEPARATOR);
            stringBuilder.append(latitude); stringBuilder.append(SEPARATOR);
            stringBuilder.append(longitude); stringBuilder.append(SEPARATOR);
            stringBuilder.append(altitude); stringBuilder.append(SEPARATOR);
            stringBuilder.append(distance); stringBuilder.append(SEPARATOR);
            stringBuilder.append(accuracy); stringBuilder.append(SEPARATOR);
            stringBuilder.append(time);
            return stringBuilder.toString();
        }

        public WeatherLocation(final String serializedString){
            if (serializedString!=null){
                String[] items = serializedString.split("\\"+SEPARATOR);
                if (items.length>=8){
                    try {
                        this.description = items[0];
                        this.description_alternate = items[1];
                        this.name = items[2];
                        this.type = Integer.parseInt(items[3]);
                        this.latitude = Double.parseDouble(items[4]);
                        this.longitude = Double.parseDouble(items[5]);
                        this.altitude = Double.parseDouble(items[6]);
                        this.distance = Float.parseFloat(items[7]);
                        this.accuracy = Float.parseFloat(items[8]);
                        this.time = Long.parseLong(items[9]);
                    } catch (Exception e){
                        // do nothing, default values are set.
                    }
                }
            }
        }

        public boolean hasAccuracy(){
            if (accuracy<0){
                return false;
            }
            return true;
        }

        public float getAccuracy(){
            return accuracy;
        }

        public Location toLocation(){
            Location location = new Location(CUSTOMPROVIDER);
            location.setLatitude(this.latitude);
            location.setLongitude(this.longitude);
            location.setAltitude(this.altitude);
            if (hasAccuracy()){
                location.setAccuracy(this.accuracy);
            }
            location.setTime(this.time);
            return location;
        }

        public boolean isInList(ArrayList<WeatherLocation> locations){
            if (locations!=null){
                for (int i=0; i<locations.size(); i++){
                    if (this.name.equals(locations.get(i).name)){
                        return true;
                    }
                }
            }
            return false;
        }

        public static final Creator<WeatherLocation> CREATOR = new Creator<WeatherLocation>() {
            @Override
            public WeatherLocation createFromParcel(Parcel in) {
                return new WeatherLocation(in);
            }

            @Override
            public WeatherLocation[] newArray(int size) {
                return new WeatherLocation[size];
            }
        };

        @Override
        public int compare(WeatherLocation s1, WeatherLocation s2) {
            return s1.description.compareTo(s2.description);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(description);
            parcel.writeString(description_alternate);
            parcel.writeString(name);
            parcel.writeInt(type);
            parcel.writeDouble(latitude);
            parcel.writeDouble(longitude);
            parcel.writeDouble(altitude);
            parcel.writeFloat(distance);
            parcel.writeFloat(accuracy);
            parcel.writeInt(type);
            parcel.writeLong(time);
        }

        public static ArrayList<String> getDescriptions(Context context, ArrayList<WeatherLocation> weatherLocations){
            if (weatherLocations!=null){
                ArrayList<String> descriptions = new ArrayList<String>();
                for (int i=0; i<weatherLocations.size(); i++){
                        descriptions.add(weatherLocations.get(i).getDescription(context));
                }
                return descriptions;
            }
            return null;
        }

        public static ArrayList<String> getNames(ArrayList<WeatherLocation> weatherLocations){
            if (weatherLocations!=null){
                ArrayList<String> names = new ArrayList<String>();
                for (int i=0; i<weatherLocations.size(); i++){
                    names.add(weatherLocations.get(i).name);
                }
                return names;
            }
            return null;
        }

        public int getType(){
            return type;
        }

        public boolean isDMO(){
            if (getType()==RawWeatherInfo.Source.DMO){
                return true;
            }
            return false;
        }

        public boolean isMOS(){
            if (getType()==RawWeatherInfo.Source.MOS){
                return true;
            }
            return false;
        }

        public boolean hasAlternateDescription() {
            if (this.description_alternate.equals(EMPTYVALUE)){
                return false;
            }
            return true;
        }

        public String getName(){
            return this.name;
        }

        public String getOriginalDescription(){
            return this.description;
        }

        public String getDescription(Context context){
            if ((hasAlternateDescription()) && ((this.description.contains("SWIS-PUNKT")) || (WeatherSettings.replaceByMunicipality(context)))){
                return this.description_alternate;
            }
            return this.description;
        }

        public String getDescriptionAlternate(){
            return this.description_alternate;
        }

        public void setName(String s){
            this.name = s;
        }

        public void setDescription(String s){
            this.description = s;
        }

        public void setDescriptionAlternate(String s){
            this.description_alternate = s;
        }

    }

    public static class WeatherLocationFinder implements Runnable{

        private Context context;
        private Location ownLocation;

        public WeatherLocationFinder(Context context, Location location){
            this.context = context;
            this.ownLocation = location;
        }

        @Override
        public void run() {
            StationsManager.StationsReader stationsReader = new StationsManager.StationsReader(context){
                @Override
                public void onLoadingListFinished(ArrayList<Weather.WeatherLocation> new_stations){
                    new_stations = StationsManager.sortStationsByDistance(new_stations, ownLocation);
                    sortedStations(new_stations);
                    closestStation(new_stations.get(0));
                    Weather.WeatherLocation newWeatherLocation = new Weather.WeatherLocation();
                    newWeatherLocation.altitude = ownLocation.getAltitude();
                    newWeatherLocation.latitude = ownLocation.getLatitude();
                    newWeatherLocation.longitude = ownLocation.getLongitude();
                    newWeatherLocation.name = new_stations.get(0).name;
                    newWeatherLocation.description = new_stations.get(0).description;
                    newWeatherLocation.type = new_stations.get(0).type;
                    newWeatherLocation(newWeatherLocation);
                }
            };
            stationsReader.run();
        }

        public void sortedStations(ArrayList<Weather.WeatherLocation> new_stations){

        }

        public void closestStation(WeatherLocation weatherLocation){

        }

        public void newWeatherLocation(WeatherLocation weatherLocation){

        }
    }

    public static class Units {

        public static float KnotsFromKmh(float kmh){
            return kmh*0.539956803456f;
        }

        public static float BeaufortFromKmh(float kmh){
            if (kmh>=118){
                return 12;
            }
            if (kmh>=103){
                return 11;
            }
            if (kmh>=89){
                return 10;
            }
            if (kmh>=75){
                return 9;
            }
            if (kmh>=62){
                return 8;
            }
            if (kmh>=50){
                return 7;
            }
            if (kmh>=39){
                return 6;
            }
            if (kmh>=29){
                return 5;
            }
            if (kmh>=20){
                return 4;
            }
            if (kmh>=12){
                return 3;
            }
            if (kmh>=6){
                return 2;
            }
            if (kmh>=1){
                return 1;
            }
            return 0;
        }

        public static float MSFromKmh(float kmh){
            return kmh*0.277777778f;
        }
    }

    public class WeatherItem{
        long polling_time;
        WeatherLocation location;
        String timetext;    // text; original timestamp
        long timestamp;     // millis; UTC stamp for forecast
        double TTT;         // K; temp. 2 m above surface
        double E_TTT;       // K; absolute error of TTT
        double T5cm;        // K; temp. 5 cm above surface
        double Td;          // K; dew point 2m above surface (Taupunkt)
        double E_Td;        // K; absolute error Td;
        double Tx;          // K; max. temp. during last 12h
        double Tn;          // K; min. temp. during last 12h
        double TM;          // K; mean temp. during last 12h
        double TG;          // K; min. surface temp. at 5 cm within last 12h
        double DD;          // km/h; wind direction 0-360°
        double E_DD;        // 0-360°; absolute error DD
        double FF;          // m/s; wind speed in km/h
        double E_FF;        // m/s; absolute error wind speed 10m above surface
        double FX1;         // m/s; max. wind gust within last hour (Windböen)
        double FX3;         // m/s; max. wind gust within last 3h (Windböen)
        double FXh;         // m/s; max. wind gust within last 12h (Windböen)
        int FXh25;          // %, probability of wind gusts >= 25 km/h within last 12h
        int FXh40;          // %, probability of wind gusts >= 40 km/h within last 12h
        int FXh55;          // %, probability of wind gusts >= 55 km/h within last 12h
        int FX625;          // %, probability of wind gusts >= 25 km/h within last 6h
        int FX640;          // %, probability of wind gusts >= 40 km/h within last 6h
        int FX655;          // %, probability of wind gusts >= 55 km/h within last 6h
        double RR1c;        // kg/m², total precipitation during last hour consistent with significant weather
        double RRL1c;       // kg/m², total liquid during the last hour consistent with significant weather
        double RR3;         // kg/m², total precipitation during last 3h
        double RR6;         // kg/m², total precipitation during last 6h
        double RR3c;        // kg/m², total precipitation during last 3h consistent with significant weather
        double RR6c;        // kg/m², total precipitation during last 6h consistent with significant weather
        double RRhc;        // kg/m², total precipitation during last 12h consistent with significant weather
        double RRdc;        // kg/m², total precipitation during last 24h consistent with significant weather
        double RRS1c;       // kg/m², snow-rain equivalent during last hour
        double RRS3c;       // kg/m², snow-rain equivalent during last 3h
        int R101;           // %, probability of precipitation > 0.1 mm during the last hour
        int R102;           // %, probability of precipitation > 0.2 mm during the last hour
        int R103;           // %, probability of precipitation > 0.3 mm during the last hour
        int R105;           // %, probability of precipitation > 0.5 mm during the last hour
        int R107;           // %, probability of precipitation > 0.7 mm during the last hour
        int R110;           // %, probability of precipitation > 1.0 mm during the last hour
        int R120;           // %, probability of precipitation > 2.0 mm during the last hour
        int R130;           // %, probability of precipitation > 3.0 mm during the last hour
        int R150;           // %, probability of precipitation > 5.0 mm during the last hour
        int RR1o1;          // %, probability of precipitation > 10.0 mm during the last hour
        int RR1w1;          // %, probability of precipitation > 15.0 mm during the last hour
        int RR1u1;          // %, probability of precipitation > 25.0 mm during the last hour
        int R600;           // %, probability of precipitation > 0.0 mm during last 6h
        int Rh00;           // %, probability of precipitation > 0.0 mm during last 12h
        int R602;           // %, probability of precipitation > 0.2 mm during last 6h
        int Rh02;           // %, probability of precipitation > 0.2 mm during last 12h
        int Rd02;           // %, probability of precipitation > 0.2 mm during last 24h
        int R610;           // %, probability of precipitation > 1.0 mm during last gh
        int Rh10;           // %, probability of precipitation > 1.0 mm during last 12h
        int R650;           // %, probability of precipitation > 5.0 mm during last 6h
        int Rh50;           // %, probability of precipitation > 5.0 mm during last 12h
        int Rd00;           // %, probability of precipitation > 0.0 mm during last 24h
        int Rd10;           // %, probability of precipitation > 1.0 mm during last 24h
        int Rd50;           // %, probability of precipitation > 5.0 mm during last 24h
        int wwPd;           // %; occurrance of any precipitation within the last 24h
        double DRR1;        // s, duration of precipitation within the last hour
        int wwZ;            // %, occurrence of drizzle within the last hour
        int wwZ6;           // %, occurrence of drizzle within the last 6h
        int wwZh;           // %, occurrence of drizzle within the last 12h
        int wwD;            // %, occurrence of stratiform precipitation within the last hour
        int wwD6;           // %, occurrence of stratiform precipitation within the last 6h
        int wwDh;           // %, occurrence of stratiform precipitation within the last 12h
        int wwC;            // %, occurrence of convective precipitation within the last hour
        int wwC6;           // %, occurrence of convective precipitation within the last 6h
        int wwCh;           // %, occurrence of convective precipitation within the last 12h
        int wwT;            // %, occurrence of thunderstorms within the last hour
        int wwT6;           // %, occurrence of thunderstorms within the last 6h
        int wwTh;           // %, occurrence of thunderstorms within the last 12h
        int wwTd;           // %, occurrence of thunderstorms within the last 24h
        int wwL;            // %, occurrence of liquid precipitation within the last hour
        int wwL6;           // %, occurrence of liquid precipitation within the last 6h
        int wwLh;           // %, occurrence of liquid precipitation within the last 12h
        int wwS;            // %, occurrence of solid precipitation within the last hour
        int wwS6;           // %, occurrence of solid precipitation within the last 6h
        int wwSh;           // %, occurrence of solid precipitation within the last 12h
        int wwF;            // %, occurrence of freezing rain within the last hour
        int wwF6;           // %, occurrence of freezing rain within the last 6h
        int wwFh;           // %, occurrence of freezing rain within the last 12h
        int wwP;            // %, occurrence of precipitation within the last hour
        int wwP6;           // %, occurrence of precipitation within the last 6h
        int wwPh;           // %, occurrence of precipitation within the last 12h
        int VV10;           // %, probability visibility below 1 km
        int ww;             // significant weather
        int ww3;            // significant weather at last 3h
        int W1W2;           // weather during last 6h
        int WPc11;          // optional significant weather (highest priority) during last 1h
        int WPc31;          // optional significant weather (highest priority) during last 3h
        int WPc61;          // optional significant weather (highest priority) during last 6h
        int WPch1;          // optional significant weather (highest priority) during last 12h
        int WPcd1;          // optional significant weather (highest priority) during last 24h (?)
        int N;              // 0-100% total cloud cover
        int N05;            // % cloud cover below 500ft.
        int Nl;             // % low cloud cover (lower than 2 km)
        int Nm;             // % midlevel cloud cover (2-7 km)
        int Nh;             // % high cloud cover (>7 km)
        int Nlm;            // % cloud cover low and mid level clouds below  7 km
        double H_BsC;       // m; cloud base of convective clouds
        double PPPP;        // Pa, surface pressure reduced
        double E_PPP;       // Pa, absolute error of PPPP
        double RadS3;       // kJ/m²; short wave radiation balance during last 3h
        double RRad1;       // % (0..80); global irradiance within the last hour
        double Rad1h;       // kJ/m²; global irradiance
        double RadL3;       // kJ/m²; long wave radiation balance during last 3h 
        double VV;          // m; visibility
        double D1;          // s; sunshine duration during last hour
        double SunD;        // s; sunshine duration during last day
        double SunD3;       // s; sunshine duration during last 3h
        int RSunD;          // %; relative sunshine duration last 24h
        int PSd00;          // %; probability relative sunshine duration >0% within 24h
        int PSd30;          // %; probability relative sunshine duration >30% within 24h
        int PSd60;          // %; probability relative sunshine duration >60% within 24h
        int wwM;            // %; probability of fog within last hour
        int wwM6;           // %; probability of fog within last 6h
        int wwMh;           // %; probability of fog within last 12h
        int wwMd;           // %; occurrence of fog within the last 24h
        double PEvap;       // kg/m²; potential evapotranspiration within the last 24h
    }

    public final static int DATA_SIZE = 250;

    public static class Clouds{
        private Integer N;          // 0-100% total cloud cover
        private Integer N05;        // % cloud cover below 500ft
        private Integer Nl;         // % low cloud cover (lower than 2 km)
        private Integer Nm;         // % midlevel cloud cover (2-7 km)
        private Integer Nh;         // % high cloud cover (>7 km)
        private Integer Nlm;        // % cloud cover low and mid level clouds below 7 km
        private Double H_BsC;       // m; cloud base of convective clouds
        private Integer Neff;       // 0-100% effective cloud cover

        public Integer[] getIntArray(){
            Integer[] result = new Integer[8];
            result[0] = this.N;
            result[1] = this.N05;
            result[2] = this.Nl;
            result[3] = this.Nm;
            result[4] = this.Nh;
            result[5] = this.Nlm;
            if (this.H_BsC==null){
                result[6] = null;
            } else {
                result[6] = (int) Math.round(this.H_BsC);
            }
            result[7] = this.Neff;
            return result;
        }

        public boolean hasHeightValues(){
            return ((this.N05!=null) && (this.Nl!=null) && (this.Nm!=null) && (this.Nh!=null));
        }
    }

    public static class WeatherInfo{
        private long timestamp;
        private int forecast_type = ForecastType.UNKNOWN;
        private Integer condition_code;
        private boolean condition_is_calculated = false;
        private Double temperature;
        private Double temperature5cm;
        private Double temperature_high;
        private Double temperature_low;
        private Double wind_speed;
        private Double wind_direction;
        private Double flurries;
        private Double precipitationTotal1h;
        private Double precipitation;
        private Integer prob_thunderstorms;
        private Integer prob_precipitation;
        private Integer prob_solid_precipitation;
        private Integer prob_drizzle;
        private Integer prob_freezing_rain;
        private Integer prob_fog;
        private Integer visibility;
        private Integer prob_visibility_below_1km;
        private Double pressure;
        private Double uv;
        private Integer uvHazardIndex;
        private Double td;
        private Integer[] probOfPrecipitation;
        public Clouds clouds;
        private Integer sunDuration;
        private Boolean isDayTime = null;

        final class ForecastType{
            public static final int CURRENT  = 0;
            public static final int ONE_HOUR = 1;
            public static final int HOURS_3  = 2;
            public static final int HOURS_6  = 3;
            public static final int HOURS_12 = 4;
            public static final int HOURS_24 = 5;
            public static final int UNKNOWN  = 128;
        }

        public WeatherInfo(){
            this.clouds = new Clouds();
        }

        public WeatherInfo(int forecast_type){
            this.forecast_type = forecast_type;
            this.clouds = new Clouds();
        }

        public void setTimestamp(long timestamp){
            this.timestamp = timestamp;
        }

        public void setForecastType(int i){
            this.forecast_type = i;
        }

        public void setConditionCode(Integer condition_code){
            this.condition_code = condition_code;
        }

        public void setTemperature(Double temperature){
            this.temperature = temperature;
        }

        public void setTemperature5cm(Double temperature5cm){
            this.temperature5cm = temperature5cm;
        }

        public void setLowTemperature(Double temperature_low){
            this.temperature_low= temperature_low;
        }

        public void setHighTemperature(Double temperature_high){
            this.temperature_high = temperature_high;
        }

        public void setWindSpeed(Double wind_speed){
            this.wind_speed = wind_speed;
        }

        public void setWindDirection(Double wind_direction){
            this.wind_direction = wind_direction;
        }

        public void setFlurries(Double flurries){
            this.flurries = flurries;
        }

        public void setPrecipitationTotal1h(Double precipitationTotal1h){
            this.precipitationTotal1h = precipitationTotal1h;
        }

        public void setPrecipitation(Double precipitation){
            this.precipitation = precipitation;
        }

        public void setClouds(Integer clouds){
            this.clouds.N = clouds;
        }

        public void setClouds_N05(Integer clouds){
            this.clouds.N05 = clouds;
        }

        public void setClouds_Nl(Integer clouds){
            this.clouds.Nl = clouds;
        }

        public void setClouds_Nm(Integer clouds){
            this.clouds.Nm = clouds;
        }

        public void setClouds_Nh(Integer clouds){
            this.clouds.Nh = clouds;
        }

        public void setClouds_Nlm(Integer clouds){
            this.clouds.Nlm = clouds;
        }

        public void setClouds_H_BsC(Double clouds){
            this.clouds.H_BsC = clouds;
        }

        public void setClouds_Neff(Integer clouds){
            this.clouds.Neff = clouds;
        }

        public void setProbThunderstorms(Integer thunderstorms){
            this.prob_thunderstorms = thunderstorms;
        }

        public void setProbPrecipitation(Integer prob_precipitation){
            this.prob_precipitation = prob_precipitation;
        }

        public void setProbDrizzle(Integer prob_precipitation){
            this.prob_drizzle = prob_precipitation;
        }

        public void setProbSolidPrecipitation(Integer prob_solid_precipitation){
            this.prob_solid_precipitation = prob_solid_precipitation;
        }

        public void setProbFreezingRain(Integer prob_freezing_rain){
            this.prob_freezing_rain = prob_freezing_rain;
        }

        public void setProbFog(Integer prob_fog){
            this.prob_fog = prob_fog;
        }

        public void setProbVisibilityBelow1km(Integer prob_visibility_below_1km){
            this.prob_visibility_below_1km = prob_visibility_below_1km;
        }

        public void setVisibility(Integer visibility){
            this.visibility = visibility;
        }

        public void setPressure(Double pressure){
            this.pressure = pressure;
        }

        public void setUV(Double uv){
            this.uv = uv;
        }

        public void setTd(Double td) {
            this.td = td;
        }

        public void setUvHazardIndex(Integer uvHazardIndex){
            this.uvHazardIndex = uvHazardIndex;
        }

        public boolean hasPrecipitationDetails(){
            return probOfPrecipitation != null;
        }

        public void setPrecipitationDetails(Integer[] ints){
           this.probOfPrecipitation = ints;
        }

        public void setSunDuration(Integer i){
            this.sunDuration = i;
        }

        public boolean hasIsDayTime(){
            return !(isDayTime==null);
        }

        public void setIsDayTime(boolean b){
            this.isDayTime = b;
        }

        public Boolean getIsDayTime(){
            return isDayTime;
        }

        public long getTimestamp(){
            return this.timestamp;
        }

        public int getForecastType(){
            return this.forecast_type;
        }

        public boolean hasWindDirection(){
            if (wind_direction!=null){
                return true;
            }
            return false;
        }

        private static float getSweepAngleFromAbsoluteDegrees(float source, float target) {
            /*
                Examples:
                a   b
                350 20 => 30	20 - 350 = -330	b - a + 360
                280 90 => 170   90 - 280 = -190 	b - a + 360
                10  350 => -20   350 - 10 = 340	b - a - 360
                10  40 => 30 	40-10   		b - a
                40  30 => -10 	30-40 			b - a
                180 120 => -60 			b - a
             */
            float sweepangle = target - source;
            if (sweepangle<-180){
                sweepangle = sweepangle + 360;
            } else
            if (sweepangle>180){
                sweepangle = sweepangle - 360;
            }
            return sweepangle;
        }

        private static int getTintColor(int wind_speed){
            Color color = new Color();
            int value = wind_speed*11;
            if (value>255){
                value = 255;
            }
            return Color.rgb(255-value,255-value,255);
        }

        public static Bitmap getWindForecastTint(Bitmap arrowBitmap, ArrayList<WindData> windForecastList){
            float bitmapSize = arrowBitmap.getHeight();
            float lineWidth = bitmapSize / 5f;
            if (arrowBitmap.getWidth()>bitmapSize){
                bitmapSize = arrowBitmap.getWidth();
            }
            int alphaDecay = 180/windForecastList.size();
            int alpha = 255;
            RectF rectF = new RectF(0,0,bitmapSize,bitmapSize);
            RectF rectF2 = new RectF(lineWidth/2,lineWidth/2,bitmapSize-lineWidth/2,bitmapSize-lineWidth/2);
            Bitmap windForecastBitmap = Bitmap.createBitmap(Math.round(rectF.right),Math.round(rectF.bottom),Bitmap.Config.ARGB_8888);
            Canvas windForecastCanvas = new Canvas();
            windForecastCanvas.setBitmap(windForecastBitmap);
            Paint arcPaint = new Paint();
            for (int i=0; i<windForecastList.size()-1; i++){
                arcPaint.setColor(getTintColor((int) (windForecastList.get(i).speed)));
                arcPaint.setAlpha(alpha);
                alpha = alpha - alphaDecay;
                float startAngle = (float) windForecastList.get(i).getDirection();
                float sweepAngle = getSweepAngleFromAbsoluteDegrees(startAngle,(float) windForecastList.get(i+1).getDirection());
                windForecastCanvas.drawArc(rectF,startAngle-90-180,sweepAngle,true,arcPaint);
                Paint blackPaint = new Paint();
                blackPaint.setColor(Color.TRANSPARENT);
                blackPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                windForecastCanvas.drawArc(rectF2,startAngle-90-180,sweepAngle,true,blackPaint);
            }
            Paint xferPaint = new Paint();
            xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            windForecastCanvas.drawBitmap(arrowBitmap,(bitmapSize-arrowBitmap.getWidth())/2f,(bitmapSize-arrowBitmap.getHeight())/2f,xferPaint);
            return windForecastBitmap;
        }

        public Bitmap getArrowBitmap(Context context, boolean fromWidget){
            // wind direction is in ° and is the direction where the wind comes from.
            // new arrow icon neutral position is 0°.
            // rotation is clockwise.
            if (wind_direction!=null){
                int arrowResurce = WeatherIcons.getIconResource(context,WeatherIcons.ARROW);
                // make bitmap mutable
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.mipmap.arrow,options);
                if (bitmap != null){
                    int originalX = bitmap.getWidth();
                    int originalY = bitmap.getHeight();
                    if (fromWidget) {
                        ThemePicker.applyColor(bitmap,ThemePicker.getWidgetTextColor(context));
                    } else {
                        ThemePicker.applyColor(bitmap,ThemePicker.getColorTextLight(context));
                    }
                    Matrix m = new Matrix();
                    m.postRotate(wind_direction.floatValue());
                    // draw the wind forecast
                    bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,false);
                    int deltaX = bitmap.getWidth() - originalX;
                    int deltaY = bitmap.getHeight() - originalY;
                    bitmap = Bitmap.createBitmap(bitmap,deltaX/2,deltaY/2,originalX,originalY);
                    return bitmap;
                }
            }
            return null;
        }

        public Bitmap getBeaufortBitmap(Context context, boolean fromWidget){
            // fall back to arrow if wind speed is unknown
            if (wind_speed==null){
                return getArrowBitmap(context,fromWidget);
            }
            if (wind_direction!=null) {
                // make bitmap mutable
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), getBeaufortIconResourceID(context,getWindSpeedInBeaufortInt()),options);
                if (bitmap != null) {
                    int originalX = bitmap.getWidth();
                    int originalY = bitmap.getHeight();
                    if (fromWidget) {
                        ThemePicker.applyColor(bitmap,ThemePicker.getWidgetTextColor(context));
                    } else {
                        ThemePicker.applyColor(bitmap,ThemePicker.getColorTextLight(context));
                    }
                    Matrix m = new Matrix();
                    m.postRotate(wind_direction.floatValue());
                    // draw the wind forecast
                    bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,false);
                    return bitmap;
                } else {
                    return null;
                }
            }
            return null;
        }

        private int getBeaufortIconResourceID(Context context, int windspeed_beaufort){
            switch (windspeed_beaufort){
                case 0: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_00);
                case 1: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_01);
                case 2: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_02);
                case 3: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_03);
                case 4: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_04);
                case 5: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_05);
                case 6: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_06);
                case 7: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_07);
                case 8: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_08);
                case 9: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_09);
                case 10: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_10);
                case 11: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_11);
                default: return WeatherIcons.getIconResource(context,WeatherIcons.WIND_BEAUFORT_12);
            }
        }

        public String getWindDirectionString(Context context){
            if (wind_direction!=null){
                if ((wind_direction>337.5) || (wind_direction<=22.5)){
                    return context.getResources().getString(R.string.direction_north);
                }
                if ((wind_direction>22.5) && (wind_direction<=67.5)){
                    return context.getResources().getString(R.string.direction_northeast);
                }
                if ((wind_direction>67.5) && (wind_direction<=112.5)){
                    return context.getResources().getString(R.string.direction_east);
                }
                if ((wind_direction>112.5) && (wind_direction<=157.5)){
                    return context.getResources().getString(R.string.direction_southeast);
                }
                if ((wind_direction>157.5) && (wind_direction<=202.5)){
                    return context.getResources().getString(R.string.direction_south);
                }
                if ((wind_direction>202.5) && (wind_direction<=247.5)){
                    return context.getResources().getString(R.string.direction_southwest);
                }
                if ((wind_direction>247.5) && (wind_direction<=292.5)){
                    return context.getResources().getString(R.string.direction_west);
                }
                if ((wind_direction>292.5) && (wind_direction<=337.5)) {
                    return context.getResources().getString(R.string.direction_northwest);
                }
            }
          return "?";
        }

        public Bitmap getWindSymbol(Context context, int windDisplayType, boolean fromWidget){
            float width_bitmap = 256;
            float height_bitmap = 256;
            if (windDisplayType==WindDisplayType.BEAUFORT){
                return getBeaufortBitmap(context,fromWidget);
            }
            if (windDisplayType==WindDisplayType.TEXT){
                Bitmap bitmap = Bitmap.createBitmap(Math.round(width_bitmap),Math.round(height_bitmap), Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.TRANSPARENT);
                String windsting = getWindDirectionString(context);
                float max_fontsize = LargeWidget.getMaxPossibleFontsize(windsting,width_bitmap,height_bitmap,null);
                Paint paint = new Paint();
                paint.setColor(MainActivity.getColorFromResource(context,R.attr.colorText));
                paint.setTextSize(max_fontsize);
                // center text vertically & horizontally
                float x_offset     = (width_bitmap-paint.measureText(windsting))/2;
                float y_offset     = (height_bitmap - paint.getTextSize())/2 + paint.getTextSize();
                Canvas canvas = new Canvas(bitmap);
                canvas.drawText(windsting,x_offset,y_offset,paint);
                return bitmap;
            }
            return getArrowBitmap(context,fromWidget);
        }

        public String getWindUnitString(Context context){
            String unit = "km/h";
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.METERS_PER_SECOND){
                unit = "m/s";
            }
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.KNOTS){
                unit = "kn";
            }
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.BEAUFORT){
                unit = "bf";
            }
            return unit;

        }

        public String getWindSpeedString(Context context, boolean unit){
            String windspeedstring = String.valueOf(getWindSpeedInKmhInt());
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.KNOTS){
                windspeedstring = String.valueOf(getWindSpeedInKnotsInt());
            }
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.BEAUFORT){
                windspeedstring = String.valueOf(getWindSpeedInBeaufortInt());
            }
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.METERS_PER_SECOND){
                windspeedstring = String.valueOf(getWindSpeedInMsInt());
            }
            if (unit){
                windspeedstring = windspeedstring + getWindUnitString(context);
            }
            return windspeedstring;
        }

        public String getFlurriesString(Context context, boolean unit){
            String windspeedstring = String.valueOf(getFlurriesInKmhInt());
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.KNOTS){
                windspeedstring = String.valueOf(getFlurriesInKnotsInt());
            }
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.BEAUFORT){
                windspeedstring = String.valueOf(getFlurriesInBeaufortInt());
            }
            if (WeatherSettings.getWindDisplayUnit(context)==WindDisplayUnit.METERS_PER_SECOND){
                windspeedstring = String.valueOf(getFlurriesInMsInt());
            }
            if (unit){
                windspeedstring = windspeedstring + " " + getWindUnitString(context);
            }
            return windspeedstring;
        }

        public double getWindDirection(){
            return wind_direction;
        }

        public int getWindDirectionInt(){
            int j = (int) Math.round(wind_direction);
            return j;
        }

        public boolean hasCondition(){
            if (condition_code!=null){
                if (condition_code==WeatherCodeContract.NOT_AVAILABLE){
                    return false;
                }
                return true;
            }
            return false;
        }

        public int getCondition(){
            return condition_code;
        }

        public boolean hasTemperature(){
            if (temperature!=null){
                return true;
            }
            return false;
        }

        public double getTemperature(){
            return temperature;
        }

        public int getTemperatureInt(){
            int j = (int) Math.round(temperature);
            return j;
        }

        public double getTemperatureInCelsius(){
            Double d = (temperature - KelvinConstant);
            return d;
        }
        public int getTemperatureInCelsiusInt(){
            Double d = (temperature - KelvinConstant);
            int j = (int) Math.round(d);
            return j;
        }

        public boolean hasTemperature5cm(){
            if (temperature5cm!=null){
                return true;
            } else {
                return false;
            }
        }

        public int getTemperture5cmInt(){
            int j = (int) Math.round(temperature5cm);
            return j;
        }

        public double getTemperature5cmInCelsius(){
            Double d = (temperature5cm-KelvinConstant);
            return d;
        }

        public int getTemperature5cmInCelsiusInt(){
            Double d = (temperature5cm-KelvinConstant);
            int j = (int) Math.round(d);
            return j;
        }

        public boolean hasMaxTemperature(){
            if (temperature_high!=null){
                return true;
            }
            return false;
        }

        public int getMaxTemperatureInt(){
            int j = (int) Math.round(temperature_high);
            return j;
        }

        public double getMaxTemperatureInCelsius(){
            return (temperature_high - KelvinConstant);
        }

        public int getMaxTemperatureInCelsiusInt(){
            Double d = getMaxTemperatureInCelsius();
            int j = (int) Math.round(d);
            return j;
        }

        public boolean hasMinTemperature(){
            if (temperature_low!=null){
                return true;
            }
            return false;
        }

        public double getMinTemperature(){
            return temperature_low;
        }

        public int getMinTemperatureInt(){
            int j = (int) Math.round(temperature_low);
            return j;
        }

        public int getMinTemperatureInCelsiusInt(){
            Double d = temperature_low - KelvinConstant;
            int j = (int) Math.round(d);
            return j;
        }

        public String getMinMaxTemperatureInCelsiusIntString(boolean useSpaces){
            StringBuilder result = new StringBuilder();
            if (hasMinTemperature()){
                result.append(getMinTemperatureInCelsiusInt());
                result.append("°");
            } else {
                result.append("-");
            }
            if (useSpaces){
                result.append(" ");
            }
            result.append("|");
            if (useSpaces){
                result.append(" ");
            }
            if (hasMaxTemperature()){
                result.append(getMaxTemperatureInCelsiusInt());
                result.append("°");
            } else {
                result.append("-");
            }
            return result.toString();
        }

        public boolean hasPrecipitation(){
            if (precipitation!=null){
                return true;
            }
            return false;
        }

        public double getPrecipitationTotal1h(Double precipitationTotal1h){
            return precipitationTotal1h;
        }

        public double getPrecipitation(){
            return precipitation;
        }

        public String getPrecipitationIntervalString(){
            if ((forecast_type==ForecastType.CURRENT) || (forecast_type==ForecastType.ONE_HOUR)){
                return "h";
            }
            if (forecast_type==ForecastType.HOURS_3){
                return "3h";
            }
            if (forecast_type==ForecastType.HOURS_6){
                return "6h";
            }
            if (forecast_type==ForecastType.HOURS_12){
                return "12h";
            }
            if (forecast_type==ForecastType.HOURS_24){
                return "24h";
            }
            return "";
        }

        public String getPrecipitationString(){
            if (precipitation!=null){
                return String.valueOf(precipitation)+" kg/m²/"+getPrecipitationIntervalString();
            } else {
                return "-";
            }
        }

        public String getPrecipitationUnitLower(){
            return "m²/"+getPrecipitationIntervalString();
        }

        public boolean hasProbPrecipitation(){
            if (prob_precipitation!=null){
                return true;
            }
            return false;
        }

        public int getProbPrecipitation(){
            return prob_precipitation;

        }

        public boolean hasProbDrizzle(){
            if (prob_drizzle!=null){
                return true;
            }
            return false;
        }

        public int getProbDrizzle(){
            return prob_drizzle;
        }

        public boolean hasWindSpeed(){
            if (wind_speed!=null){
                return true;
            }
            return false;
        }

        public int getWindSpeedInMsInt(){
            Double d = wind_speed;
            return (int) Math.round(d);
        }

        public int getWindSpeedInKmhInt(){
            Double d = (wind_speed*3.6);
            int speed = (int) Math.round(d);
            return speed;
        }

        private int fromKmhToBeaufort(int wind_kmh){
            int beauford = 12;
            if (wind_kmh<=117){
                beauford = 11;
            }
            if (wind_kmh<=102){
                beauford = 10;
            }
            if (wind_kmh<=88){
                beauford = 9;
            }
            if (wind_kmh<=74){
                beauford = 8;
            }
            if (wind_kmh<=61){
                beauford = 7;
            }
            if (wind_kmh<=49){
                beauford = 6;
            }
            if (wind_kmh<=38){
                beauford = 5;
            }
            if (wind_kmh<=28){
                beauford = 4;
            }
            if (wind_kmh<=19){
                beauford = 3;
            }
            if (wind_kmh<=11){
                beauford = 2;
            }
            if (wind_kmh<=5){
                beauford = 1;
            }
            if (wind_kmh<=1){
                beauford = 0;
            }
            return beauford;
        }

        public int getWindSpeedInBeaufortInt(){
            return fromKmhToBeaufort(getWindSpeedInKmhInt());
        }

        public int getWindSpeedInKnotsInt(){
            Double d = wind_speed*1.943844;
            return (int) Math.round(d);
        }

        public boolean hasFlurries(){
            if (flurries!=null){
                return true;
            }
            return false;
        }

        public int getFlurriesInMsInt(){
            Double d = flurries;
            return (int) Math.round(d);
        }

        public int getFlurriesInKmhInt(){
            Double d = (flurries*3.6);
            int flurries = (int) Math.round(d);
            return flurries;
        }

        public int getFlurriesInBeaufortInt(){
            return fromKmhToBeaufort(getFlurriesInKmhInt());
        }

        public int getFlurriesInKnotsInt(){
            Double d = flurries*1.943844;
            return (int) Math.round(d);
        }

        public boolean hasClouds(){
            if (clouds.N!=null){
                return true;
            }
            return false;
        }

        public int getClouds(){
            return clouds.N;
        }

        public boolean hasClouds_N05(){
            if (clouds.N05!=null){
                return true;
            }
            return false;
        }

        public int getClouds_N05(){
            return clouds.N05;
        }

        public boolean hasClouds_Nl(){
            if (clouds.Nl!=null){
                return true;
            }
            return false;
        }

        public int getClouds_Nl(){
            return clouds.Nl;
        }

        public boolean hasClouds_Nm(){
            if (clouds.Nm!=null){
                return true;
            }
            return false;
        }

        public int getClouds_Nm(){
            return clouds.Nm;
        }

        public boolean hasClouds_Nh(){
            if (clouds.Nh!=null){
                return true;
            }
            return false;
        }

        public int getClouds_Nh(){
            return clouds.Nh;
        }

        public boolean hasClouds_Nlm(){
            if (clouds.Nlm!=null){
                return true;
            }
            return false;
        }

        public int getClouds_Nlm(){
            return clouds.Nlm;
        }

        public boolean hasClouds_H_BsC(){
            if (clouds.H_BsC!=null){
                return true;
            }
            return false;
        }

        public double getClouds_H_BsC(){
            return clouds.H_BsC;
        }

        public int getClouds_Neff(){
            return clouds.Neff;
        }

        public boolean hasProbThunderstorms(){
            if (prob_thunderstorms!=null){
                return true;
            }
            return false;
        }

        public int getProbThunderStorms(){
            return prob_thunderstorms;
        }

        public boolean hasProbSolidPrecipitation(){
            if (prob_solid_precipitation!=null){
                return true;
            }
            return false;
        }

        public int getProbSolidPrecipitation(){
            return prob_solid_precipitation;
        }

        public boolean hasProbFreezingRain(){
            if (prob_freezing_rain!=null){
                return true;
            }
            return false;
        }

        public int getProbFreezingRain(){
            return prob_freezing_rain;
        }

        public boolean hasProbFog(){
            if (prob_fog!=null){
                return true;
            }
            return false;
        }

        public int getProbFog(){
            return prob_fog;
        }

        public boolean hasVisibility(){
            if (visibility!=null){
                return true;
            }
            return false;
        }

        public int getVisibilityInMetres(){
            return visibility;
        }

        public double getVisibilityInNauticMiles(){
            Double d = (double) visibility;
            Double nm = d / 1852;
            return nm;
        }

        public double getVisibilityInMiles(){
            Double d = (double) visibility;
            Double nm = d / 1609.344;
            return nm;
        }

        public double getVisibilityInYards(){
            Double d = (double) visibility;
            Double nm = d * 0.9144;
            return nm;
        }

        public boolean hasProbVisibilityBelow1km(){
            return prob_visibility_below_1km!=null;
        }

        public int getProbVisibilityBelow1km(){
            return prob_visibility_below_1km;
        }

        public boolean hasPressure() {
            return pressure!=null;
        }

        public int getPressure(){
            int p = (int) Math.round(pressure);
            return p;
        }

        public boolean hasUV(){
            if (uv!=null){
                return true;
            }
            return false;
        }

        public int getUV(){
            int j = (int) Math.round(uv);
            return j;
        }

        public boolean hasTd(){
            return td != null;
        }

        public double getTd(){
            return td;
        }

        public int getTdInCelsiusInt(){
            double value = td - KelvinConstant;
            return (int) Math.round(value);
        }

        public boolean hasRH(){
            if ((temperature==null) || (td==null)){
                return false;
            }
            return true;
        }

        public double getRH(){
            final double rh_c2 = 17.5043;
            final double rh_c3 = 241.2;
            double rh = 100*Math.exp((rh_c2*td/(rh_c3+td))-(rh_c2*temperature/(rh_c3+temperature)));
            return rh;
        }

        public int getRHInt(){
            return (int) Math.round(getRH());
        }

        public boolean isDaytime(WeatherLocation weatherLocation){
            // this is to minimize daytime calculations
            if (this.isDayTime==null){
                if (forecast_type==ForecastType.ONE_HOUR){
                    this.isDayTime = Weather.isDaytime(weatherLocation,timestamp);
                } else
                if (forecast_type==ForecastType.HOURS_6){
                    boolean startIsDaytime = Weather.isDaytime(weatherLocation,timestamp-1000*60*60*6);
                    boolean stopIsDaytime = Weather.isDaytime(weatherLocation,timestamp);
                    this.isDayTime = (startIsDaytime || stopIsDaytime);
                } else
                if (forecast_type==ForecastType.HOURS_24){
                    this.isDayTime = true;
                } else this.isDayTime = true;
            }
            return this.isDayTime;
        }

        public boolean calculateMissingCondition(){
            if (condition_code==null){
                condition_code = WeatherCodeContract.calculateCustomWeatherconditionFromData(this);
                if (condition_code != WeatherCodeContract.NOT_AVAILABLE){
                    this.condition_is_calculated = true;
                    return true;
                }
            }
            return false;
        }

        public boolean isConditionCalculated(){
            return condition_is_calculated;
        }

        public Integer[] getPrecipitationDetails(){
            return this.probOfPrecipitation;
        }

        public boolean hasSunDuration(){
            return this.sunDuration != null;
        }

        public int getSunDurationInSeconds(){
            return this.sunDuration;
        }

        public int getSunDurationInMinutes(){
            return getSunDurationInSeconds()/60;
        }

        public int getSunDurationInHours(){
            return Math.round(getSunDurationInSeconds()/(60f*60f));
        }

        public int getRelativeDay(){
            return WeatherLayer.getRelativeDays(getTimestamp());
        };

        public boolean hasUvHazardIndex(){
            if (uvHazardIndex==null){
                return false;
            }
            if (uvHazardIndex==-1){
                return false;
            }
            return true;
        }

        public int getUvHazardIndex(){
            if (hasUvHazardIndex()){
                return this.uvHazardIndex;
            } else {
                return -1;
            }
        }

    }

    public static CurrentWeatherInfo getCurrentWeatherInfo(Context context, int updateSource){
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        //WeatherSettings weatherSettings = new WeatherSettings(context);
        //String station_name = weatherSettings.station_name;
        String station_name = WeatherSettings.getSetStationLocation(context).name;
        Cursor cursor;
        String[] selectionArg={station_name};
        String orderBy=""+WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_polling_time+" DESC";
        cursor = contentResolver.query(WeatherContentManager.FORECAST_URI_ALL,
                null,WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_name+" = ?",selectionArg,orderBy);
        // read only fist element. Database should not hold more than one data set for one station.
        // Should there be more, take the most recent by polling time. The most recent entry is at position 1.
        if (cursor.moveToFirst()){
            CurrentWeatherInfo currentWeatherInfo = WeatherContentManager.getWeatherInfo(context,cursor);
            return currentWeatherInfo;
        }
        cursor.close();
        return null;
    }

    public static boolean hasUVHIData(Context context){
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        String station_name = WeatherSettings.getSetStationLocation(context).name;
        Cursor cursor;
        String[] selectionArg={station_name};
        String[] projectionArg={WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_UVHazardIndex};
        String orderBy=""+WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_polling_time+" DESC";
        cursor = contentResolver.query(WeatherContentManager.FORECAST_URI_ALL,
                projectionArg,WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_name+" = ?",selectionArg,orderBy);
        // read only fist element. Database should not hold more than one data set for one station.
        // Should there be more, take the most recent by polling time. The most recent entry is at position 1.
        if (cursor.moveToFirst()){
            // get the uvhi strings only
            String[] uvhiStringArray = WeatherContentManager.deSerializeString(cursor.getString(cursor.getColumnIndex(WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_UVHazardIndex)));
            // check if very first element is -1 or something else
            if (uvhiStringArray!=null){
                if (uvhiStringArray.length>0){
                    if (!uvhiStringArray[0].equals("-1")){
                        return true;
                    }
                }
            }
            return false;
        }
        cursor.close();
        return false;
    }

    public static long getPollingTime(Context context){
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        String station_name = WeatherSettings.getSetStationLocation(context).name;
        Cursor cursor;
        String[] selectionArg={station_name};
        String[] projectionArg={WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_polling_time};
        String orderBy=""+WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_polling_time+" DESC";
        cursor = contentResolver.query(WeatherContentManager.FORECAST_URI_ALL,
                projectionArg,WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_name+" = ?",selectionArg,orderBy);
        // read only fist element. Database should not hold more than one data set for one station.
        // Should there be more, take the most recent by polling time. The most recent entry is at position 1.
        if (cursor.moveToFirst()){
            // get the uvhi strings only
            long pollingTime = cursor.getLong(cursor.getColumnIndex(WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_polling_time));
            return pollingTime;
        }
        cursor.close();
        return 0;
    }

    public static void sanitizeDatabase(Context context){
        ContentResolver contentResolver = context.getContentResolver();
        String selectionWeather  = WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_polling_time+"<?";
        String selectionTexts    = WeatherContentProvider.WeatherDatabaseHelper.KEY_TEXTS_polled+"<?";
        String selectionWarnings = WeatherContentProvider.WeatherDatabaseHelper.KEY_WARNINGS_polling_time+"<?";
        String[] selectionArgs = {String.valueOf(Calendar.getInstance().getTimeInMillis()-1000*60*60*24*14)};  // anything polled prior to 14 days
        int rowsWeather  = contentResolver.delete(WeatherContentManager.FORECAST_URI_ALL,selectionWeather, selectionArgs);
        int rowsTexts    = contentResolver.delete(WeatherContentManager.TEXT_URI_ALL,selectionTexts, selectionArgs);
        int rowsWarnings = contentResolver.delete(WeatherContentManager.WARNING_URI_ALL,selectionWarnings, selectionArgs);
        PrivateLog.log(context,PrivateLog.UPDATER,PrivateLog.INFO,"Deleted old entries: "+rowsWeather+" forecasts, "+rowsTexts+" texts, "+rowsWarnings+" warnings older than 14 days.");
    }

    public static final String[] SQL_COMMAND_QUERYTIMECOLUMN = {WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_timestamp,
                                                                WeatherContentProvider.WeatherDatabaseHelper.KEY_FORECASTS_name};

    private static ArrayList<RawWeatherInfo> getTimestampArrayList(Context context) {
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        ArrayList<RawWeatherInfo> dataArrayList = new ArrayList<RawWeatherInfo>();
        Cursor c = null;
        try {
            c = contentResolver.query(WeatherContentManager.FORECAST_URI_ALL,SQL_COMMAND_QUERYTIMECOLUMN,null,null,null);
            if (c.moveToFirst()) {
                do {
                    RawWeatherInfo rawWeatherInfo = WeatherContentManager.getRawWeatherInfoFromCursor(c);
                    dataArrayList.add(rawWeatherInfo);
                } while (c.moveToNext());
            }
        } catch (Exception SQLiteException){
            // onCreate(sql_db);
        }
        if (c!=null)
            c.close();
        return dataArrayList;
    }

    private static double getJulianDay(long time){
        Calendar c1 = Calendar.getInstance();
        c1.clear();
        c1.set(Calendar.YEAR,2020);
        c1.set(Calendar.MONTH,0);
        c1.set(Calendar.DAY_OF_MONTH,0);
        c1.set(Calendar.HOUR,0);
        c1.set(Calendar.MINUTE,0);
        c1.set(Calendar.SECOND,0);
        c1.set(Calendar.MILLISECOND,0);
        double days_since_2020 = TimeUnit.DAYS.convert(time - c1.getTimeInMillis(),TimeUnit.MILLISECONDS);
        double julian_days = 2458849.41667 + days_since_2020; // 2458849.41667 = 01.01.2020 00:00:00
        //double julian_days = 2458849 + days_since_2020; // 2458849.41667 = 01.01.2020 00:00:00
        return julian_days;
    }

    public static Astronomy.Riseset getRiseset(Weather.WeatherLocation weatherLocation, long time){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(time);
        int zone = ((calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) /(1000*60*60));
        Astronomy.Riseset riseset = Astronomy.sunRise(getJulianDay(time),
                DELTA_T,
                Math.toRadians(weatherLocation.longitude),
                Math.toRadians(weatherLocation.latitude),
                zone,false);
        return riseset;
    }

    public static Astronomy.Riseset getMoonRiseset(Weather.WeatherLocation weatherLocation, long time){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(time);
        int zone = ((calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) /(1000*60*60));
        Astronomy.Riseset riseset = Astronomy.moonRise(getJulianDay(time),
                DELTA_T,
                Math.toRadians(weatherLocation.longitude),
                Math.toRadians(weatherLocation.latitude),
                zone,false);
        return riseset;
    }

    public static boolean isMoonTime(Weather.WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = getRiseset(weatherLocation,time);
        long moonRise = getMoonRiseInUTC(riseset,time);
        long moonSet  = getMoonSetInUTC(riseset,time);
        if (moonRise<moonSet){
            if ((time>=moonRise) && (time<moonSet)){
                return true;
            }
        }
        if (moonSet<=moonRise){
            if (!((time>=moonSet) && (time<=moonRise))){
                return true;
            }
        }
        return false;
    }

    public static long getMidnightTime(long time){
        GregorianCalendar c1 = new GregorianCalendar();
        c1.set(Calendar.HOUR,0);
        c1.set(Calendar.MINUTE,0);
        c1.set(Calendar.SECOND,0);
        c1.set(Calendar.MILLISECOND,0);
        return c1.getTimeInMillis();
    }

    public static long getNextMidnightTime(long time){
        long mtime = getMidnightTime(time);
        GregorianCalendar c1 = new GregorianCalendar();
        c1.roll(Calendar.DAY_OF_MONTH,1);
        return c1.getTimeInMillis();
    }

    private static long getVisibleDuration(WeatherLocation weatherLocation, long rise, long set, long nextSet){
        // set is always after rise.
        // However, the Astromomy class gives set & rise of the same day.
        // if set is before rise in utc:
        // -> in this case, move set 24h forward to make it match again for calculations.
        long correctedSet = set;
        if (set<rise){
            correctedSet = nextSet;
       }
        return correctedSet-rise;
    }

    private static float getApproximatePositionOnSky(WeatherLocation weatherLocation, long rise, long set, long nextSet, long time){
        long visibleDuration = getVisibleDuration(weatherLocation,rise,set,nextSet);
        // check if time is between rise & set
        if (((set>rise) && (time>=rise) && (time<=set)) ||
           ((set<rise) && ((time<set) || (time>rise)))){
            // set is always after rise.
            // However, the Astromomy class gives set & rise of the same day.
            // if set is before rise in utc:
            // -> in this case, move time 24h forward to make it match again for calculations.
            long correctedTime = time;
            if ((set<rise) && (time<rise)){
                correctedTime = time + 1000*60*60*24; // 24h in millis
            }
            float result = ((correctedTime-rise)/(float) visibleDuration);
            return result;
        }
        return -1;
    }

    private static float getRawApproxMoonPositionOnSky(Weather.WeatherLocation weatherLocation, long time) {
        Astronomy.Riseset riseset = getMoonRiseset(weatherLocation, time);
        long rise = getMoonRiseInUTC(riseset, time);
        long set = getMoonSetInUTC(riseset, time);
        Astronomy.Riseset riseset2 = getMoonRiseset(weatherLocation,rise+1000*60*60*24);
        long nextSet = getMoonSetInUTC(riseset2,rise+1000*60*60*24);
        float approximatePosition = getApproximatePositionOnSky(weatherLocation,rise,set,nextSet,time);
        return approximatePosition;
    }

    /**
     * Determines the approximate position of the moon on the sky based on rise and set times.
     * Note: to eliminate possible time shifts between rise sets of different days, in most cases this calculation
     *       is averaged over three hours.
     *
     * @param weatherLocation the location to calculate the value for
     * @param time the time in UTC (milliseconds)
     * @return the approximate position, where 0 is rise and 1 is set. Returns -1 if the moon is
     * not visible at the time.
     */

    public static float getApproxMoonPositionOnSky(Weather.WeatherLocation weatherLocation, long time) {
        float moonPositionOnSky = Weather.getRawApproxMoonPositionOnSky(weatherLocation, time);
        if (moonPositionOnSky!=-1){
            float moonPositionOnSky0 = Weather.getRawApproxMoonPositionOnSky(weatherLocation, time-1000*60*60);
            float moonPositionOnSky1 = Weather.getRawApproxMoonPositionOnSky(weatherLocation, time+1000*60*60);
            if ((moonPositionOnSky0!=-1) && (moonPositionOnSky1!=-1)){
                moonPositionOnSky = (moonPositionOnSky0 + +moonPositionOnSky+ moonPositionOnSky1)/3;
            }
        }
        return moonPositionOnSky;
    }

    private static float getRawApproxSunPositionOnSky(Weather.WeatherLocation weatherLocation, long time) {
        Astronomy.Riseset riseset = getRiseset(weatherLocation,time);
        long rise = getSunriseInUTC(riseset,time);
        long set  = getSunsetInUTC(riseset,time);
        Astronomy.Riseset riseset2 = getRiseset(weatherLocation,rise+1000*60*60*24);
        long nextSet = getSunsetInUTC(riseset2,rise+1000*60*60*24);
        float approximatePosition = getApproximatePositionOnSky(weatherLocation,rise,set,nextSet,time);
        return approximatePosition;
    }

    /**
     * Determines the approximate position of the sun on the sky based on sunrise and sunset times.
     * Note: to eliminate possible time shifts between rise sets of different days, in most cases this calculation
     *       is averaged over three hours.
     *
     * @param weatherLocation the location to calculate the value for
     * @param time the time in UTC (milliseconds)
     * @return the approximate position, where 0 is sunrise, 0.5 is midday and 1 is sunset. Returns -1 if the sun is
     * not visible at the time.
     */

    public static float getApproxSunPositionOnSky(Weather.WeatherLocation weatherLocation, long time) {
        float sunPositionOnSky = getRawApproxSunPositionOnSky(weatherLocation,time);
        if (sunPositionOnSky!=-1){
            float sunPositionOnSky0 = getRawApproxSunPositionOnSky(weatherLocation,time-1000*60*60);
            float sunPositionOnSky1 = getRawApproxSunPositionOnSky(weatherLocation,time+1000*60*60);
            if ((sunPositionOnSky0!=-1) && (sunPositionOnSky1!=-1)){
                sunPositionOnSky = (sunPositionOnSky0 + sunPositionOnSky + sunPositionOnSky1)/3;
            }
        }
        return sunPositionOnSky;
    }


    public static boolean isDaytime(Weather.WeatherLocation weatherLocation, long time){
        if (usePreciseIsDaytime(weatherLocation)){
            Astronomy.Riseset riseset = getRiseset(weatherLocation,time);
            long sunRise = getSunriseInUTC(riseset,time);
            long sunSet  = getSunsetInUTC(riseset,time);
            if (sunRise<sunSet){
                if ((time>=sunRise) && (time<sunSet)){
                    return true;
                }
            }
            if (sunSet<=sunRise){
                if (!((time>=sunSet) && (time<=sunRise))){
                    return true;
                }
            }
            return false;
        } else {
            // simple, static formula
            // daytime = 6:00 - 19:00
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            return (hour > 6) && (hour <= 19);
        }
    }

    /**
     * Returns if a precise calculation of day/night time with the Astronomy class makes sense. It
     * makes sense between a latutide of -65° to +65°, but not further south or north.
     *
     * @param weatherLocation
     * @return
     */

    public static boolean usePreciseIsDaytime(WeatherLocation weatherLocation){
        if ((weatherLocation.latitude<-65) || (weatherLocation.latitude>65)){
            return false;
        }
        return true;
    }

    /**
     * Takes a Astronomy.Riseset value and the reference time (UTC in millis) to calculate the Riseset and constructs
     * a valid Calendar instance that represents the riseset result.
     *
     * E.g., passing riseset.set and time (utc) results in a milliseconds value in utc that represents the exact time
     * of the sunset.
     *
     * @param risetime this is one of the Astronomy.Riseset values (double), e.g. set, rise, cicilTwilightMorning etc.
     * @param time     reference time that was used to calculate the Astronomy.Riseset, it is used to determine
     *                 the date.
     * @return         time in millis (utc) when the Riseset event occurs
     *
     * Note: the Riseset already handles day saving time well. This function basically takes the day from the time
     * value and overrides hour of day, minute, second and millisecond with the appropriate Riseset time.
     */

    private static long setRiseTimeToDay(double risetime, long time){
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.setTimeInMillis(time);
        long risevalue = Math.round(risetime*MILLIS_IN_HOUR);
        long hours = TimeUnit.MILLISECONDS.toHours(risevalue);
        risevalue = risevalue - TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(risevalue);
        risevalue = risevalue - TimeUnit.MINUTES.toMillis(minutes);
        c.set(Calendar.HOUR_OF_DAY,(int) hours);
        c.set(Calendar.MINUTE,(int) minutes);
        c.set(Calendar.MILLISECOND,(int) risevalue);
        return c.getTimeInMillis();
    }

    public static long getSunsetInUTC(Astronomy.Riseset riseset, long time){
        return setRiseTimeToDay(riseset.set,time);
    }

    public static long getSunsetInUTC(WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = Weather.getRiseset(weatherLocation,time);
        return getSunsetInUTC(riseset,time);
    }

    public static long getSunsetInUTC(WeatherLocation weatherLocation, WeatherInfo weatherInfo){
        return getSunsetInUTC(weatherLocation, weatherInfo.getTimestamp());
    }

    public static long getSunriseInUTC(Astronomy.Riseset riseset, long time){
        return setRiseTimeToDay(riseset.rise,time);
    }

    public static long getSunriseInUTC(WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = Weather.getRiseset(weatherLocation,time);
        return getSunriseInUTC(riseset,time);
    }

    public static long getSunriseInUTC(WeatherLocation weatherLocation, WeatherInfo weatherInfo){
        return getSunriseInUTC(weatherLocation,weatherInfo.getTimestamp());
    }


    public static long getCivilTwilightMorning(Astronomy.Riseset riseset, long time){
        return setRiseTimeToDay(riseset.cicilTwilightMorning,time);
    }

    public static long getCivilTwilightMorning(WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = Weather.getRiseset(weatherLocation,time);
        return getCivilTwilightMorning(riseset,time);
    }

    public static long getCivilTwilightMorning(WeatherLocation weatherLocation, WeatherInfo weatherInfo){
        return getCivilTwilightMorning(weatherLocation,weatherInfo.getTimestamp());
    }

    public static long getCivilTwilightEvening(Astronomy.Riseset riseset, long time){
        return setRiseTimeToDay(riseset.cicilTwilightEvening,time);
    }

    public static long getCivilTwilightEvening(WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = Weather.getRiseset(weatherLocation,time);
        return getCivilTwilightEvening(riseset,time);
    }

    public static long getCivilTwilightEvening(WeatherLocation weatherLocation, WeatherInfo weatherInfo){
        return getCivilTwilightEvening(weatherLocation,weatherInfo.getTimestamp());
    }

    public static long getMoonRiseInUTC(Astronomy.Riseset riseset, long time){
        return setRiseTimeToDay(riseset.rise,time);
    }

    public static long getMoonRiseInUTC(WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = Weather.getMoonRiseset(weatherLocation,time);
        return getMoonRiseInUTC(riseset,time);
    }

    public static long getMoonRiseInUTC(WeatherLocation weatherLocation, WeatherInfo weatherInfo){
        return getMoonRiseInUTC(weatherLocation,weatherInfo.getTimestamp());
    }

    public static long getMoonSetInUTC(Astronomy.Riseset riseset, long time){
        return setRiseTimeToDay(riseset.set,time);
    }

    public static long getMoonSetInUTC(WeatherLocation weatherLocation, long time){
        Astronomy.Riseset riseset = Weather.getMoonRiseset(weatherLocation,time);
        return getMoonSetInUTC(riseset,time);
    }

    public static long getMoonSetInUTC(WeatherLocation weatherLocation, WeatherInfo weatherInfo){
        return getMoonSetInUTC(weatherLocation,weatherInfo.getTimestamp());
    }

    public static class RiseSetTimes {
        public final static int RISE = 0;
        public final static int SET = 1;
        public final static int TWILIGHT_MORNING = 2;
        public final static int TWILIGHT_EVENING = 3;
        public long[] sun;
        public long[] moon;

        public RiseSetTimes(WeatherLocation weatherLocation, long time){
            Astronomy.Riseset risesetSun  = Weather.getRiseset(weatherLocation,time);
            Astronomy.Riseset risesetMoon = Weather.getMoonRiseset(weatherLocation,time);
            sun  = new long[4];
            moon = new long[4];
            sun[RISE]              = Weather.getSunriseInUTC(risesetSun,time);
            sun[SET]               = Weather.getSunsetInUTC(risesetSun,time);
            sun[TWILIGHT_MORNING]  = Weather.getCivilTwilightMorning(risesetSun,time);
            sun[TWILIGHT_EVENING]  = Weather.getCivilTwilightEvening(risesetSun,time);
            moon[RISE]             = Weather.getMoonRiseInUTC(risesetMoon, time);
            moon[SET]              = Weather.getMoonSetInUTC(risesetMoon, time);
            moon[TWILIGHT_MORNING] = 0;
            moon[TWILIGHT_EVENING] = 0;
        }
    }

    // known new moon on 2023, Sept. 15 01:39 UTC, in seconds epoch time 1694785140 in millis 1694785140000
    final static long KNOWN_NEW_MOON = 1694785140;
    // moon phases repeat every 29.53 days.
    final static double MOON_REPEAT_DAYS = 29.530589d;

    /**
     * Calculates the moon day from the current time.
     *
     * @param time in milliseconds epoch time
     * @return day of the moon phase; range 0 - 29.53
     */

    public static double getMoonPhaseDay(long time){
        // get seconds passed since known new moon
        long sec_passed_since_new_moon = time/1000 - KNOWN_NEW_MOON;
        // convert passed seconds to days
        double days_passed_since_new_moon = sec_passed_since_new_moon / 60d / 60d / 24d;
        // get new moons since known_new_moon
        double new_moons_since = days_passed_since_new_moon / MOON_REPEAT_DAYS;
        // get day of the moon phase, range 0 - 29.53
        double day = days_passed_since_new_moon % MOON_REPEAT_DAYS;
        return day;
    }

    /**
     * Calculates the moon phase from the current time.
     *
     * @param time in milliseconds epoch time
     * @return the moon phase; range 0 - 1, where 0 is new moon and 0.5 is full moon
     */

    public static double getMoonPhase(long time){
        double day = getMoonPhaseDay(time);
        // get moon-phase in range 0-1, while 0.5 is full moon
        double phase = day / MOON_REPEAT_DAYS;
        return phase;
    }

    /**
     * Calculates the moon phase from the current time.
     *
     * @param time in milliseconds epoch time
     * @return the moon phase in degrees; range 0 - 360, where 0 is new moon and 180 is full moon
     */

    public static int getMoonPhaseInDegrees(long time){
        double phase = getMoonPhase(time);
        double degree = 360d*phase;
        return (int) Math.round(degree);
    }


    public static boolean isSunriseInIntervalUTC(Astronomy.Riseset riseset, long start, long stop){
        long sunrise = getSunriseInUTC(riseset,(start+stop)/2);
        if ((sunrise>=start) && (sunrise<=stop)){
            return true;
        }
        return false;
    }

    public static boolean isSunsetInIntervalUTC(Astronomy.Riseset riseset, long start, long stop){
        long sunset = getSunsetInUTC(riseset,(start+stop)/2);
        if ((sunset>=start) && (sunset<=stop)){
            return true;
        }
        return false;
    }

    public static String toHourMinuteString(long time){
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(new Date(time));
    }

    public static String toFullDateTimeString(long time){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd.MM.yyyy, HH:mm");
        return simpleDateFormat.format(new Date(time));
    }

    public static class DisplayLayout{
        public final static int DEFAULT = 0;
    }

    public static class WindDisplayType{
        public final static int ARROW = 0;
        public final static int BEAUFORT = 1;
        public final static int TEXT = 2;
    }

    public static class WindDisplayUnit{
        public final static int METERS_PER_SECOND = 0;
        public final static int KILOMETERS_PER_HOUR = 1;
        public final static int BEAUFORT = 2;
        public final static int KNOTS = 3;
    }

    public static String getWindUnitString(int type){
        switch (type){
            case WindDisplayUnit.METERS_PER_SECOND: return "m/s";
            case WindDisplayUnit.KILOMETERS_PER_HOUR: return "km/h";
            case WindDisplayUnit.BEAUFORT: return "bf";
            case WindDisplayUnit.KNOTS: return "kn";
        }
        return "?";
    }

    public static class DistanceDisplayUnit{
        public final static int METRIC = 0;
        public final static int NAUTIC = 1;
        public final static int IMPERIAL = 2;
    }

    public static class WindData{
        public long timestamp;
        private double speed;
        private double direction;

        public WindData(){
        }

        public WindData(WeatherInfo weatherInfo){
            this.timestamp = weatherInfo.timestamp;
            if (weatherInfo.hasWindSpeed()){
                this.speed = weatherInfo.wind_speed;
            }
            if (weatherInfo.hasWindDirection()){
                this.direction = weatherInfo.wind_direction;
            }
        }

        public double getSpeed(){
            return speed;
        }

        public double getDirection(){
            return direction;
        }

        public long getTimestamp(){
            return timestamp;
        }

    }

    public static String getWindString(Context context,CurrentWeatherInfo weatherCard){
        if (weatherCard.currentWeather.hasWindSpeed()){
            String windstring="";
            String windspeed = "";
            if (WeatherSettings.getWindDisplayUnit(context)==Weather.WindDisplayUnit.METERS_PER_SECOND){
                windspeed = String.valueOf(weatherCard.currentWeather.getWindSpeedInMsInt())+" ";
            }
            if (WeatherSettings.getWindDisplayUnit(context)==Weather.WindDisplayUnit.KILOMETERS_PER_HOUR){
                windspeed = String.valueOf(weatherCard.currentWeather.getWindSpeedInKmhInt())+" ";
            }
            if (WeatherSettings.getWindDisplayUnit(context)==Weather.WindDisplayUnit.BEAUFORT){
                windspeed = String.valueOf(weatherCard.currentWeather.getWindSpeedInBeaufortInt())+" ";
            }
            if (WeatherSettings.getWindDisplayUnit(context)==Weather.WindDisplayUnit.KNOTS){
                windspeed = String.valueOf(weatherCard.currentWeather.getWindSpeedInKnotsInt())+" ";
            }
            windstring = windstring + windspeed;
            if (weatherCard.currentWeather.hasFlurries()){
                String flurries = "";
                switch (WeatherSettings.getWindDisplayUnit(context)){
                    case Weather.WindDisplayUnit.METERS_PER_SECOND: flurries=String.valueOf(weatherCard.currentWeather.getFlurriesInMsInt()); break;
                    case Weather.WindDisplayUnit.BEAUFORT: flurries=String.valueOf(weatherCard.currentWeather.getFlurriesInBeaufortInt()); break;
                    case Weather.WindDisplayUnit.KILOMETERS_PER_HOUR: flurries=String.valueOf(weatherCard.currentWeather.getFlurriesInKmhInt()); break;
                    case Weather.WindDisplayUnit.KNOTS: flurries=String.valueOf(weatherCard.currentWeather.getFlurriesInKnotsInt());
                }
                windstring = windstring + " ("+flurries+") ";
            }
            return windstring;
        }
        return null;
    }

    final static class SIMPLEDATEFORMATS {
        final static SimpleDateFormat DETAILED              = new SimpleDateFormat("EE, dd.MM.yyyy, HH:mm:ss");
        final static SimpleDateFormat DETAILED_NO_SECONDS   = new SimpleDateFormat("EE, dd.MM.yyyy, HH:mm");
        final static SimpleDateFormat DATETIME              = new SimpleDateFormat("dd.MM, HH:mm");
        final static SimpleDateFormat TIME_SEC              = new SimpleDateFormat("HH:mm:ss");
        final static SimpleDateFormat TIME                  = new SimpleDateFormat("HH:mm");
        final static SimpleDateFormat HOUR                  = new SimpleDateFormat("HH");
        final static SimpleDateFormat DAYOFWEEK             = new SimpleDateFormat("EE");
    }

    public static String GetDateString(SimpleDateFormat simpleDateFormat, long time){
        return simpleDateFormat.format(time);
    }

}

