package com.example.sauna

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import MqttClient
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Build
import android.os.Debug
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.anastr.speedviewlib.SpeedView
import com.github.anastr.speedviewlib.components.indicators.Indicator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var mqttAndroidClient: MqttAndroidClient? = null

    // use tcp://10.0.2.2:1883 if using local mosquitto broker
    val serverUri = "tcp://broker.mqttdashboard.com:1883"
    var clientId = "AndroidClienttesti"
    val subscriptionTopic = "MQTTTESTI"
    var notified : Boolean = false
    // username and password not needed if using local mosquitto broker
    //val mqttUsername = "a-i5u4t3-kg2otp2blv"
    //val mqttPassword = "iJt_MQqHikrls*)Cpc"
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context: Context = applicationContext
        createNotificationChannel()
        //Speedometer
        speedView.withTremble = false
        speedView.unit= " °C"
        speedView.setIndicator(Indicator.Indicators.NeedleIndicator)
        //Speedometer

        // Create the client!
        mqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)
        //val kuva = findViewById<ImageView>(R.id.saunaicon)


        // CALLBACKS, these will take care of the connection if something unexpected happen
        mqttAndroidClient?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    Log.d("MYERROR", "reconnected to MQTT")
                    // we have to subscribe again because of the reconnection
                    subscribeToTopic()
                } else {
                    // Eror log!
                    Log.d("MYERROR", "connected to MQTT")
                }
            }

            override fun connectionLost(cause: Throwable) {
                Log.d("MYERROR", "MQTT connection lost")
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                // THIS VARIABLE IS THE JSON DATA. you can use GSON to get the needed
                // data (temperature for example) out of it, and show it in a textview or something else
                try {
                    val result = String(message.payload)
                    Log.d("MYERROR", result)

                    var drops = result.split(",")

                    //txtTemperature.text = result
                    speedView.speedTo(result.toFloat())
                    if(result.toFloat() >= 70 && !notified)
                    {
                        notified = true
                        sendNotification(result.toFloat())
                    }
                    /*if(result <= "50"){
                        mqttresult.text = "Soundlevel_low"
                    }
                    if(result > "50" && result < "150"){
                        mqttresult.text = "Soundlevel_medium"
                    }
                    if(result >= "150"){
                        mqttresult.text = "Soundlevel_high"
                    }*/


                } catch (e: Exception) {
                    Log.d("MYERROR", e.toString());
                }
            }

            // used when sending data via MQTT
            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })


        // CONNECT TO MQTT

        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false

        try {
            mqttAndroidClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient!!.setBufferOpts(disconnectedBufferOptions)
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d("MYERROR", "Failed to connect!")
                }
            })
        } catch (ex: MqttException) {
            ex.printStackTrace()
        }
    }

    // subscriber method
    fun subscribeToTopic() {
        try {
            mqttAndroidClient!!.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MYERROR", "Subscribed!")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d("MYERROR", "Failed to subscribe")
                }
            })
        } catch (ex: MqttException) {
            System.err.println("Exception whilst subscribing")
            ex.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttAndroidClient!!.close()
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "Notification Title"
            val descText = "Notification Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID,name,importance).apply {
                description = descText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun sendNotification(temp: Float){
        val intent = Intent(this, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this,0,intent,0)

        val bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_launcher_foreground)
        val bitmapLargeIcon = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.smokeicon)



        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.smokeicon)
            .setContentTitle("SAUNA")
            .setContentText("Saunassa lämmintä: " + temp +  " °C astetta!")
            .setLargeIcon(bitmapLargeIcon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            //.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationId, builder.build())
        }
    }

}
