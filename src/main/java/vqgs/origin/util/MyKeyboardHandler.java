package origin.util;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.interactions.KeyboardHandler;
import processing.core.PApplet;
import processing.event.KeyEvent;

public class MyKeyboardHandler extends KeyboardHandler {
    private final PApplet pApplet;

    public MyKeyboardHandler(PApplet pApplet, UnfoldingMap... unfoldingMaps) {
        super(pApplet, unfoldingMaps);
        this.pApplet = pApplet;
    }

    @Override
    public void keyEvent(KeyEvent e) {
        if (e.getKey() == 'q') {
            pApplet.exit();
        }
    }
}