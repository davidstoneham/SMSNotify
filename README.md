# SMSNotify
An Android app to automatically send text messages from any API endpoint. The app pols the server(s) every minute for new messages. If there are any messages it will send them one by one and hit the completed API endpoint with the message id once it has been sent successfully.

*Please note this app has been thrown together and is not fully tested. It is designed to work running on a phone that is plugged in 24/7. Android 6+ has some power management features that may cause this app to stop running in the background if the device is not plugged in. The app also has no UI level logging or error messages and should be used as more of a starting point rather than a complete solution*

*Android has an anti-spam feature to protect your phone from sending too many messages too quickly so nasty apps can't rack up excessive carrier charges. You will need to disable this feature if you intend to send many messages quickly. A guide is available at https://www.xda-developers.com/change-sms-limit-android/ on how to do this.*

*Always check your carrier limits and fact sheets so your are not violating your terms of service*

## API
You can configure your own urls for this app to communicate to but you must have the 2 following endpoints at each url. 

### Get Pending Messages

`http://yoururl.com/anyOtherSuffix/SMSPending?key={optional}` 

The key is optional to check on your server to add some level of security that only you can retrieve the pending sms. This endpoint should return a JSON array of any pending SMS to send. The JSON should be in the following format
```
{
    id: integer,
    mobile: string,
    msg: string
}
```

* id: A unique id for the sms which will be sent back to the server once the message has been sent.
* mobile: The mobile number to send the message to, this doesn't need the country code if sent locally.
* msg: The sms message to be sent.

### Message Sent

`http://yoururl.com/anyOtherSuffix/SMSSent?id={msgId}&key={optional}` 

This endpoint doesn't need to return anything and can be used for your server to mark the SMS as sent so you're not sending duplicates. Each time an SMS is sent via the app this URL will be called with the id of the sms that was sent and the optional API Key.

## Using The App
1. Download and install the latest release, or compile it yourself.
2. Enter the endpoints you want the app to check. You must fully qualify these endpoings with http:// or https://. You can enter as many as you like the app will check all the endpoints each time is runs
3. enter an optional API key to be used in the requests to your server. If you leave this blank no API key will be sent

## Going Further
Feel free to download and edit the app as required if you need something more specific or just want to make some changes!
