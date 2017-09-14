package io.vercy.brick.proxy;

/** Mutable value class representing a single brick */
class BrickPayload {
    private BrickColor color;
    private int length;

    public BrickColor getColor() {
        return color;
    }

    public void setColor(BrickColor color) {
        this.color = color;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrickPayload that = (BrickPayload) o;

        if (length != that.length) return false;
        return color == that.color;
    }

    @Override
    public int hashCode() {
        int result = color != null ? color.hashCode() : 0;
        result = 31 * result + length;
        return result;
    }
}
