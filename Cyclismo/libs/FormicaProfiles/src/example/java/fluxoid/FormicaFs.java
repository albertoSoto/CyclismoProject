package fluxoid;

import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.MasterChannelType;
import org.cowboycoders.ant.messages.data.CompleteDataMessage;
import org.cowboycoders.ant.profiles.common.PageDispatcher;
import org.cowboycoders.ant.profiles.fs.Directory;
import org.cowboycoders.ant.profiles.fs.DirectoryHeader;
import org.cowboycoders.ant.profiles.fs.FileEntry;
import org.cowboycoders.ant.profiles.fs.defines.FileAttribute;
import org.cowboycoders.ant.profiles.fs.pages.BeaconAdvert;
import org.cowboycoders.ant.profiles.fs.pages.BeaconAuth;
import org.cowboycoders.ant.profiles.fs.pages.BeaconTransport;
import org.cowboycoders.ant.profiles.fs.pages.CommonBeacon;
import org.cowboycoders.ant.profiles.fs.pages.cmd.AuthCommand;
import org.cowboycoders.ant.profiles.fs.pages.cmd.DisconnectCommand;
import org.cowboycoders.ant.profiles.fs.pages.cmd.DownloadCommand;
import org.cowboycoders.ant.profiles.fs.pages.cmd.LinkCommand;
import org.cowboycoders.ant.profiles.simulators.NetworkKeys;
import org.cowboycoders.ant.utils.ByteUtils;
import org.fluxoid.utils.Format;
import org.fluxoid.utils.bytes.LittleEndianArray;
import org.fluxoid.utils.crc.Crc16Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FormicaFs {

    // these represent manufacturers of watches
    private static final int GARMIN = 1;
    private static final int HEALTH_AND_LIFE = 257;
    public static final int GARMIN_FR70_DEV_ID = 1436;
    private static final Logger logger = Logger.getLogger(FormicaFs.class.getName());

    static {
        logger.setLevel(Level.FINEST);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINEST);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }

    enum State {
        LINK,
        AUTH,
        TRANSPORT
    }


    public static void main(String [] args) {
        AntTransceiver antchip = new AntTransceiver(0);
        Node node = new Node(antchip);
        node.start();
        node.reset();

        final ExecutorService channelExecutor = Executors.newFixedThreadPool(1);

        final Channel channel = node.getFreeChannel();
        ChannelType type = new MasterChannelType(false, false);
        channel.assign(NetworkKeys.ANT_FS, type);
        channel.setId(GARMIN_FR70_DEV_ID, GARMIN,0,false);
        channel.setPeriod(8192);
        channel.setFrequency(50);
        byte [] data = new byte[8];

        new BeaconAdvert.BeaconPayload()
                .setFsDeviceType(1436)
                .setManufacturerID(1)
                .setDataAvailable(true)
                .encode(data);

        final PageDispatcher dispatcher = new PageDispatcher();

        channel.setBroadcast(data);
        channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);



        channel.registerRxListener(msg -> {

            byte [] data1 = msg.getPrimitiveData();
            if (!dispatcher.dispatch(data1)) {

            }
            logger.fine(Format.bytesToString(data1).toString());

        }, CompleteDataMessage.class);

        channel.registerBurstListener(message -> dispatcher.dispatch(ByteUtils.unboxArray(message.getData())));

        dispatcher.addListener(LinkCommand.class, message -> {
                    // possibly check serial number?
                    channelExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            byte [] data12 = new byte[8];
                            new BeaconAuth.BeaconAuthPayload()
                                    .setDataAvailable(true)
                                    .setSerialNumber(message.getSerialNumber())
                                    .encode(data12);
                            data12[3] = (byte) AuthCommand.AuthMode.PASSTHROUGH.ordinal();
                            logger.info(Format.format("serial: %x\n", message.getSerialNumber()));
                            channel.setBroadcast(data12);
                            channel.setPeriod(4096);
                            channel.setFrequency(message.getRequestedFrequency());
                            logger.fine("made transisiton");
                        }
                    });

            });

        dispatcher.addListener(AuthCommand.class, message -> {
            logger.log(Level.INFO, "authentication request: {0}", message.getMode());
            switch (message.getMode()) {
                case PASSTHROUGH:
                    // burst might not necessary
                    final byte [] data13 = new byte[16];
                    data13[0] = 67; // beacon advert
                    data13[8] = 68;
                    data13[9] = (byte) 0x84;
                    data13[10] = 1; // accept auth
                    channelExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
//                                try {
//                                    channel.sendBurst(data,2L, TimeUnit.SECONDS);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                } catch (TimeoutException e) {
//                                    e.printStackTrace();
//                                }
//                                System.out.println("sent burst");
                            byte [] data13 = new byte[8];
                            new BeaconTransport.BeaconTransportPayload()
                                    //.setSerialNumber(0xa56bde7b) // android app
                                    .setDataAvailable(true)
                                    .setState(CommonBeacon.State.TRANSPORT)
                                    .encode(data13);
                            channel.setBroadcast(data13);
                        }
                    });

            }

        });


        Runnable downloadIndexBehaviour = new Runnable() {

            @Override
            public void run() {
                byte [] data = new byte[64]; // pad to multiple of 8 bytes
                LittleEndianArray view = new LittleEndianArray(data);
                data[0] = 67;
                data[2] = 3; // guess
                data[8] = 68;
                data[9] = (byte) 0x89; // download response codetS
                data[10] = 0; // response code - zero = no error?
                view.put(12,4,32); // remaining data
                view.put(16,4,0); // offset of data
                view.put(20,4,32); // file size;
                ByteBuffer buf = ByteBuffer.wrap(data);
                buf.position(24);
                buf = buf.slice();



                mkDir().accept(buf);

                logger.finest("before crc");
                byte [] cp = Arrays.copyOfRange(data, 24, 56);
                int crc = Crc16Utils.computeCrc(Crc16Utils.ANSI_CRC16_TABLE, 0, cp);
                logger.finest("crc: " + crc);
                view.put(data.length - 2,2, crc);

                channel.sendBurst(data,10L, TimeUnit.SECONDS);
                logger.finest("sent file info");
            }
        };

        MultiPart multiPart = new MultiPart(getFit(),channel);


        //TODO: we need to listen to burst rather than these individual messages
        dispatcher.addListener(DownloadCommand.class, message -> {
            switch (message.getIndex()) {
                default: {
                    logger.info("index: " + message.getIndex() + ", requested");
                    logger.info("offset: " + message.getOffset() + ", requested");
                    logger.info("firstReq: " + message.isFirstRequest());
                    multiPart.setOffset(message.getOffset());
                    if (multiPart.hasFailed()) { // sending file failed
                        //TODO: transition to previous state
                    }
                    channelExecutor.submit(multiPart);
                    break;
                }
                case 0: {
                    channelExecutor.submit(downloadIndexBehaviour);
                    break;
                }


            }
            channelExecutor.submit(new Runnable() {
                @Override
                public void run() {

                    // transition back to transport
                    byte [] beacon = new byte[8];
                    new BeaconTransport.BeaconTransportPayload()
                            .setDataAvailable(true)
                            .setState(CommonBeacon.State.TRANSPORT)
                            .encode(beacon);
                    channel.setBroadcast(beacon);

                }
            });
        });


        dispatcher.addListener(DisconnectCommand.class, (msg) -> {
            logger.info("Received disconnect command");
        });




        channel.open();
    }

    public static Directory mkDir() {
        DirectoryHeader.DirectoryHeaderBuilder header = new DirectoryHeader.DirectoryHeaderBuilder()
                .setModifiedTimestamp(100)
                .setSystemTimestamp(200);

        FileEntry soleEntry = new FileEntry.FileEntryBuilder()
                .addAttributes(FileAttribute.READ)
                .setIndex(1) // don't set to zero
                .setLength(8)
                .setId(4) // for watch file type id
                .setTimeStamp(150)
                .setDataType(128) // fit file
                .create();

        Directory dir = new Directory.DirectoryBuilder()
                .setHeader(header)
                .addFile(soleEntry)
                .create();
        return dir;
    }

    public static byte[] getFit() {
        // should probably use a stream rather than loading into memory
        byte [] data = new byte[0];
        try (InputStream resource = ClassLoader.getSystemResourceAsStream("test.fit");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int avail;
            while ((avail = resource.available()) > 0) {
                out.write(resource.read());
            }
            data = out.toByteArray();

        } catch (IOException e) {
            // return zero length array
        }
        return data;
    }

    private static class MultiPart implements Runnable {

        private final Channel channel;
        private final int chunkSize;
        private boolean failed;

        public boolean hasFailed() {
            return failed;
        }

        private MultiPart(byte [] fullFile, Channel channel) {
            this(fullFile, DEFAULT_CHUNK_SIZE, channel);
        }

        private MultiPart(byte [] fullFile, int chunkSize, Channel channel) {
            this.chunkSize = chunkSize;
            this.full = fullFile;
            this.channel = channel;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        private final byte [] full;
        private static final int DEFAULT_CHUNK_SIZE = 512;
        private int offset = 0;
        private int crc = 0;

        @Override
        public void run() {
            try {
                if (full.length <= offset) {
                    logger.severe("offset out of bounds");
                    return;
                }
                byte[] header = new byte[24]; // pad to multiple of 8 bytes
                int end = Math.min(offset + chunkSize, full.length);
                byte[] chunk = Arrays.copyOfRange(full, offset, end);

                //byte [] half2 = Arrays.copyOfRange(fitFile, fitFile.length /2, fitFile.length);
                LittleEndianArray view = new LittleEndianArray(header);
                header[0] = 67;
                header[2] = 3; // guess
                header[8] = 68;
                header[9] = (byte) 0x89; // download response codetS
                header[10] = 0; // response code - zero = no error?
                view.put(12, 4, chunk.length); // remaining data
                view.put(16, 4, offset); // offset of data
                view.put(20, 4, full.length); // file size;

                logger.finest("pre copy");
                int minLen = header.length + chunk.length + 2;
                minLen = minLen + (8 - minLen % 8) % 8; // pad to multiple of 8 bytes
                byte[] data = new byte[minLen];
                System.arraycopy(header, 0, data, 0, header.length);
                System.arraycopy(chunk, 0, data, header.length, chunk.length);
                crc = Crc16Utils.computeCrc(Crc16Utils.ANSI_CRC16_TABLE, crc, chunk);
                logger.finest("crc : " + crc);
                view = new LittleEndianArray(data);
                view.put(data.length - 2, 2, crc);

                logger.finest("pre send half");
                channel.sendBurst(data, 60L, TimeUnit.SECONDS);
                logger.finest("sent fit file");
            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
            }

        }
    };

}
