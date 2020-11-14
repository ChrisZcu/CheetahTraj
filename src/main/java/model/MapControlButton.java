package model;

import processing.core.PApplet;
import util.PSC;

import static model.Colour.LIGHT_GREY;
import static model.Colour.WHITE;

public class MapControlButton extends EleButton {
    private Colour color = LIGHT_GREY;
    private boolean control = false;
    private boolean linked = false;

    public MapControlButton(int x, int y, int width, int height, int eleId, String eleName) {
        super(x, y, width, height, eleId, eleName);
    }

    @Override
    public void render(PApplet pApplet) {
        pApplet.noStroke();
        pApplet.fill(PSC.COLOR_LIST[color.value].getRGB());
        pApplet.rect(x, y, width, height);

        pApplet.fill(PSC.COLOR_LIST[WHITE.value].getRGB());
        pApplet.textAlign(CENTER, CENTER);
        pApplet.text(eleName, x + (width / 2), y + (height / 2));
        pApplet.textAlign(LEFT, TOP);
    }

    @Override
    public int getEleId() {
        return eleId;
    }

    public void setColor(Colour color) {
        this.color = color;
    }

    public boolean isControl() {
        return control;
    }

    public void setControl(boolean control) {
        this.control = control;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }

    @Override
    public void colorExg() {
        if (color.value == 6) {
            color = Colour.getColor()[7];
        } else {
            color = Colour.getColor()[6];
        }
    }
}
