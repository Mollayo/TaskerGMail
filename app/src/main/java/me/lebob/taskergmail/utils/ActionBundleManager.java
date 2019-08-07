package me.lebob.taskergmail.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.regex.Pattern;

import me.lebob.taskergmail.R;


public class ActionBundleManager {

    // only accept valid MAC addresses of form 00:11:22:AA:BB:CC, where colons can be dashes
    public static boolean isEMailAddress(String emailAddress) {
        if (emailAddress == null) {
            return false;
        }

        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        if (emailAddress.matches(regex))
            return true;

        // We allow variable for email address
        return TaskerPlugin.variableNameValid(emailAddress);
    }

    // Whether the bundle is valid. Strings must be non-null, and either variables
    // or valid format (correctly-formatted MAC, non-empty, proper hex if binary, etc.)
    public static boolean isBundleValid(final Bundle bundle) {
        if (bundle == null) {
            Log.w(Constants.LOG_TAG, "Null bundle");
            return false;
        }

        String[] keys = {Constants.BUNDLE_STRING_OAUTH, Constants.BUNDLE_STRING_RECIPIENT,
                Constants.BUNDLE_STRING_SUBJECT, Constants.BUNDLE_STRING_BODY};
        for (String key: keys) {
            if (!bundle.containsKey(key)) {
                Log.w(Constants.LOG_TAG, "Bundle missing key " + key);
            }
        }

        String recipient = getRecipient(bundle);
        if (!isEMailAddress(recipient)) {
            Log.w(Constants.LOG_TAG, "Invalid EMail address");
            return false;
        }

        // Check for the case of sending a message
        String subject = getSubject(bundle);
        if (subject == null) {
            Log.w(Constants.LOG_TAG, "Null subject");
            return false;
        }

        return true;
    }

    // method to get error message for the given values, or null if no error exists
    public static String getErrorMessage(Context context, final String oAuth, final String recipient,
                                         String subject, String body) {
        Resources res = context.getResources();
        if (!isEMailAddress(recipient)) {
            return res.getString(R.string.invalid_email);
        }

        if (subject == null || subject.isEmpty()) {
            return res.getString(R.string.invalid_subject);
        }
        return null;
    }

    // Method to create bundle from the individual values
    public static Bundle generateBundle(final String oAuth, final String recipient, String subject, String body) {
        if (oAuth == null || recipient == null || subject == null) {
            return null;
        }
        if (body==null)
            body="";

        final Bundle bundle = new Bundle();
        bundle.putString(Constants.BUNDLE_STRING_OAUTH, oAuth);
        bundle.putString(Constants.BUNDLE_STRING_RECIPIENT, recipient);
        bundle.putString(Constants.BUNDLE_STRING_SUBJECT, subject);
        bundle.putString(Constants.BUNDLE_STRING_BODY, body);

        if (!isBundleValid(bundle)) {
            return null;
        } else {
            return bundle;
        }
    }

    // Method for getting short String description of bundle
    public static String getBundleBlurb(final Bundle bundle) {
        if (!isBundleValid(bundle)) {
            return null;
        }

        final String oAuth = getOAuth(bundle);
        final String recipient = getRecipient(bundle);
        final String subject = getSubject(bundle);
        final String body = getBody(bundle);

        final int max_len = 480;
        final String ellipses = "...";

        StringBuilder builder = new StringBuilder();
        builder.append("Recipient: ");
        builder.append(recipient);
        builder.append("\n\nSubject: ");
        builder.append(subject);
        builder.append("\n\nBody: ");
        builder.append(body);

        int length = builder.length();

        if (length > max_len) {
            builder.delete(max_len - ellipses.length(), length);
            builder.append(ellipses);
        }

        return builder.toString();
    }

    // Method to get MAC address of bundle
    public static String getOAuth(final Bundle bundle) {
        return bundle.getString(Constants.BUNDLE_STRING_OAUTH, null);
    }

    // Method to get MAC address of bundle
    public static String getRecipient(final Bundle bundle) {
        return bundle.getString(Constants.BUNDLE_STRING_RECIPIENT, null);
    }

    // Method to get message part of bundle
    public static String getSubject(final Bundle bundle) {
        return bundle.getString(Constants.BUNDLE_STRING_SUBJECT, null);
    }

    // Method to get CRLF part of bundle
    public static String getBody(final Bundle bundle) {
        return bundle.getString(Constants.BUNDLE_STRING_BODY, null);
    }
}
