package io.vercy.brick.proxy;

class BrickPayload {
    private final AnsiColor color;
    private final int length;

    BrickPayload(AnsiColor color, int length) {
        this.color = color;
        this.length = length;
    }

    public AnsiColor getColor() {
        return color;
    }

    public int getLength() {
        return length;
    }

    static BrickPayload from(String c, String l) {
        if(c == null || c.isEmpty())
            throw new IllegalArgumentException("The color cannot be empty");

        return new BrickPayload(
                AnsiColor.valueOf(c.toUpperCase()),
                Integer.parseInt(l));
    }

    @Override
    public String toString() {
        return color + "-"+length;
    }

    String render() {
        StringBuilder sb = new StringBuilder("\u001b[3")
                .append(color.ordinal())
                .append('m');

        for(int i = 0; i < length; i++) {
            sb.append('█');
        }
        sb.append("\u001b[0m");
        return sb.toString();
    }
}
