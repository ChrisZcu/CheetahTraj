package model;

public enum Colour {
    DEEP_BLUE(0), SKY_BLUE(1), LIGHT_YELLOW(2), YELLOW(3), ORANGE(4), RED(5),
    LIGHT_GREY(6), DARK(7), WHITE(8);

    public int value;

    Colour(int value) {
        this.value = value;
    }

    public static Colour[] getColor() {
        return new Colour[]{DEEP_BLUE, SKY_BLUE, LIGHT_YELLOW, YELLOW, ORANGE, RED, LIGHT_GREY, DARK, WHITE};
    }
}
