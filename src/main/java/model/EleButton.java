package model;


import processing.core.PApplet;
import util.PSC;

import static model.Colour.*;

public class EleButton extends Element {
    public EleButton(int x, int y, int width, int height, int eleId, String eleName) {
        super(x, y, width, height);
        this.eleId = eleId;
        this.eleName = eleName;
    }

    @Override
    public void render(PApplet pApplet) {
        pApplet.noStroke();
        pApplet.fill(PSC.COLOR_LIST[LIGHT_GREY.value].getRGB());
        pApplet.rect(x, y, this.width, this.height);

        pApplet.fill(PSC.COLOR_LIST[WHITE.value].getRGB());
        pApplet.textAlign(PApplet.CENTER, PApplet.CENTER);
        pApplet.text(eleName, x + (width / 2), y + (height / 2));
        pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
    }


    @Override
    public int getEleId() {
        return eleId;
    }

    public void colorExg() {
    }
}