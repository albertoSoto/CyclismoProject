package fluxoid;

import java9.util.function.Consumer;
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
import org.cowboycoders.ant.profiles.pages.AntPage;
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

    private interface StateMutator {

        ExecutorService channelExecutor = Executors.newFixedThreadPool(1);

        default void performOffThread(Runnable runnable) {
            channelExecutor.submit(runnable);
        }

        void setStateNow(FsState newState);

        default void setState(FsState newState) {
            performOffThread(() -> setStateNow(newState));
        }

        Channel getChannel();

        AuthState getAuthState();
        TransportState getTransportState();
        AdvertState getAdvertState();
    }

    private interface FsState {
        void onTransition(StateMutator mutator);
        void handleMessage(StateMutator mutator, AntPage page);
    }

    private interface WrappedState<T extends FsState> {
        public T getBase();
    }

    private static class DisconnectDecorator<T extends FsState> implements FsState, WrappedState<T> {

        private final T base;

        public T getBase() {
            return base;
        }

        public DisconnectDecorator(T base) {
            this.base = base;
        }

        @Override
        public void onTransition(StateMutator mutator) {
            base.onTransition(mutator);
        }

        @Override
        public void handleMessage(StateMutator ctx, AntPage page) {
            if (page instanceof DisconnectCommand) {
                logger.info("Received disconnect command");
                ctx.setState(ctx.getAdvertState());
                return;
            }
            base.handleMessage(ctx, page);
        }
    }

    // advertise as garmin fr70
    private AdvertState advertState = new AdvertState(1, GARMIN_FR70_DEV_ID);
    private WrappedState<AuthState> authState = new DisconnectDecorator<>(new AuthState());
    private WrappedState<TransportState>  transportState = new DisconnectDecorator<>(new TransportState());

    private static class AdvertState implements FsState {

        private final int manufacturerId;
        private final int deviceType;

        public AdvertState(int manufacturerId, int deviceType) {
            this.manufacturerId = manufacturerId;
            this.deviceType = deviceType;
        }

        @Override
        public void onTransition(StateMutator mutator) {
            Channel channel = mutator.getChannel();
            channel.setPeriod(8192);
            channel.setFrequency(50);
            byte[] data = new byte[8];

            new BeaconAdvert.BeaconPayload()
                    .setFsDeviceType(deviceType)
                    .setManufacturerID(manufacturerId)
                    .setDataAvailable(true)
                    .encode(data);

            channel.setBroadcast(data);
        }

        @Override
        public void handleMessage(StateMutator ctx, AntPage page) {
            if (!(page instanceof LinkCommand)) {
                return;
            }
            LinkCommand linkCommand = (LinkCommand) page;
            Channel channel = ctx.getChannel();
            byte[] beaconData = new byte[8];
            new BeaconAuth.BeaconAuthPayload()
                    .setDataAvailable(true)
                    .setSerialNumber(linkCommand.getSerialNumber())
                    .encode(beaconData);
            beaconData[3] = (byte) AuthCommand.AuthMode.PASSTHROUGH.ordinal();
            logger.info(Format.format("serial: %x\n", linkCommand.getSerialNumber()));
            channel.setBroadcast(beaconData);
            channel.setPeriod(4096);
            channel.setFrequency(linkCommand.getRequestedFrequency());
            logger.fine("made link transition");
            ctx.setState(ctx.getAuthState());
        }
    }


    private static class AuthState implements FsState {

        @Override
        public void onTransition(StateMutator ctx) {

        }

        @Override
        public void handleMessage(StateMutator ctx, AntPage page) {
            if (!(page instanceof AuthCommand)) {
                return;
            }
            AuthCommand authCommand = (AuthCommand) page;
            logger.log(Level.INFO, "authentication request: {0}", authCommand.getMode());
            switch (authCommand.getMode()) {
                case PASSTHROUGH:
                        ctx.setState(ctx.getTransportState());
                    break;
                default:
                    logger.severe("should handle other auth states");


            }
        }

    }

    private static class TransportState implements FsState {

        private final MultiPart multiPart = new MultiPart(getFit());;

        Consumer<Channel> downloadIndexBehaviour = (channel) -> {
            byte[] data = new byte[64]; // pad to multiple of 8 bytes
            LittleEndianArray view = new LittleEndianArray(data);
            data[0] = 67;
            data[2] = 3; // guess
            data[8] = 68;
            data[9] = (byte) 0x89; // download response codetS
            data[10] = 0; // response code - zero = no error?
            view.put(12, 4, 32); // remaining data
            view.put(16, 4, 0); // offset of data
            view.put(20, 4, 32); // file size;
            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.position(24);
            buf = buf.slice();


            mkDir().accept(buf);

            logger.finest("before crc");
            byte[] cp = Arrays.copyOfRange(data, 24, 56);
            int crc = Crc16Utils.computeCrc(Crc16Utils.ANSI_CRC16_TABLE, 0, cp);
            logger.finest("crc: " + crc);
            view.put(data.length - 2, 2, crc);

            channel.sendBurst(data, 10L, TimeUnit.SECONDS);
            logger.finest("sent file info");
        };


        @Override
        public void onTransition(StateMutator ctx) {
            Channel channel = ctx.getChannel();
            // transition back to transport
            byte[] beacon = new byte[8];
            new BeaconTransport.BeaconTransportPayload()
                    .setDataAvailable(true)
                    .setState(CommonBeacon.State.TRANSPORT)
                    .encode(beacon);
            channel.setBroadcast(beacon);

        }

        @Override
        public void handleMessage(StateMutator ctx, AntPage page) {
            if (!(page instanceof DownloadCommand)) {
                return;
            }
            DownloadCommand message = (DownloadCommand) page;
            Channel channel = ctx.getChannel();
            switch (message.getIndex()) {
                default: {
                    logger.info("index: " + message.getIndex() + ", requested");
                    logger.info("offset: " + message.getOffset() + ", requested");
                    logger.info("firstReq: " + message.isFirstRequest());
                    multiPart.setOffset(message.getOffset());
                    if (multiPart.hasFailed()) { // sending file failed
                        //TODO: transition to previous state
                    }
                    multiPart.accept(channel);
                    break;
                }
                case 0: {
                    downloadIndexBehaviour.accept(channel);
                    break;
                }


            }
            onTransition(ctx);
        }


    }


    private StateMutator stateMutator;
    private FsState state = advertState;


    public void start(Node node) {
        node.start();
        node.reset();

        final Channel channel = node.getFreeChannel();

         stateMutator = new StateMutator() {

            @Override
            public void setStateNow(FsState newState) {
                if (state != newState) {
                    newState.onTransition(this);
                }
                state = newState;
            }


            @Override
            public Channel getChannel() {
                return channel;
            }

             @Override
             public AuthState getAuthState() {
                 return authState.getBase();
             }

             @Override
             public TransportState getTransportState() {
                 return transportState.getBase();
             }

             @Override
             public AdvertState getAdvertState() {
                 return advertState;
             }
         };

        ChannelType type = new MasterChannelType(false, false);
        channel.assign(NetworkKeys.ANT_FS, type);
        channel.setId(GARMIN_FR70_DEV_ID, GARMIN, 0, false);

        final PageDispatcher dispatcher = new PageDispatcher();

        channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

        advertState.onTransition(stateMutator);

        channel.registerRxListener(msg -> {

            byte[] data1 = msg.getPrimitiveData();
            if (!dispatcher.dispatch(data1)) {

            }
            logger.fine(Format.bytesToString(data1).toString());

        }, CompleteDataMessage.class);

        channel.registerBurstListener(message -> dispatcher.dispatch(ByteUtils.unboxArray(message.getData())));

        dispatcher.addListener(AntPage.class, (msg) -> {
            // need to perform this out of dispatcher thread as this is shared for replies to channel messages
            stateMutator.performOffThread(() -> state.handleMessage(stateMutator, msg));
        });

        channel.open();
    }

    public static void main(String[] args) {
        AntTransceiver antchip = new AntTransceiver(0);
        Node node = new Node(antchip);
        new FormicaFs().start(node);

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
        byte[] data = new byte[0];
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

    private static class MultiPart implements Consumer<Channel> {

        private final int chunkSize;
        private boolean failed;

        public boolean hasFailed() {
            return failed;
        }

        private MultiPart(byte[] fullFile) {
            this(fullFile, DEFAULT_CHUNK_SIZE);
        }

        private MultiPart(byte[] fullFile, int chunkSize) {
            this.chunkSize = chunkSize;
            this.full = fullFile;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        private final byte[] full;
        private static final int DEFAULT_CHUNK_SIZE = 512;
        private int offset = 0;
        private int crc = 0;

        public void accept(Channel channel) {
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
    }


}
