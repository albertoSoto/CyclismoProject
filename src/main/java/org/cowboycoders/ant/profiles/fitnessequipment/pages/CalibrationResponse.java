package org.cowboycoders.ant.profiles.fitnessequipment.pages;

import org.cowboycoders.ant.profiles.BitManipulation;
import org.cowboycoders.ant.profiles.pages.AntPage;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.cowboycoders.ant.profiles.BitManipulation.UNSIGNED_INT16_MAX;
import static org.cowboycoders.ant.profiles.BitManipulation.UNSIGNED_INT8_MAX;
import static org.cowboycoders.ant.profiles.BitManipulation.intToBoolean;

/**
 * Page 1
 * Created by fluxoid on 02/01/17.
 */
public class CalibrationResponse implements AntPage {

    private static final int OFFSET_FLAG__MASK = 0x40;
    private static final int SPINDOWN_FLAG_MASK = 0x80;
    private static final int SPINDOWN_FLAG_OFFSET = 2;
    private static final int SPINDOWN_OFFSET = 7;
    private static final int OFFSET_OFFSET = 5;
    private static final int OFFSET_FLAG_OFFSET = 2;
    private static final int TEMP_OFFSET = 4;


    public boolean isZeroOffsetSuccess() {
        return zeroOffsetSuccess;
    }

    public boolean isSpinDownSuccess() {
        return spinDownSuccess;
    }

    private final boolean zeroOffsetSuccess;
    private final boolean spinDownSuccess;


    /**
     * Some sort of offset in range 0 - 65534 (no units), resolution 1
     */
    public Integer getZeroOffset() {
        return zeroOffset;
    }

    /**
     *  spin down time of wheel/roller in range 0ms - 65534ms, resolution 1ms
     */
    public Integer getSpinDownTime() {
        return spinDownTime;
    }

    /**
     * internal temperature of the trainer: -25C - +100C, resolution: 0.5C
     *
     * @return internal temp, or null if not set
     */
    public BigDecimal getTemp() {
        return temp;
    }

    private final Integer zeroOffset;
    private final Integer spinDownTime;
    private final BigDecimal temp;


    public CalibrationResponse(byte[] packet) {
        zeroOffsetSuccess = intToBoolean (packet[OFFSET_FLAG_OFFSET] & OFFSET_FLAG__MASK);
        spinDownSuccess = intToBoolean (packet[SPINDOWN_FLAG_OFFSET] & SPINDOWN_FLAG_MASK);
        final int tempRaw = BitManipulation.UnsignedNumFrom1LeByte(packet[TEMP_OFFSET]);
        if (tempRaw != UNSIGNED_INT8_MAX) { // NULL
            temp = new BigDecimal(tempRaw).divide(new BigDecimal(2), 1, BigDecimal.ROUND_HALF_UP).subtract(new BigDecimal(25));
        } else {
            temp = null;
        }
        final int offsetRaw = BitManipulation.UnsignedNumFrom2LeBytes(packet, OFFSET_OFFSET);
        if (offsetRaw != UNSIGNED_INT16_MAX) {
            zeroOffset = offsetRaw;
        } else {
            zeroOffset = null;
        }
        final int spinDownRaw = BitManipulation.UnsignedNumFrom2LeBytes(packet, SPINDOWN_OFFSET);
        if (spinDownRaw != UNSIGNED_INT16_MAX) {
            spinDownTime = spinDownRaw;
        } else {
            spinDownTime = null;
        }

    }


}
