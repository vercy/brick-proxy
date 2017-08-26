package io.vercy.brick.shop;

import java.util.Random;

public class BrickSetFactory {

    private static final int ANSI_COLOR_LIMIT = 8;
    private static final int BRICK_LENGTH_LIMIT = 15;
    private static final int BRICK_SET_PIECE_COUNT_LIMIT = 8;

    private Random random = new Random();

    public byte[] nextBrickSet() {
        int pieceCount = nextBrickSetPieceCount();
        byte[] brickSet = new byte[pieceCount * 2];
        for(int i = 0; i < pieceCount; i++) {
            brickSet[i << 1] = (byte)nextColor();
            brickSet[(i << 1) + 1] = (byte)nextLength();
        }
        return brickSet;
    }

    private int nextBrickSetPieceCount() {
        return random.nextInt(BRICK_SET_PIECE_COUNT_LIMIT);
    }

    private int nextColor() {
        return random.nextInt(ANSI_COLOR_LIMIT);
    }

    private int nextLength() {
        int length = random.nextInt(BRICK_LENGTH_LIMIT) + 1;
        return length;
    }
}
