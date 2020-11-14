package app;

import de.tototec.cmdoption.CmdlineParser;
import util.Config;

public class ScreenShot {
    public static void main(String[] args) {

        Config config = new Config();
        CmdlineParser cp = new CmdlineParser(config);
        cp.setAggregateShortOptionsWithPrefix("-");
        cp.setProgramName("Screenshot");
        cp.parse(args);
        if (config.help) {
            cp.usage();
            System.exit(0);
        }
        if (config.color)
            origin.util.screenShot.main(args);
        else origin.util.draw_with_color_sz.main(args);
    }
}
