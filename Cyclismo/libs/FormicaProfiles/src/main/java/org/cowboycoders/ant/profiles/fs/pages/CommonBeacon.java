package org.cowboycoders.ant.profiles.fs.pages;

import org.cowboycoders.ant.profiles.BitManipulation;
import org.cowboycoders.ant.profiles.pages.BurstEncodable;
import org.cowboycoders.ant.profiles.pages.SinglePacketEncodable;
import org.cowboycoders.ant.profiles.pages.AntPage;
import org.fluxoid.utils.bytes.LittleEndianArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class CommonBeacon implements AntPage {
    public static final int PAGE_NUM = 67; // guess
    protected final boolean isDataAvailable;

    public CommonBeacon(byte[] data) {
        isDataAvailable = BitManipulation.booleanFromU8(data[1], 0x20);
    }

    @Override
    public int getPageNumber() {
        return PAGE_NUM;
    }

    public boolean isDataAvailable() {
        return isDataAvailable;
    }

    public enum State {
        LINK,
        AUTH,
        TRANSPORT,
        BUSY,
        UNKNOWN,
    }

    public abstract static class CommonBeaconPayload implements SinglePacketEncodable, BurstEncodable {
        protected boolean isDataAvailable = false;
        protected State state = State.LINK;

        public boolean isDataAvailable() {
            return isDataAvailable;
        }


        public CommonBeaconPayload  setDataAvailable(boolean dataAvailable) {
            isDataAvailable = dataAvailable;
            return this;
        }

        public State getState() {
            return state;
        }

        public CommonBeaconPayload setState(State state) {
            this.state = state;
            return this;
        }

        @Override
        public void encode(ByteArrayOutputStream os) {
            byte [] packet = new byte[8];
            encode(packet);
            os.write(packet, 0, packet.length);
        }

        @Override
        public void encode(byte[] packet) {
            LittleEndianArray view = new LittleEndianArray(packet);
            view.put(PAGE_OFFSET,1, PAGE_NUM);
            view.put(2,1,state.ordinal());
            packet[1] = isDataAvailable ? (byte) 0x20 : 0;
        }
    }
}
