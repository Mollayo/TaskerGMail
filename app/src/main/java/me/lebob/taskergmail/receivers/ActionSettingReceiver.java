package me.lebob.taskergmail.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.io.BaseEncoding;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import me.lebob.taskergmail.utils.ActionBundleManager;
import me.lebob.taskergmail.utils.Constants;
import me.lebob.taskergmail.utils.TaskerPlugin;


public class ActionSettingReceiver  extends BroadcastReceiver {

    // Method that checks whether bundle is valid
    protected boolean isBundleValid(Bundle bundle) {
        Log.v(Constants.LOG_TAG, "ActionSettingReceiver::isBundleValid");
        return ActionBundleManager.isBundleValid(bundle);
    }


    // Method responsible for the connection and data transmission. Assumes bluetooth is enabled
    // and the device has been paired with.
    @Override
    //protected void firePluginSetting(Context context, Bundle bundle)
    public void onReceive(Context context, Intent intent)
    {
        Log.v(Constants.LOG_TAG, "ActionSettingReceiver::firePluginSetting");

        final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
        final Bundle localeBundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if (!isBundleValid(localeBundle)) {
            Log.v(Constants.LOG_TAG, "localBundle is invalid"); //$NON-NLS-1$
            return;
        }

        // Get the user parameters
        final String oAuth = ActionBundleManager.getOAuth(localeBundle);
        final String recipient = ActionBundleManager.getRecipient(localeBundle);
        final String subject = ActionBundleManager.getSubject(localeBundle);
        final String body = ActionBundleManager.getBody(localeBundle);
        boolean error=false;

        try {
            AuthState authState=AuthState.jsonDeserialize(oAuth);
            final Context appContext=context.getApplicationContext();
            final Intent fireIntentFromHost=intent;
            AuthorizationService service = new AuthorizationService(context.getApplicationContext());
            authState.performActionWithFreshTokens(service, (accessToken, idToken, ex) -> {
                if (ex != null) {
                    Log.e(Constants.LOG_TAG, "Token refresh problem: " + ex);
                    TaskerPlugin.Setting.signalFinish( appContext, fireIntentFromHost, TaskerPlugin.Setting.RESULT_CODE_FAILED, null );
                    return;
                }
                // Sending the message
                boolean error1 =!sendMessage(accessToken, recipient, subject, body);
                if (error1)
                {
                    TaskerPlugin.Setting.signalFinish( appContext, fireIntentFromHost, TaskerPlugin.Setting.RESULT_CODE_FAILED, null );
                    Log.e(Constants.LOG_TAG, "Failed to send the email");
                }
                else
                    TaskerPlugin.Setting.signalFinish( appContext, fireIntentFromHost, TaskerPlugin.Setting.RESULT_CODE_OK, null );
            });
        } catch (Exception e) {
            error=true;
            Log.e(Constants.LOG_TAG, "Failed to send the email: " + e);
            e.printStackTrace();
        }

        if ( isOrderedBroadcast() )
        {
            if (error)
                setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);
            else {
                setResultCode(TaskerPlugin.Setting.RESULT_CODE_PENDING);
            }
        }
    }

    static public boolean sendMessage(String accessToken, String recipient, String subject, String body)
    {
        // To avoid the strictmode policy violation
        // Normally, HTTP requests should not be made in the UI thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (recipient.isEmpty()) {
            Log.e(Constants.LOG_TAG,"Email is not sent because the address is empty");
            return false;
        }
        try {
            AccessToken tok = new AccessToken(accessToken,null);
            GoogleCredentials credentials=new GoogleCredentials(tok);
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

            // Create the gmail API client
            Gmail gmail = new Gmail.Builder(new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("TaskerGMail")
                    .build();

            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
            mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(body);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);
            Message message = new Message();
            message.setRaw(BaseEncoding.base64Url().encode(baos.toByteArray()));

            gmail.users().messages().send("me", message).execute();
            return true;
        } catch (Exception ex) {
            Log.e(Constants.LOG_TAG, "Error sending message", ex);
        }
        return false;
    }

}
