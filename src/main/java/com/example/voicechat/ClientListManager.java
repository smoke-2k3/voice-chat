package com.example.voicechat;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientListManager {
    private final Map<String, Long> clientLastHeartbeat = new HashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    private static final long HEARTBEAT_INTERVAL = 5000; // Heartbeat interval in milliseconds
    private static final long TIMEOUT_THRESHOLD = 2 * HEARTBEAT_INTERVAL; // Timeout threshold in milliseconds

    public ClientListManager() {
        // Schedule the heartbeat task
        heartbeatScheduler.scheduleAtFixedRate(this::checkClientStatus, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void processClientHeartbeat(String clientAddress) {
        // Update or add server with the current timestamp
        clientLastHeartbeat.put(clientAddress, System.currentTimeMillis());
    }

    private void checkClientStatus() {
        long currentTime = System.currentTimeMillis();

        for (String clientAddress : clientLastHeartbeat.keySet()) {
            long lastHeartbeat = clientLastHeartbeat.get(clientAddress);
            long elapsedTime = currentTime - lastHeartbeat;

            if (elapsedTime > TIMEOUT_THRESHOLD) {
                // Client is inactive, handle accordingly
                handleInactiveClient(clientAddress);
            }
        }
    }

    private void handleInactiveClient(String clientAddress) {
        // Perform actions when a server becomes inactive
        System.out.println("Client " + clientAddress + " is inactive.");
        clientLastHeartbeat.remove(clientAddress);
        try {
            Server.clientList.remove(InetAddress.getByName(clientAddress));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (Server.clientList.isEmpty()) {
            Server.stopAudioTransmission();
        }
    }

    // Example method to stop the heartbeat scheduler when it's no longer needed
    public void stopHeartbeatScheduler() {
        heartbeatScheduler.shutdown();
    }
}



