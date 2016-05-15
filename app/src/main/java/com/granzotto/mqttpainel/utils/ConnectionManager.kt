package com.granzotto.mqttpainel.utils

import android.content.Context
import android.provider.Settings
import com.pawegio.kandroid.d
import com.pawegio.kandroid.e
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*

object ConnectionManager {

    private var serverUrl: String? = ""
    private var serverPort: String? = ""
    private var userName: String? = ""
    private var userPassword: String? = ""

    var client: MqttAndroidClient? = null
    var connectionListener: IMqttActionListener? = null
    var listeners: HashMap<String, MessageReceivedListener> = HashMap()

    fun setUp(serverUrl: String?, serverPort: String?, userName: String?, userPassword: String?) {
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
        this.userName = userName;
        this.userPassword = userPassword;
    }

    fun connect(appContext: Context) {
        var url: String;
        serverUrl = if (!serverUrl.isNullOrBlank() && serverUrl!!.contains("//")) serverUrl!! else
            "tcp://" + serverUrl

        url = "${serverUrl}:${serverPort}"

        client = MqttAndroidClient(appContext, url, Settings.Secure.ANDROID_ID);
        client?.registerResources(appContext)
        val options = MqttConnectOptions();
        options.userName = userName
        options.password = userPassword?.toCharArray()
        client?.connect(options, null, connectionListener)

        client?.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                listeners.forEach {
                    it.value.messageReceived(topic, message)
                }
            }

            override fun connectionLost(cause: Throwable?) {
                e("Connection lost!")
                cause?.printStackTrace()
                connect(appContext)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                d("deliveryComplete: \n${token}")
            }

        })
    }

    fun addRecievedListener(listener: MessageReceivedListener, tag: String) {
        listeners.remove(tag)
        listeners.put(tag, listener)
    }

    fun addRecievedListenerAndSubscribe(listener: MessageReceivedListener, tag: String, topic: String, qos: Int) {
        addRecievedListener(listener, tag)
        client?.subscribe(topic, qos)
    }

    fun removeRecievedListener(tag: String) {
        listeners.remove(tag)
    }

}

interface MessageReceivedListener {
    fun messageReceived(topic: String?, message: MqttMessage?)
}
