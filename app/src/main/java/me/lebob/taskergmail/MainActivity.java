package me.lebob.taskergmail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.AuthState;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import me.lebob.taskergmail.utils.Constants;


public class MainActivity extends AppCompatActivity {
    AppAuthService authService;
    TextView tvStatus;
    String authStateJSon="";
    ActivityResultLauncher<Intent> authServiceResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        authService.getAuthToken(result.getData(), () -> {
                            MainActivity.this.setAuthStatus();

                            // Save the authorization to a file
                            AuthState authState = authService.getAuthState();
                            authStateJSon = authState.jsonSerializeString();
                            writeToFile(authStateJSon, getApplicationContext());
                        });
                    } else {
                        Toast.makeText(MainActivity.this, R.string.login_cancelled, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.v(Constants.LOG_TAG, "MainActivity::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = AppAuthService.getInstance(this);

        findViewById(R.id.authorize).setOnClickListener(view -> MainActivity.this.handleLogin());
        tvStatus = findViewById(R.id.status);


        authStateJSon=readFromFile(getApplicationContext());
        if (authStateJSon.length()>0)
            try {
                authService.authState=AuthState.jsonDeserialize(authStateJSon);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        setAuthStatus();
    }

    private void handleLogin()
    {
        if (authService.clientId.length()==0) {
            Toast.makeText(this,
                    "A cliend ID should be created by enabling the GMail API and creating credentials https://console.developers.google.com/",
                    Toast.LENGTH_LONG).show();
            return;
        }

        authServiceResultLauncher.launch(authService.getAuthRequestIntent());
    }

    private void setAuthStatus() {
        String status = authService.getAuthState().isAuthorized() ?
                getString(R.string.authenticated) : getString(R.string.not_authenticated);
        tvStatus.setText(status);


        // To check the status of the access token:
        // https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=????
    }


    static public void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("authState.json", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            //Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    static public String readFromFile(Context context)
    {
        String ret = "";
        try {
            InputStream inputStream = context.openFileInput("authState.json");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            //Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            //Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

}
