package com.example.voicechat;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class Receiver {
    
    private boolean pingServiceRunning = false;
    private boolean audioReceiving = false;
    private boolean serverDiscoveryRunning = false;
    private HashMap<InetAddress,Boolean> activeServers = new HashMap<>();

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
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                Runtime.getRuntime().exec("cls");
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
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
                    socket = new DatagramSocket(50005);
                    socket.setSoTimeout(5000);
                    byte[] buffer = new byte[1024];
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
}

