package com.example.voicechat;

import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    static final int AUDIO_RECEPTION_PORT = 50005;
    static final String SERVER_NAME = "JServer001";
    static final int PING_REPLY_PORT = 9001;
    static final int ADVERTISEMENT_PORT = 5001;
    static final int COMM_PORT = 43567;
    static final int AUDIO_CHUNK_SIZE = 1024;

    public static void main( String[] args )
    {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;
        while(!(choice == 2 || choice == 1 || choice == 3)){
            System.out.print("1. Create room\n2. Connect to a server\n3. Quit\nEnter your choise: ");
            choice = scanner.nextInt();
            if(choice != 1 || choice != 2 || choice != 3) System.out.println("Invalid choice\n");
        }
        scanner.close();
        if (choice == 1) {
            System.out.println("Starting server...");
            Server server = new Server(SERVER_NAME,AUDIO_RECEPTION_PORT,COMM_PORT,AUDIO_CHUNK_SIZE);
            server.start(ADVERTISEMENT_PORT,PING_REPLY_PORT);
        }
        else if(choice == 2){
            System.out.println("Available Servers:");
            Receiver receiver = new Receiver(AUDIO_RECEPTION_PORT,COMM_PORT);
            receiver.startserverDiscovery(ADVERTISEMENT_PORT);
        }
        else {
            System.out.println("Quitting");
        }
    }
}
