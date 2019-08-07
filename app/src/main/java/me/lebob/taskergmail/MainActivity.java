package me.lebob.taskergmail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final int RC_AUTH = 100;
    AppAuthService authService;
    TextView tvStatus;
    String authStateJSon="";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.v(Constants.LOG_TAG, "MainActivity::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = AppAuthService.getInstance(this);

        findViewById(R.id.authorize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.handleLogin();
            }
        });
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


    // Method that gets called once request to enable GMail access
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, R.string.login_cancelled, Toast.LENGTH_SHORT).show();
        }
        else
        {
            authService.getAuthToken(data, new AppAuthService.TokenResponseCallback() {
                @Override
                public void onComplete() {
                    MainActivity.this.setAuthStatus();

                    // Save the authorization to a file
                    AuthState authState=authService.getAuthState();
                    authStateJSon=authState.jsonSerializeString();
                    writeToFile(authStateJSon,getApplicationContext());
                }
            });
        }
    }

    private void handleLogin()
    {
        if (authService.clientId.length()==0) {
            Toast.makeText(this,
                    "A cliend ID should be created by enabling the GMail API and creating credentials https://console.developers.google.com/",
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(authService.getAuthRequestIntent(), RC_AUTH);
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
                String receiveString = "";
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
