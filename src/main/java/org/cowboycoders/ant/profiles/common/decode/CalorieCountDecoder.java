package org.cowboycoders.ant.profiles.common.decode;

import org.cowboycoders.ant.events.BroadcastMessenger;
import org.cowboycoders.ant.profiles.common.decode.interfaces.CalorieCountDecodable;
import org.cowboycoders.ant.profiles.common.decode.utils.CounterBasedDecoder;
import org.cowboycoders.ant.profiles.common.events.CalorieBurntUpdate;
import org.cowboycoders.ant.profiles.common.events.WheelRotationsUpdate;
import org.cowboycoders.ant.profiles.common.events.interfaces.TelemetryEvent;

;


/**
 * Created by fluxoid on 10/01/17.
 */
public class CalorieCountDecoder implements Decoder<CalorieCountDecodable> {


    private long caloriesBurnt;
    private CalorieCountDecodable prev;


    public CalorieCountDecoder(BroadcastMessenger<TelemetryEvent> updateHub) {
        reset();
    }

    public void reset() {
        caloriesBurnt = 0;
    }


    @Override
    public void update(CalorieCountDecodable newPage) {
        if (prev == null) {
            prev = newPage;
            return;
        }
        caloriesBurnt += newPage.getCalorieDelta(prev);
        prev = newPage;
    }

    @Override
    public void invalidate() {
        prev = null;
    }
}
