package io.vercy.brick.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class BrickReader {
    static final Logger log = LoggerFactory.getLogger(BrickReader.class);
    static final int BRICK_REQUEST_BYTE_COUNT = 2;

    enum ReadState { EOF, INVALID_BRICK, READ_OK}

    /** Read a brick into the target BrickPayload instance. Return true if the end of stream is reached */
    ReadState read(InputStream in, BrickPayload target) throws IOException {
        int rawColor = in.read();
        if(rawColor < 0)
            return ReadState.EOF;
        int rawLength = in.read();
        if(rawLength < 0)
            return ReadState.EOF;

        BrickColor bColor = BrickColor.parse(rawColor - '0');
        int bLength = rawLength - '0';
        if (bColor == null || bLength <= 0) {
            log.warn("{} > Skipping invalid brick: {color: {}, length: {}} colorByte: {}, lengthByte: {}", bColor, bLength, rawColor, rawLength);
            return ReadState.INVALID_BRICK;
        }

        target.setLength(bLength);
        target.setColor(bColor);

        return ReadState.READ_OK;
    }
}
