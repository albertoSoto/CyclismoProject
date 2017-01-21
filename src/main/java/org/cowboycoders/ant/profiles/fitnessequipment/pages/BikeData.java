package org.cowboycoders.ant.profiles.fitnessequipment.pages;

import org.cowboycoders.ant.profiles.fitnessequipment.Defines;

import static org.cowboycoders.ant.profiles.BitManipulation.*;

/**
 * Page 21
 * Created by fluxoid on 02/01/17.
 */
public class BikeData extends CommonPageData {

    public static final int CADENCE_OFFSET = 4;
    public static final int POWER_OFFSET = 5;

    /**
     * @return in rpm
     */
    public int getCadence() {
        return cadence;
    }

    /**
     *
     * @return in watts
     */
    public int getPower() {
        return power;
    }

    private final int cadence;
    private final int power;

    public BikeData(byte[] data) {
        super(data);
        final int cadenceRaw = UnsignedNumFrom1LeByte(data[CADENCE_OFFSET]);
        if (cadenceRaw != UNSIGNED_INT8_MAX) {
            cadence = cadenceRaw;
        } else {
            cadence = 0;
        }
        final int powerRaw = UnsignedNumFrom2LeBytes(data, POWER_OFFSET);
        if (powerRaw != UNSIGNED_INT16_MAX) {
            power = powerRaw;
        } else {
            power = 0;
        }
    }

    public static class BikeDataPayload  extends CommonPagePayload {
        private int power = 0;
        private int cadence = 0;

        public BikeDataPayload setPower(int power) {
            this.power = power;
            return this;
        }

        public BikeDataPayload setCadence(int cadence) {
            this.cadence = cadence;
            return this;
        }

        public int getPower() {
            return power;
        }

        public int getCadence() {
            return cadence;
        }

        @Override
        public BikeDataPayload setLapFlag(boolean state) {
            return (BikeDataPayload) super.setLapFlag(state);
        }

        @Override
        public BikeDataPayload setType(Defines.EquipmentType type) {
            return (BikeDataPayload) super.setType(type);
        }

        @Override
        public BikeDataPayload setState (Defines.EquipmentState state) {
            return (BikeDataPayload) super.setState(state);
        }

        public void encode(final byte[] packet) {
            super.encode(packet);
            PutUnsignedNumIn2LeBytes(packet, POWER_OFFSET, power);
            PutUnsignedNumIn1LeBytes(packet, CADENCE_OFFSET, cadence);
        }
    }
}
