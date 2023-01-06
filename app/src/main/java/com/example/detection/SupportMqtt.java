package com.example.detection;

import org.eclipse.paho.client.mqttv3.MqttClient;

public class SupportMqtt {

    private static SupportMqtt supportMqtt;

    private MqttClient mqttClient;

    private SupportMqtt(){

    }

    public static SupportMqtt getInstance(){
       if(supportMqtt == null){
           supportMqtt = new SupportMqtt();
       }
       return supportMqtt;
    }
    public MqttClient getMqttClient() {
        return mqttClient;
    }

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }
}
