package com.example.voicechat;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver {
    static DatagramSocket socket;
    public static void main(String[] args) {
        try {
            // Audio format settings
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(format);
            speakers.start();
            
            // UDP socket setup
            socket = new DatagramSocket(50005);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            System.out.println("Waiting for voice data...");
            
            while (true) {
                // Receive audio data
                socket.receive(packet);
                // Play audio data
                speakers.write(packet.getData(), 0, packet.getLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}

