package util;

import java.util.*;

import de.tototec.cmdoption.CmdOption;
import de.tototec.cmdoption.CmdlineParser;

public class Config {
    @CmdOption(names = {"--help", "-h"}, description = "Show this help", isHelp = true)
    public boolean help;

    @CmdOption(names = {"--color", "-c"}, description = "Choose the render type: color or not, not color by default")
    public boolean color;

    @CmdOption(names = {"--dataset", "-s"}, args = {"DATASET"}, description = "The dataset path")
    public String dataset = null;

    @CmdOption(names = {"--vfgs", "-g"}, args = {"VFGS"}, description = "VFGS/VFGS+ calculation result directory path")
    public String vfgs = null;

    @CmdOption(names = {"--delta", "-d"}, args = {"DELTA"}, description = "The delta in VFGS+, 64 by default")
    public int delta = 64;

    @CmdOption(names = {"--rate", "-r"}, args = {"RATE"}, description = "The sampling rate in VFGS/VFGS+, 0.005 by default")
    public String rate = "0.005";

    @CmdOption(names = {"--datatype", "-t"}, args = {"DATATYPE"}, description = "The data type, including Shenzhen data(0) and Portugal dataset(1)")
    public int type = 0;


    public static void main(String[] args) {
        Config config = new Config();
        CmdlineParser cp = new CmdlineParser(config);
        cp.setProgramName("Config");
        cp.setAggregateShortOptionsWithPrefix("-");
        cp.parse(new String[]{"-h"});
        if (config.help) {
            cp.usage();
            System.exit(0);
        }
    }
}