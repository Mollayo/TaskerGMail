# TaskerGMail
A plugin for Tasker (https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm) to send mails using GMail. Unlike existing plugins (https://play.google.com/store/apps/details?id=com.balda.mailtask&hl=en) and (https://play.google.com/store/apps/details?id=com.joaomgcd.autogmail&hl=en), this plugin can use Google accounts that are not added to the Android device.

This plugin can use Tasker variables for the email address of the recipient and the subject and body of the mail to send.

Limitations:
- Only one GMail account can be used at a time.
- Not possible to send files in attachment.
- Can only send mails.

In order to use this plugin, you need to create a client ID. This should be done by enabling the GMail API and creating the credentials using your account with https://console.developers.google.com/. The method "initializeSettings()" in the file "AppAuthService.java" should then be modified with this new client ID.
