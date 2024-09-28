package com.example.voicechat;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender {
    public static void main(String[] args) {
        try {
            // Audio format settings
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            
            // UDP socket setup
            DatagramSocket socket = new DatagramSocket();
            InetAddress receiverAddress = InetAddress.getByName("localhost"); // replace with IP of receiver if necessary
            
            byte[] buffer = new byte[1024];
            System.out.println("Starting voice capture...");
            
            while (true) {
                // Capture audio
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                // Send audio data over UDP
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, receiverAddress, 50005);
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
