package jo.recording;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.client.Contract;
import com.ib.client.TickType;
import com.ib.client.Types.DeepSide;
import com.ib.client.Types.DeepType;
import com.ib.client.Types.MktDataType;
import com.ib.client.Types.WhatToShow;

import jo.constant.Stocks;
import jo.controller.IBroker;
import jo.handler.ITopMktDataHandler;
import jo.model.Bar;
import jo.recording.event.AbstractEvent;
import jo.recording.event.ErrorEvent;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.RealTimeBarEvent;
import jo.recording.event.TickPriceEvent;
import jo.recording.event.TickSizeEvent;
import jo.recording.event.TickStringEvent;

public class MarketRecorder implements Recorder {
    private static final Logger log = LogManager.getLogger(MarketRecorder.class);
    private boolean recordDeepBook = true;
    private Contract contract;
    private OutputStream out;
    private PrintWriter ps;
    private ArrayBlockingQueue<AbstractEvent> q = new ArrayBlockingQueue<>(64000);
    private ObjectMapper objectMapper = new ObjectMapper();

    public MarketRecorder(Contract contract) {
        this.contract = contract;
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

    public void start(IBroker ib) {
        openFile();

        Thread writerThread = new Thread(this::pollQueue);
        writerThread.setDaemon(true);
        writerThread.setName("MarketRecorder-" + contract.symbol());
        writerThread.start();

        ib.reqRealTimeBars(contract, WhatToShow.TRADES, true, (b) -> addBarEvent(b));
        ib.reqTopMktData(contract, "165,375,295", /* snapshot */false, new TopMktDataHandler());

        if (recordDeepBook) {
            ib.reqDeepMktData(Stocks.toNasdaq(contract), 40, this::updateMktDepth);
        }
    }

    @Override
    public void stop() {
        if (ps != null) {
            ps.close();
        }
    }

    private void addBarEvent(Bar bar) {
        q.add(new RealTimeBarEvent(bar));
    }

    private void updateMktDepth(int position, String marketMaker, DeepType operation, DeepSide side, double price, int size) {
        q.add(new MarketDepthEvent(position, marketMaker, operation, side, price, size));
    }

    public void pollQueue() {
        try {
            while (true) {
                Object event = q.take();
                String str = objectMapper.writeValueAsString(event); // wtf, writeValue(stream) closes the stream
                write(str);
            }
        } catch (InterruptedException e) {
            // terminated
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    private void write(String str) {
        if (ps == null) {
            openFile();
        }
        ps.println(str);
        ps.flush();
    }

    private void openFile() {
        LocalDateTime now = LocalDateTime.now();

        File dir = new File("log/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE));
        dir.mkdir();

        String fileName = String.format("Market-%s-%s.log",
                contract.symbol(),
                StringUtils.replace(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), ":", "-"));

        File logFile = new File(dir, fileName);

        try {
            out = new FileOutputStream(logFile, true);
            ps = new PrintWriter(out);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        q.add(new ErrorEvent(id, errorCode, errorMsg));
    }

    public MarketRecorder withDeepBook(boolean recordDeepBook) {
        this.recordDeepBook = recordDeepBook;
        return this;
    }

    private class TopMktDataHandler implements ITopMktDataHandler {

        @Override
        public void tickPrice(TickType tickType, double price) {
            q.add(new TickPriceEvent(tickType, price));
        }

        @Override
        public void tickSize(TickType tickType, int size) {
            q.add(new TickSizeEvent(tickType, size));
        }

        @Override
        public void tickString(TickType tickType, String value) {
            q.add(new TickStringEvent(tickType, value));
        }

        @Override
        public void tickSnapshotEnd() {

        }

        @Override
        public void marketDataType(MktDataType marketDataType) {

        }
    }
}
