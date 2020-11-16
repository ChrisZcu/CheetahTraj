package vqgs.app;

import de.tototec.cmdoption.CmdlineParser;
import vqgs.util.Config;

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
            vqgs.origin.util.screenShot.main(args);
        else vqgs.origin.util.draw_with_color_sz.main(args);
    }
}
