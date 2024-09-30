package com.example.voicechat;

import javax.sound.sampled.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Server {
    private static int commPort;
    private int port;
    private String serverName;
    private boolean adRunning = false;
    private boolean pingServiceRunning = false;
    static HashMap<InetAddress, Boolean> clientList = new HashMap<>();
    private ClientListManager clm = null;
    private static boolean transmissionRunning = false;
    private int audioChunkSize;

    Server(String serverName, int port,int commPort,int audioChunkSize){
        this.port = port;
        Server.commPort = commPort;
        this.serverName = serverName;
        this.audioChunkSize = audioChunkSize;
        clm = new ClientListManager();
    }

    public void start(int adPort,int pingReplyPort){
        startAd(adPort);
        pingReplyService(pingReplyPort);
        startTransmission();
    }

    public static void sendPeerListToClent() throws IOException{
        for (InetAddress key : clientList.keySet()) {
            sendMapToDevice(key, commPort);
        }
    }

    private static void sendMapToDevice(InetAddress address, int port) throws IOException {
        Socket socket = new Socket(address, port);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(clientList);  // Send the serialized HashMap
        oos.flush();           // Ensure everything is sent
        oos.close();           // Close the stream
        socket.close();        // Close the socket
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
                    System.out.println("Broadcast started");
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
        Thread prsThread = new Thread(new Runnable() {
            @Override
            public void run(){
                DatagramSocket pingSocket = null;
                try {
                    pingSocket = new DatagramSocket(pingReplyPort);
                    byte[] buffer = new byte[64];
                    byte[] responseData = "Y".getBytes();
                    DatagramPacket pingRecvPacket = new DatagramPacket(buffer, buffer.length);
                    DatagramPacket responsePacket = new DatagramPacket(responseData,responseData.length);
                    pingServiceRunning = true;
                    InetAddress responseAddr = null;
                    System.out.println("Ping reply Service started");
                    while(pingServiceRunning){
                        pingSocket.receive(pingRecvPacket);
                        responseAddr = responsePacket.getAddress();
                        if(!clientList.containsKey(responseAddr)){
                            clientList.put(responseAddr, true);
                            clm.processClientHeartbeat(responseAddr.toString());
                            if(!transmissionRunning) startTransmission();
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
        });
        prsThread.start();
    }

    public void stopPRS(){pingServiceRunning = false;}

    public void stop(){
        stopAd();
        stopPRS();
        if(clm != null) clm.stopHeartbeatScheduler();
        stopAudioTransmission();
    }

    public static void stopAudioTransmission(){
        transmissionRunning = false;
    }

    public void startTransmission() {
        Thread transmissionThread = new Thread(new Runnable() {
            @Override
            public void run(){
                DatagramSocket socket = null;
                TargetDataLine microphone = null;
                if(clientList.isEmpty()){
                    System.out.println("No Client connected");
                    return;
                }
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
                    
                    byte[] buffer = new byte[audioChunkSize];
                    System.out.println("Starting voice capture...");
                    int bytesRead = 0;
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, receiverAddress, port);
                    transmissionRunning = true;
                    System.out.println("Started Transmission");
                    while (transmissionRunning) {
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
        });
        transmissionThread.start();
    }
}
