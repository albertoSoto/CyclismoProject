package org.cowboycoders.ant.profiles.simulators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.ChannelEventHandler;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.MasterChannelType;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.cowboycoders.ant.messages.data.DataMessage;
import org.cowboycoders.ant.profiles.common.PageDispatcher;
import org.cowboycoders.ant.profiles.fitnessequipment.Defines.CommandId;
import org.cowboycoders.ant.profiles.fitnessequipment.pages.*;
import org.cowboycoders.ant.profiles.pages.Request;

import java.math.BigDecimal;
import java.util.Formatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.cowboycoders.ant.profiles.common.PageDispatcher.getPageNum;

/**
 * Created by fluxoid on 31/01/17.
 */
public class Dummy {

    private static final Logger logger = LogManager.getLogger();

    private Node transceiver;

    public static CharSequence printBytes(Byte[] arr) {
        Formatter formatter = new Formatter();
        final int len = arr.length * 3;
        for (byte b: arr) {
            formatter.format("%02x:",b);
        }
        // strip last char
        int end = arr.length == 0 ? 0 : len - 1;
        return formatter.toString().substring(0, end);
    }

    Timer timer = new Timer();


    public static void main(String [] args) {
        //printBytes(new Byte[] {-1,2,3});
        AntTransceiver antchip = new AntTransceiver(0);
        Node node = new Node(antchip);
        node.start();
        node.reset();
        new Dummy().start(node);
    }

    public void start(Node transceiver) {
        this.transceiver = transceiver;



        ExecutorService pool = Executors.newSingleThreadExecutor();

        final DummyTrainerState state = new DummyTrainerState();


        state.setPower(200);
        state.setCadence(75);
        state.setHeartRate(123);
        state.setSpeed(new BigDecimal(5));

        final PageDispatcher pageDispatcher = new PageDispatcher();

        pageDispatcher.addListener(Request.class, new BroadcastListener<Request>() {

            @Override
            public void receiveMessage(Request request) {
                final int page = request.getPageNumber();
                switch (page) {
                    case CapabilitiesPage.PAGE_NUMBER:
                        logger.trace("capabilities requested");
                        state.setCapabilitesRequested();
                        break;
                    case ConfigPage.PAGE_NUMBER:
                        logger.trace("config requested");
                        state.setConfigRequested();
                        break;
                    case Command.PAGE_NUMBER:
                        logger.trace("command status requested");
                        state.sendCmdStatus();
                    case CalibrationResponse.PAGE_NUMBER:
                        logger.trace("calibration response requested");
                        state.sendCalibrationResponse();
                }
            }
        });

        pageDispatcher.addListener(ConfigPage.class, new BroadcastListener<ConfigPage>() {

            @Override
            public void receiveMessage(ConfigPage page) {
                state.useConfig(page.getConfig());
            }
        });

        pageDispatcher.addListener(PercentageResistance.class, new BroadcastListener<PercentageResistance>() {
            @Override
            public void receiveMessage(PercentageResistance percentageResistance) {
                state.setResistance(percentageResistance.getResistance());
            }
        });

        pageDispatcher.addListener(CalibrationResponse.class, new BroadcastListener<CalibrationResponse>() {
            @Override
            public void receiveMessage(CalibrationResponse calibrationResponse) {
                if (calibrationResponse.isSpinDownSuccess()) {
                    state.requestSpinDownCalibration();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            state.setSpeed(new BigDecimal(11.0));
                        }
                    }, 10000);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            state.setSpeed(new BigDecimal(0.0));
                        }
                    }, 20000);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            state.setSpeed(new BigDecimal(5.0));
                        }
                    }, 25000);
                }
                if (calibrationResponse.isZeroOffsetSuccess()) {
                    state.requestOffsetCalibration();
                }
            }
        });



        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                state.incrementLaps();

            }
        }, 1000, 60000);


        final Channel channel = transceiver.getFreeChannel();
        ChannelType type = new MasterChannelType(false, false);
        channel.assign(NetworkKeys.ANT_SPORT, type);
        channel.setId(1234,17,255,false);
        channel.setPeriod(8192);
        channel.setFrequency(57);
        channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);
        channel.registerRxListener(new BroadcastListener<DataMessage>() {
            @Override
            public void receiveMessage(DataMessage msg) {
                byte [] data = msg.getPrimitiveData();
                pageDispatcher.dispatch(data);
                CommandId cmd = CommandId.getValueFromInt(getPageNum(data));
                if (cmd != CommandId.UNRECOGNIZED && cmd != CommandId.NO_CONTROL_PAGE_RECEIVED) {
                    logger.trace("receieved cmd: " + cmd);
                }
                logger.trace(printBytes(msg.getData()));
            }
        }, DataMessage.class);

        channel.registerEventHandler(new ChannelEventHandler() {
            @Override
            public void onTransferNextDataBlock() {

            }

            @Override
            public void onTransferTxStart() {

            }

            @Override
            public void onTransferTxCompleted() {

            }

            @Override
            public void onTransferTxFailed() {

            }

            @Override
            public void onChannelClosed() {

            }

            @Override
            public void onRxFailGoToSearch() {

            }

            @Override
            public void onChannelCollision() {

            }

            @Override
            public void onTransferRxFailed() {

            }

            @Override
            public void onRxSearchTimeout() {

            }

            @Override
            public void onRxFail() {

            }

            @Override
            public void onTxSuccess() {
                BroadcastDataMessage msg = new BroadcastDataMessage();
                msg.setData(state.nextPacket());
                channel.send(msg);
            }
        });

        channel.open();

    }

}
