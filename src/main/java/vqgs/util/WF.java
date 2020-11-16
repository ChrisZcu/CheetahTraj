package vqgs.util;

/**
 * Algorithm Work Flow
 * Used to work flow control & resources management.
 */
public enum WF {
	BEGIN, PRE_PROCESS, VFGS_CAL, VFGS_COLOR_CAL, QUALITY_CAL, END;

	public static WF status = WF.BEGIN;
	public static boolean error = false;
}
