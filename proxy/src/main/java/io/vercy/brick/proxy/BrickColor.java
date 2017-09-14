package io.vercy.brick.proxy;

/** Colors ordinals are ansi color constants */
enum BrickColor {
    BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE;

    public static BrickColor parse(int value) {
        switch(value) {
            case 0: return BLACK;
            case 1: return RED;
            case 2: return GREEN;
            case 3: return YELLOW;
            case 4: return BLUE;
            case 5: return MAGENTA;
            case 6: return CYAN;
            case 7: return WHITE;
            default: return null;
        }
    }
}
