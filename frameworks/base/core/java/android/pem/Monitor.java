package android.pem;

import android.os.RemoteException;
import android.util.Log;
import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.pm.ParceledListSlice;

import java.util.*;
import java.io.*;
import java.security.LogicDroid;

public class Monitor{
  static class MonitorInfo
  {
    public int UID;
    public String name;

    public MonitorInfo(int UID, String name)
    {
      this.UID = UID;
      this.name = name;
    }

    public void appendName(String name)
    {
      this.name = this.name + ", " + name;
    }
  }

  protected static int app_num;


  public static final int ROOT_UID = 0;
  public static final String ROOT_APP = "ROOT";
  public static final int INTERNET_UID = 1100;
  public static final String INTERNET_APP = "INTERNET";
  public static final int SMS_UID = 1101;
  public static final String SMS_APP = "SMS";
  public static final int LOCATION_UID = 1102;
  public static final String LOCATION_APP = "LOCATION";
  public static final int CONTACT_UID = 1103;
  public static final String CONTACT_APP = "CONTACT";

  public static boolean notInitialized = true;
  private static HashMap<String, Integer> relMapping = null;
  private static HashMap<Integer, Integer> mapping;
  private static MonitorInfo[] componentInfo;

  public static boolean isVirtualUID(int UID)
  {
    if (UID == ROOT_UID|| UID == INTERNET_UID|| UID == SMS_UID|| UID == LOCATION_UID|| UID == CONTACT_UID)
      return true;
    return false;
  }

  public static String virtualAppName(int UID)
  {
    switch (UID)
    {
      case ROOT_UID : return ROOT_APP;
      case INTERNET_UID : return INTERNET_APP;
      case SMS_UID : return SMS_APP;
      case LOCATION_UID : return LOCATION_APP;
      case CONTACT_UID : return CONTACT_APP;
    }
    return null;
  }

  private static boolean initializeMonitor()
  {
    Log.i("track - Monitor", "Initializing Monitor...");
    ArrayList<ApplicationInfo> apps = null;
    try
    {
      ParceledListSlice parcels = AppGlobals.getPackageManager().getInstalledApplications(0, "", 0);
      apps = new ArrayList<ApplicationInfo>();
      parcels.populateList(apps, ApplicationInfo.CREATOR);
    }
    catch (RemoteException re)
    {
      Log.i("track - Monitor", "InitializeMonitor : failed to get app list");
      notInitialized = true;
      app_num = 0;
      return false;
    }
    catch (Exception ex)
    {
      Log.i("track - Monitor", "InitializeMonitor : " + ex);
      notInitialized = true;
      app_num = 0;
      return false;
    }
    mapping = new HashMap<Integer, Integer>();
    // Doesn't matter if the list application is not sorted
    int appIdx = 0;
    ArrayList<MonitorInfo> tempArr = new ArrayList<MonitorInfo>();
    tempArr.add(new MonitorInfo(ROOT_UID, ROOT_APP));
    tempArr.add(new MonitorInfo(INTERNET_UID, INTERNET_APP));
    tempArr.add(new MonitorInfo(SMS_UID, SMS_APP));
    tempArr.add(new MonitorInfo(LOCATION_UID, LOCATION_APP));
    tempArr.add(new MonitorInfo(CONTACT_UID, CONTACT_APP));
    mapping.put(ROOT_UID, 0);
    mapping.put(INTERNET_UID, 1);
    mapping.put(SMS_UID, 2);
    mapping.put(LOCATION_UID, 3);
    mapping.put(CONTACT_UID, 4);
    appIdx = 5;
    for (ApplicationInfo ai : apps)
    {
      if (!mapping.containsKey(ai.uid))
      {
        mapping.put(ai.uid, appIdx);
        tempArr.add(new MonitorInfo(ai.uid, ai.processName));
        appIdx++;
      }
      else
      {
        tempArr.get(mapping.get(ai.uid)).appendName(ai.processName);
      }
    }
    app_num = tempArr.size();
    componentInfo = tempArr.toArray(new MonitorInfo[app_num]);

    appIdx = 0;
    // Additional array to communicate with kernel
    int UID[] = new int[app_num - 5];
    for (int i = 5; i < app_num; i++)
    {
      UID[i - 5] = tempArr.get(i).UID;
    }
    LogicDroid.initializeMonitor(UID);

    if (relMapping == null) relMapping = new HashMap<String, Integer>();

    for (int i = 0; i < app_num; i++)
    {
      MonitorInfo mi = tempArr.get(i);
      Log.i("track - Monitor", "   - item(" + appIdx + ") : " + mi.name + " (" + mi.UID + ")");
      appIdx++;
    }

    notInitialized = false;
    return true;
  }

  public static boolean checkEvent(Event ev, long timestamp)
  {
    if (notInitialized)
    {
      if (!initializeMonitor()) return false; // If the monitor is failed to initialize just let the event pass through
    }
    if (ev.varCount != 2 || !ev.rel.equalsIgnoreCase("call"))
    {
      return false; // only handles call event
    }

    boolean result = false;
    if (relMapping.containsKey("call"))
    {
      long timer = System.currentTimeMillis();
      result = LogicDroid.checkEvent(ev.vars.get(0), ev.vars.get(1));
      Log.i("LogicDroid", "Finished checking PE for (" + ev.vars.get(0) + ", " + ev.vars.get(1) + ") resulting in " + result);
    }
    return result;
  }

  public static void renewMonitorVariable(String rel, boolean value, int ... UID)
  {
    if (notInitialized)
    {
      if (!initializeMonitor()) return; // If the monitor is failed to initialize just ignore
    }

    if(relMapping.containsKey(rel))
    {
      int idx = relMapping.get(rel);
      LogicDroid.modifyStaticVariable(UID[0], value, relMapping.get(rel));
      String nm = componentInfo[mapping.get(UID[0])].name;
      if(nm == null) nm = "";
      Log.i("LogicDroid", "renew monitor status for app " + nm + " " + rel + " to " + Boolean.toString(value));
    }
    else
    {
      if (rel.startsWith("monitorvars:"))
      {
        Log.i("LogicDroid", "Setting monitor relation for " + rel.substring(12, rel.length()) + " to " + UID[0]);
        relMapping.put(rel.substring(12, rel.length()), UID[0]);
      }
    }
  }
}
