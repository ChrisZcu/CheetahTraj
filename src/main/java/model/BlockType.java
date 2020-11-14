package model;

public enum BlockType {
    NONE(0), FULL(1), RAND(2), VFGS(3);
    int value;

    BlockType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
