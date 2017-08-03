package org.cowboycoders.ant.profiles.common.decode;

import org.cowboycoders.ant.events.BroadcastMessenger;
import org.cowboycoders.ant.profiles.common.FilteredBroadcastMessenger;
import org.cowboycoders.ant.profiles.common.decode.interfaces.DistanceDecodable;
import org.cowboycoders.ant.profiles.common.decode.utils.CounterBasedDecoder;;
import org.cowboycoders.ant.profiles.common.events.DistanceUpdate;
import org.cowboycoders.ant.profiles.common.events.WheelRotationsUpdate;
import org.cowboycoders.ant.profiles.common.events.interfaces.TelemetryEvent;

import java.math.BigDecimal;


/**
 * Created by fluxoid on 10/01/17.
 */
public class DistanceDecoder implements Decoder<DistanceDecodable> {


    private final BigDecimal wheelCircumferece;
    private long wheelTicks;
    private MyCounterBasedDecoder decoder;

    public DistanceDecoder(FilteredBroadcastMessenger<TelemetryEvent> updateHub, BigDecimal wheelCircumference) {
        assert  wheelCircumference != null;
        this.wheelCircumferece = wheelCircumference;
        decoder = new MyCounterBasedDecoder(updateHub);
        reset();
    }

    public void reset() {
        wheelTicks = 0;
    }

    private class MyCounterBasedDecoder extends CounterBasedDecoder<DistanceDecodable> {

        public MyCounterBasedDecoder(FilteredBroadcastMessenger<TelemetryEvent> updateHub) {
            super(updateHub);
        }


        @Override
        protected void onUpdate() {
        }

        @Override
        protected void onInitializeCounters() {

        }

        @Override
        protected void onValidDelta() {
            DistanceDecodable next = getCurrentPage();
            DistanceDecodable prev = getPreviousPage();
            wheelTicks += next.getWheelRotationsDelta(prev);
        }

        @Override
        protected void onNoCoast() {
            bus.send(new WheelRotationsUpdate(wheelTicks));
            bus.send(new DistanceUpdate(wheelCircumferece.multiply(new BigDecimal(wheelTicks))));
        }
    }


    @Override
    public void update(DistanceDecodable newPage) {
        decoder.update(newPage);
    }

    @Override
    public void invalidate() {
        decoder.invalidate();
    }
}
