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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmClockUpdatedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent!=null){
            String action = intent.getAction();
            if (action!=null){
                if (action.equals("android.app.action.NEXT_ALARM_CLOCK_CHANGED")){
                    PrivateLog.log(context.getApplicationContext(), PrivateLog.WIDGET,PrivateLog.INFO,"new alarm (clock) received, updating widget.");
                    WidgetRefresher.refreshClockWidget(context);
                }
            }
        }
    }
}
