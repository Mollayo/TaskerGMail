package me.lebob.taskergmail;

import static me.lebob.taskergmail.MainActivity.readFromFile;
import static me.lebob.taskergmail.receivers.ActionSettingReceiver.sendMessage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.AuthState;

import org.json.JSONException;

import me.lebob.taskergmail.utils.ActionBundleManager;
import me.lebob.taskergmail.utils.BundleScrubber;
import me.lebob.taskergmail.utils.Constants;
import me.lebob.taskergmail.utils.TaskerPlugin;


public class ActionPluginActivity extends AppCompatActivity {

    String oAuth="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_action);

        final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";
        final String previousBlurb = getIntent().getStringExtra(EXTRA_STRING_BLURB);
        final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
        final Bundle previousBundle = getIntent().getBundleExtra(EXTRA_BUNDLE);
        BundleScrubber.scrub(previousBundle);
        if (isBundleValid(previousBundle) && previousBlurb!=null)
            onPostCreateWithPreviousResult(previousBundle,previousBlurb);

        findViewById(R.id.test_email).setOnClickListener(view -> ActionPluginActivity.this.sendTestEMail());

        AuthState authState;
        oAuth=readFromFile(getApplicationContext());
        if (oAuth.length()>0)
            try {
                authState=AuthState.jsonDeserialize(oAuth);
                String status = authState.isAuthorized() ?
                        getString(R.string.authenticated) : getString(R.string.not_authenticated);
                TextView tvStatus = findViewById(R.id.status);
                tvStatus.setText(status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }


    // Method that checks if bundle is valid
    public boolean isBundleValid(Bundle bundle) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::isBundleValid");
        return ActionBundleManager.isBundleValid(bundle);
    }

    // Method that uses previously saved bundle
    public void onPostCreateWithPreviousResult(Bundle bundle,String blurb) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::onPostCreateWithPreviousResult");

        final String recipient = ActionBundleManager.getRecipient(bundle);
        ((EditText) findViewById(R.id.email_address)).setText(recipient);

        final String subject = ActionBundleManager.getSubject(bundle);
        ((EditText) findViewById(R.id.email_subject)).setText(subject);

        final String body = ActionBundleManager.getBody(bundle);
        ((EditText) findViewById(R.id.email_body)).setText(body);
    }

    // Method that returns the bundle to be saved
    public Bundle getResultBundle() {

        Log.v(Constants.LOG_TAG, "ActionPluginActivity::getResultBundle");
        String recipient = ((EditText) findViewById(R.id.email_address)).getText().toString();
        String subject = ((EditText) findViewById(R.id.email_subject)).getText().toString();
        String body = ((EditText) findViewById(R.id.email_body)).getText().toString();

        Bundle bundle = ActionBundleManager.generateBundle(oAuth, recipient, subject, body);

        if (bundle == null) {
            Context context = getApplicationContext();
            String error = ActionBundleManager.getErrorMessage(context, oAuth, recipient, subject, body);
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            } else {
                Log.e(Constants.LOG_TAG, "Null bundle, but no error");
            }
            return null;
        }

        // This is to specify that the Mac address and the message can be Tasker variables
        if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)) {
            TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[]{
                    Constants.BUNDLE_STRING_RECIPIENT,
                    Constants.BUNDLE_STRING_SUBJECT,
                    Constants.BUNDLE_STRING_BODY});
        }
        return bundle;
    }

    // Method that creates summary of bundle
    public String getResultBlurb(Bundle bundle) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::getResultBlurb");
        return ActionBundleManager.getBundleBlurb(bundle);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onBackPressed() {

        // The bundle to save the user parameters
        final Bundle resultBundle = getResultBundle();
        if (!isBundleValid(resultBundle))
            return;

        super.onStop();
        Intent intent = new Intent();
        String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
        intent.putExtra(EXTRA_BUNDLE, resultBundle);

        // For the explanation text of the plugin
        String blurbStr=getResultBlurb(resultBundle);
        String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";
        intent.putExtra(EXTRA_STRING_BLURB, blurbStr);

        // For the synchronous execution
        if (TaskerPlugin.Setting.hostSupportsSynchronousExecution( getIntent().getExtras()))
            TaskerPlugin.Setting.requestTimeoutMS( intent, 10000 );

        setResult(RESULT_OK, intent);
        finish();
    }

    private void sendTestEMail()
    {
        AppAuthService authService = AppAuthService.getInstance(this);
        oAuth=readFromFile(getApplicationContext());
        if (oAuth.length()>0) {
            try {
                authService.authState = AuthState.jsonDeserialize(oAuth);
                if (!authService.authState.isAuthorized()) {
                    Log.e(Constants.LOG_TAG, "GMail not authorized");
                    Toast.makeText(getApplicationContext(), "GMail not authorized", Toast.LENGTH_LONG).show();
                    return;
                }

            } catch (JSONException e) {
                Log.e(Constants.LOG_TAG, "Problem parsing the json file: " + e);
                Toast.makeText(getApplicationContext(), "Problem parsing the json file: " + e, Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Log.e(Constants.LOG_TAG, "Problem reading the json file");
            Toast.makeText(getApplicationContext(), "Problem reading the json file", Toast.LENGTH_LONG).show();
            return;
        }

        authService.getAuthState().performActionWithFreshTokens(authService.service, (accessToken, idToken, ex) -> {
            if (ex != null) {
                Log.e(Constants.LOG_TAG, "Token refresh problem: " + ex);
                Toast.makeText(getApplicationContext(),"Token problem: " + ex,Toast.LENGTH_LONG).show();
                return;
            }
            // Sending a message
            String recipient = ((EditText) findViewById(R.id.email_address)).getText().toString();
            String subject = ((EditText) findViewById(R.id.email_subject)).getText().toString();
            String body = ((EditText) findViewById(R.id.email_body)).getText().toString();
            if (!sendMessage(accessToken,recipient,subject,body))
                Toast.makeText(getApplicationContext(),"Failed to send the test email",Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(),"Test mail sent successfully",Toast.LENGTH_SHORT).show();
        });
    }

}

