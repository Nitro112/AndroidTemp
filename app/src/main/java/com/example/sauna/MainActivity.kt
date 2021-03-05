package com.example.sauna

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import MqttClient
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var mqttAndroidClient: MqttAndroidClient? = null

    // use tcp://10.0.2.2:1883 if using local mosquitto broker
    val serverUri = "tcp://broker.mqttdashboard.com:1883"
    var clientId = "AndroidClienttesti"
    val subscriptionTopic = "MQTTTESTI"
    // username and password not needed if using local mosquitto broker
    //val mqttUsername = "a-i5u4t3-kg2otp2blv"
    //val mqttPassword = "iJt_MQqHikrls*)Cpc"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context: Context = applicationContext
        // Create the client!
        mqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)
        val kuva = findViewById<ImageView>(R.id.saunaicon)


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

                    txtTemperature.text = result
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

}
