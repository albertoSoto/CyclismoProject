package org.cowboycoders.ant.profiles.fitnessequipment.pages;

import org.cowboycoders.ant.profiles.BitManipulation;

import java.math.BigDecimal;

/**
 * Page 48
 * Created by fluxoid on 16/01/17.
 */
public class PercentageResistance {

    private static final int RESITANCE_OFFSET = 8;
    private final BigDecimal resistance;

    public PercentageResistance(byte[] packet) {
        int raw = BitManipulation.UnsignedNumFrom1LeByte(packet[RESITANCE_OFFSET]);
        resistance = new BigDecimal(raw).divide(new BigDecimal(2));
    }

    /**
     * Percentage resistance
     * @return 0-100%, resolution 0.5%
     */
    public BigDecimal getResistance() {
        return resistance;
    }
}
