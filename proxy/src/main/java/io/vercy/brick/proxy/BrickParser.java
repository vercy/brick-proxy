package io.vercy.brick.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class BrickParser {
    static final Logger log = LoggerFactory.getLogger(BrickParser.class);
    static AtomicInteger totalBricksReceived = new AtomicInteger(0);
    static final int BRICK_REQUEST_BYTE_COUNT = 2;

    BrickPayload read(InputStream in) throws IOException {
        if(in.available() < 2)
            return null;

        int rawColor = in.read();
        int rawLength = in.read();

        totalBricksReceived.incrementAndGet();
        AnsiColor bColor = AnsiColor.parse(rawColor - '0');
        int bLength = rawLength - '0';
        if (bColor == null || bLength <= 0) {
            log.warn("{} > Skipping invalid brick: {color: {}, length: {}} colorByte: {}, lengthByte: {}", bColor, bLength, rawColor, rawLength);
            return null;
        }

        return new BrickPayload(bColor, bLength);
    }
}
