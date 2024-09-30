package com.example.voicechat;

import javax.sound.sampled.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Receiver {
    
    private int commPort;
    private int audioRecvPort;
    private int audioChunkSize;
    private boolean pingServiceRunning = false;
    private boolean audioReceiving = false;
    private boolean serverDiscoveryRunning = false;
    private boolean transmissionRunning = false;
    private boolean commRunning = false;
    private HashMap<InetAddress,Boolean> activeServers = new HashMap<>();
    private HashMap<InetAddress, Boolean> peerList = new HashMap<>();

    public Receiver(int audioRecvPort, int commPort,int audioChunkSize){
        this.audioRecvPort = audioRecvPort;
        this.commPort = commPort;
        this.audioChunkSize = audioChunkSize;
    }

    public void startserverDiscovery(int broadcastPort){
        Thread discovery = new Thread(new Runnable() {
            @Override
            public void run(){
                DatagramSocket disSocket = null;
                try {
                    disSocket = new DatagramSocket(broadcastPort);
                    disSocket.setBroadcast(true);
                    byte[] buffer = new byte[128];
                    DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
                    serverDiscoveryRunning = true;
                    InetAddress recvAddr = null;
                    while (serverDiscoveryRunning) {
                        disSocket.receive(recvPacket);
                        recvAddr = recvPacket.getAddress();
                        if (!activeServers.containsKey(recvAddr)) {
                            activeServers.put(recvAddr, true);
                            //process heartbeat for inactives
                        }
                        showActiveServers();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (disSocket != null && !disSocket.isClosed()) {
                        disSocket.close();
                    }
                }
            }
        });
        discovery.start();
    }

    public void showActiveServers(){
        clearConsole();
        int i = 0;
        for (InetAddress key : activeServers.keySet()) {
            System.out.println(i+')'+key.toString());
            i++;
        }
    }

    public void startpingService(InetAddress serverAddr, int serverPingPort){
        Thread pinger = new Thread(new Runnable() {
            @Override
            public void run(){
                DatagramSocket pingSock = null;
                try {
                    pingSock = new DatagramSocket();
                    DatagramPacket pingPack = new DatagramPacket("P".getBytes(), "P".getBytes().length, serverAddr, serverPingPort);
                    pingServiceRunning = true;
                    while (pingServiceRunning) {
                        pingSock.send(pingPack);
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (pingSock != null && !pingSock.isClosed()) {
                        pingSock.close();
                    }
                }
            }
        });
        pinger.start();
    }

    public void stopPinger(){pingServiceRunning = false;}

    
    private void clearConsole() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;
            if (os.contains("win")) {
                // Windows command
                processBuilder = new ProcessBuilder("cmd", "/c", "cls");
            } else {
                // Unix/Linux/Mac command
                processBuilder = new ProcessBuilder("clear");
            }

            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receivePeerList(){
        Thread peerRecv = new Thread(new Runnable() {
            @Override
            public void run(){
                ServerSocket peerData = null;
                ObjectInputStream ois = null;
                Socket socket = null;
                try {
                    peerData = new ServerSocket(commPort);
                    while (commRunning) {
                        socket = peerData.accept();
                        ois = new ObjectInputStream(socket.getInputStream());
                        peerList = (HashMap<InetAddress, Boolean>) ois.readObject();
                        ois.close();
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(peerData != null && !peerData.isClosed()){
                        try {
                            peerData.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (socket != null && !socket.isClosed()){
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(ois != null){
                        try {
                            ois.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        peerRecv.start();
    }

    public void receiveAudio() {
        Thread reception = new Thread(new Runnable() {
            @Override
            public void run(){
                DatagramSocket socket = null;
                SourceDataLine speakers = null;
                try {
                    // Audio format settings
                    AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    speakers = (SourceDataLine) AudioSystem.getLine(info);
                    speakers.open(format);
                    speakers.start();
                    
                    // UDP socket setup
                    socket = new DatagramSocket(audioRecvPort);
                    socket.setSoTimeout(5000);
                    byte[] buffer = new byte[audioChunkSize];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    System.out.println("Waiting for voice data...");
                    audioReceiving = true;
                    while (audioReceiving) {
                        socket.receive(packet);
                        speakers.write(packet.getData(), 0, packet.getLength());
                    }
                } catch (Exception e) {
                    System.out.println("Server Disconnected");
                    e.printStackTrace();
                } finally {
                    if (socket != null && !socket.isClosed()) socket.close();
                    if (speakers != null && speakers.isOpen()) speakers.close();
                }
            }
        });
        reception.start();
    }

    public void startTransmission() {
        Thread transmissionThread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                    InetAddress receiverAddress = InetAddress.getByName("localhost");

                    byte[] buffer = new byte[1024];
                    System.out.println("Starting voice capture...");
                    int bytesRead = 0;
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, receiverAddress, audioRecvPort);
                    transmissionRunning = true;
                    System.out.println("Started Transmission");
                    while (transmissionRunning) {
                        bytesRead = microphone.read(buffer, 0, buffer.length);
                        // Send audio data over UDP
                        packet.setData(buffer, 0, bytesRead);
                        for (InetAddress key : peerList.keySet()) {
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
                    if (microphone != null && microphone.isOpen()) {
                        microphone.close();
                    }
                }
            }
        });
        transmissionThread.start();
    }
}

