package org.cowboycoders.ant.profiles.fitnessequipment.pages;

import org.cowboycoders.ant.profiles.BitManipulation;
import org.cowboycoders.ant.profiles.pages.AntPacketEncodable;
import org.cowboycoders.ant.profiles.pages.AntPage;

import java.math.BigDecimal;

import static org.cowboycoders.ant.profiles.BitManipulation.PutUnsignedNumIn1LeBytes;

/**
 * Page 48
 * Created by fluxoid on 16/01/17.
 */
public class PercentageResistance implements AntPage {

    public static final  int PAGE_NUMBER = 48;

    @Override
    public int getPageNumber() {
        return PAGE_NUMBER;
    }

    private static final int RESISTANCE_OFFSET = 7;
    private final BigDecimal resistance;

    public static class PercentageResistancePayload implements AntPacketEncodable {
        private BigDecimal resistance = new BigDecimal(0);

        public BigDecimal getResistance() {
            return resistance;
        }

        public PercentageResistancePayload setResistance(BigDecimal resistance) {
            if (resistance == null) {
                throw new IllegalArgumentException("resistance cannot be null");
            }
            this.resistance = resistance;
            return this;
        }

        public void encode(final byte [] packet) {
            PutUnsignedNumIn1LeBytes(packet, PAGE_OFFSET, PAGE_NUMBER);
            BigDecimal n = resistance.multiply(new BigDecimal(2));
            PutUnsignedNumIn1LeBytes(packet, RESISTANCE_OFFSET, n.intValue());

        }
    }

    public PercentageResistance(byte[] packet) {
        int raw = BitManipulation.UnsignedNumFrom1LeByte(packet[RESISTANCE_OFFSET]);
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
