import de.fhpotsdam.unfolding.UnfoldingMap;
import de.tototec.cmdoption.CmdlineParser;
import processing.core.PApplet;
import util.*;
import vfgs.PreProcess;
import vfgs.Quality;
import vfgs.VFGS;
import vfgs.VFGSColor;

/**
 * Run all processes conveniently.
 * <br> <b>Warning: May lead to out of memory exception.</b>
 * Only use it when memory is sufficient.
 * <br> JVM options: -Xmn48g -Xms48g -Xmx48g
 */
public class StaticCal extends PApplet {
    @Override
    public void setup() {
        UnfoldingMap map = new UnfoldingMap(this);
        DM.printAndLog(PSC.LOG_PATH, PSC.str());

        for (WF process : PSC.PROCESS_LIST) {
            switch (process) {
                case PRE_PROCESS:
                    PreProcess.main(map);
                    break;
                case VFGS_CAL:
                    VFGS.main(map);
                    break;
                case VFGS_COLOR_CAL:
                    VFGSColor.main(map);
                    break;
                case QUALITY_CAL:
                    Quality.main(map);
                    break;
            }

            if (WF.error) {
                EmailSender.sendEmail();
                exit();
                return;
            }
        }

        WF.status = WF.END;
        EmailSender.sendEmail();

        exit();
    }

    public static void main(String[] args) {
        Config config = new Config();
        CmdlineParser cp = new CmdlineParser(config);
        cp.setAggregateShortOptionsWithPrefix("-");
        cp.setProgramName("Static Calculation");
        cp.parse(args);
        if (config.help) {
            cp.usage();
        } else if (config.dataset == null) {
            System.err.println("Dataset file path is necessary!!!");
            System.err.println("Run with --help/-h for parameter information");
            System.exit(0);
        } else {
            PSC.RATE_LIST = new double[]{Double.parseDouble(config.rate)};
            PSC.DELTA_LIST = new int[]{config.delta};
            PApplet.main(StaticCal.class.getName());
        }
    }
}
