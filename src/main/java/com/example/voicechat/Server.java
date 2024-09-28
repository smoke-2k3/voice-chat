package com.example.voicechat;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Server {
    private int port;
    private String serverName;
    private boolean adRunning = false;
    private boolean pingServiceRunning = false;
    static HashMap<InetAddress, Boolean> clientList = new HashMap<>();
    private ClientListManager clm = null;
    private boolean serverRunning = false;

    Server(String serverName, int port){
        this.port = port;
        this.serverName = serverName;
        clm = new ClientListManager();
    }

    public void startAd(final int adPort){
        Thread advertisement = new Thread(new Runnable() {
            @Override
            public void run(){
                byte[] buf = serverName.getBytes();
                DatagramSocket adSocket = null;
                DatagramPacket adPacket = null;
                try {
                    adPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName("255.255.255.255"),adPort);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } finally {
                    if (adSocket != null && !adSocket.isClosed()) {
                        adSocket.close();
                    }
                }
                try {
                    adSocket = new DatagramSocket(adPort);
                    adSocket.setBroadcast(true);
                    adRunning = true;
                    while (adRunning) {
                        adSocket.send(adPacket);
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (adSocket != null && !adSocket.isClosed()) {
                        adSocket.close();
                    }
                }
            }
        });
        advertisement.start();
    }

    public void stopAd(){adRunning = false;}

    public void pingReplyService(int pingReplyPort){
        DatagramSocket pingSocket = null;
        try {
            pingSocket = new DatagramSocket(pingReplyPort);
            byte[] buffer = new byte[64];
            byte[] responseData = "Y".getBytes();
            DatagramPacket pingRecvPacket = new DatagramPacket(buffer, buffer.length);
            DatagramPacket responsePacket = new DatagramPacket(responseData,responseData.length);
            pingServiceRunning = true;
            InetAddress responseAddr = null;
            while(pingServiceRunning){
                pingSocket.receive(pingRecvPacket);
                responseAddr = responsePacket.getAddress();
                if(!clientList.containsKey(responseAddr)){
                    clientList.put(responseAddr, true);
                    clm.processClientHeartbeat(responseAddr.toString());
                }
                responsePacket.setAddress(responsePacket.getAddress());
                responsePacket.setPort(responsePacket.getPort());
                pingSocket.send(responsePacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pingSocket != null && !pingSocket.isClosed()) {
                pingSocket.close();
            }
        }
    }

    public void stopPRS(){pingServiceRunning = false;}

    public void stop(){
        stopAd();
        stopPRS();
        if(clm != null) clm.stopHeartbeatScheduler();
    }

    public void stopAudioTransmission(){
        serverRunning = false;
    }

    public static void pause(){
        
    }

    public void start() {
        DatagramSocket socket = null;
        TargetDataLine microphone = null;
        try {
            // Audio format settings
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            // UDP socket setup
            socket = new DatagramSocket();
            InetAddress receiverAddress = InetAddress.getByName("localhost"); // replace with IP of receiver if necessary
            
            byte[] buffer = new byte[1024];
            System.out.println("Starting voice capture...");
            int bytesRead = 0;
            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, receiverAddress, port);
            serverRunning = true;
            while (serverRunning) {
                bytesRead = microphone.read(buffer, 0, buffer.length);
                // Send audio data over UDP
                packet.setData(buffer, 0, bytesRead);
                for (InetAddress key : clientList.keySet()) {
                    packet.setAddress(key);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if(microphone != null && microphone.isOpen()){
                microphone.close();
            }
        }
    }
}
