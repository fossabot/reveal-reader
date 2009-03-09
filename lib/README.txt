Welcome to Flurry Analytics!

This archive should contain these files:
 - FlurryAgent.jar : The library containing Flurry's collection and reporting code.  Include this library in your application.
 - ProjectApiKey.txt : This file contains the name of your project and your project's API key.
 - README.txt : This text file containing instructions.  

To integrate Flurry Analytics into your Android application:

1. Add the FlurryAgent.jar file to your classpath.
   - If you're using Eclipse, modify your Java Build Path, and choose Add External JAR...
   - If you're using the SDK tools directly, should be able to drop it into your libs folder and the ant task will pick it up.
   
2. Configure AndroidManifest.xml:
   Required Permission:
   - android.permission.INTERNET
      is required to send analytics data back to the flurry servers
   
   Optional Permissions:
   - android.permission.ACCESS_COARSE_LOCATION or android.permission.ACCESS_FINE_LOCATION
      If your application has location permissions, analytics will track where your application is being used.  Without this, only country level location information will be available.
   - android.permission.READ_PHONE_STATE
      is recommended to uniquely identify the phones on which your application is installed, so that you can track unique users by devices.  If this permission is not enabled, Flurry Analytics may not be able to distinguish a repeat installation from a new user. 
      
   Specify a versionName attribute in the manifest to have data reported under that version name.
   
3. Add calls to onStartSession and onEndSession
  - Insert a call to FlurryAgent.onStartSession(Context, String), passing it a reference to a Context object (such as an Activity or Service), and your project's API key.  The point at which you define a session beginning depends upon how your application or service is used.  We recommend using the onStart method of each Activity in your application.
  - Insert a call to FlurryAgent.onEndSession when a session is complete.  We recommend using the onStop method of each Activity in your application.
  - If onStartSession is called within 10 seconds of onEndSession, then the session will be resumed, instead of a new session being created.  Session length, usage frequency, events and errors will continue to be tracked as part of the same session.  This ensures that as a user transitions from one Activity to another in your application that they will not have a separate session tracked for each Activity.  If you want to track Activity usage, we recommend using onEvent, described below.  If you wish to change the window during which a session can be resumed, call FlurryAgent.setContinueSessionMillis(long milliseconds) before the first call to FlurryAgent.onStartSession.
  
  NOTE: If you do not want your metrics tracked with detailed location, even though your app already has permission, add a call to FlurryAgent.setReportLocation(false) before calling FlurryAgent.onStartSession() and no detailed location information will be sent.

That's all you need to do to begin receiving basic metric data.  You can use the following methods (during a session only) to report additional data:

 - FlurryAgent.onEvent(String eventId, Map<String, String> parameters).  Use onEvent to track user events that happen during a session.  You can track how many times each event occurs, what order events happen in, as well as what the most common parameters are for each event.  This can be useful for measuring how often users take various actions, or what sequences of actions they usually perform.
  Each project supports a maximum of 100 events, and each event id, parameter key, and parameter value must be no more than 255 characters in length.  Each event can have no more than 10 parameters.  The parameter argument is optional, and may be null.

 - FlurryAgent.onError(String errorId, String message, String errorClass).  Use onError to report application errors.  Flurry will report the first 10 errors to occur in each session.

Please let us know if you have any questions. If you need any help, just email androidsupport@flurry.com!

Cheers,
The Flurry Team
http://www.flurry.com
androidsupport@flurry.com
