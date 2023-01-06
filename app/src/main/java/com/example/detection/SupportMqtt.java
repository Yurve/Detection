package com.example.detection;

import org.eclipse.paho.client.mqttv3.MqttClient;

public class SupportMqtt {

    public static SupportMqtt supportMqtt;

    private MqttClient mqttClient;

    public SupportMqtt(){
        supportMqtt = SupportMqtt.this;
    }
    public MqttClient getMqttClient() {
        return mqttClient;
    }

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }
}
