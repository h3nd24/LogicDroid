package java.security;

public final class LogicDroid
{
	public static native void initializeMonitor(int[] UID);
	public static native void modifyStaticVariable(int UID, boolean value, int rel);
	public static native boolean checkEvent(int caller, int target);
}
