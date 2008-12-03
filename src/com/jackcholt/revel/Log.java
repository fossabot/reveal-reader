package com.jackcholt.revel;

public class Log {
    
    public static void d(String activity, String message) {
        boolean messageSent = false;
        
        try {
            android.util.Log.d(activity,message);
            messageSent = true;
        } catch (Throwable e) {
            if (!messageSent) System.out.println("[" + activity + "][DEBUG] " + message);
        }
    }

    public static void v(String activity, String message) {
        boolean messageSent = false;
        
        try {
            android.util.Log.v(activity,message);
            messageSent = true;
        } catch (Throwable e) {
            if (!messageSent) System.out.println("[" + activity + "][VERBOSE] " + message);
        }
    }
    
    public static void i(String activity, String message) {
        boolean messageSent = false;
        
        try {
            android.util.Log.i(activity,message);
            messageSent = true;
        } catch (Throwable e) {
            if (!messageSent) System.out.println("[" + activity + "][INFO] " + message);
        }
    }
    public static void w(String activity, String message) {
        boolean messageSent = false;
        
        try {
            android.util.Log.w(activity,message);
            messageSent = true;
        } catch (Throwable e) {
            if (!messageSent) System.out.println("[" + activity + "][WARN] " + message);
        }
    }
    public static void e(String activity, String message) {
        boolean messageSent = false;
        
        try {
            android.util.Log.e(activity,message);
            messageSent = true;
        } catch (Throwable e) {
            if (!messageSent) System.out.println("[" + activity + "][ERROR] " + message);
        }
    }

}
