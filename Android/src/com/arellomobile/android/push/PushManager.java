//
//  PushManager.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.arellomobile.android.push;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.gcm.GCMRegistrar;
import org.json.JSONException;
import org.json.JSONObject;

public class PushManager
{
    // app id in the backend
    private volatile String mAppId;
    volatile static String mSenderId;

    // message id in the notification bar
    public static int MESSAGE_ID = 1001;

    private static final String HTML_URL_FORMAT = "https://cp.pushwoosh.com/content/%s";

    public static final String REGISTER_EVENT = "REGISTER_EVENT";
    public static final String REGISTER_ERROR_EVENT = "REGISTER_ERROR_EVENT";
    public static final String UNREGISTER_EVENT = "UNREGISTER_EVENT";
    public static final String UNREGISTER_ERROR_EVENT = "UNREGISTER_ERROR_EVENT";
    public static final String PUSH_RECEIVE_EVENT = "PUSH_RECEIVE_EVENT";

    private Context mContext;
    private Bundle mLastBundle;
    public static Boolean mSimpleNotification;

    PushManager(Context context)
    {
        checkNotNull(context, "context");
        mContext = context;
        mAppId = PreferenceUtils.getApplicationId(context);
        mSenderId = PreferenceUtils.getSenderId(context);
    }

    public PushManager(Context context, String appId, String senderId)
    {
        this(context);

        mAppId = appId;
        mSenderId = senderId;
        PreferenceUtils.setApplicationId(context, mAppId);
        PreferenceUtils.setSenderId(context, senderId);
    }

    /**
     * @param savedInstanceState if this method calls in onCreate method, can be null
     * @param context            current context
     */
    public void onStartup(Bundle savedInstanceState, Context context)
    {
        checkNotNullOrEmpty(mAppId, "mAppId");
        checkNotNullOrEmpty(mSenderId, "mSenderId");

        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(context);
        // Make sure the manifest was properly set - comment out this line
        // while developing the app, then uncomment it when it's ready.
        GCMRegistrar.checkManifest(context);

        final String regId = GCMRegistrar.getRegistrationId(context);
        if (regId.equals(""))
        {
            // Automatically registers application on startup.
            GCMRegistrar.register(context, mSenderId);
        }
        else
        {
            if (context instanceof Activity)
            {
                if (((Activity) context).getIntent().hasExtra(PushManager.PUSH_RECEIVE_EVENT))
                {
                    // if this method calls because of push message, we don't need to register
                    return;
                }
            }

            String oldAppId = PreferenceUtils.getApplicationId(context);

            if (!oldAppId.equals(mAppId) || savedInstanceState == null)
            {
                // if not register yet or an other id detected
                DeviceRegistrar.registerWithServer(context, regId);
            }
        }
    }

    /**
     * Note this will take affect only after PushGCMIntentService restart if it is already running
     */
    public void setMultiNotificationMode()
    {
        mSimpleNotification = false;
    }

    /**
     * Note this will take affect only after PushGCMIntentService restart if it is already running
     */
    public void setSimpleNotificationMode()
    {
        mSimpleNotification = true;
    }

    private void checkNotNullOrEmpty(String reference, String name)
    {
        checkNotNull(reference, name);
        if (reference.length() == 0)
        {
            throw new IllegalArgumentException(
                    String.format("Please set the %1$s constant and recompile the app.", name));
        }
    }

    private void checkNotNull(Object reference, String name)
    {
        if (reference == null)
        {
            throw new IllegalArgumentException(
                    String.format("Please set the %1$s constant and recompile the app.", name));
        }
    }

    public void unregister()
    {
        GCMRegistrar.unregister(mContext);
    }

    public String getCustomData()
    {
        if (mLastBundle == null)
        {
            return null;
        }

        String customData = (String) mLastBundle.get("u");
        return customData;
    }

    public boolean onHandlePush(Activity activity)
    {
        Bundle pushBundle = activity.getIntent().getBundleExtra("pushBundle");
        if (null == pushBundle || null == mContext)
        {
            return false;
        }

        mLastBundle = pushBundle;

        JSONObject dataObject = new JSONObject();
        try
        {
            if (pushBundle.containsKey("title"))
            {
                dataObject.put("title", pushBundle.get("title"));
            }
            if (pushBundle.containsKey("u"))
            {
                dataObject.put("userdata", new JSONObject(pushBundle.getString("u")));
            }
        } catch (JSONException e)
        {
            // pass
        }

        PushEventsTransmitter.onMessageReceive(mContext, dataObject.toString());

        // push message handling
        String url = (String) pushBundle.get("h");

        if (url != null)
        {
            url = String.format(HTML_URL_FORMAT, url);

            // show browser
            Intent intent = new Intent(activity, PushWebview.class);
            intent.putExtra("url", url);
            activity.startActivity(intent);
        }

        return true;
    }
}
