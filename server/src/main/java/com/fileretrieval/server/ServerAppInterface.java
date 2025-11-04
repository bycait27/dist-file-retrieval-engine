package com.fileretrieval.server;

import java.lang.System;
import java.util.Scanner;
import java.util.ArrayList;

public class ServerAppInterface {
    private ServerProcessingEngine engine;

    public ServerAppInterface(ServerProcessingEngine engine) {
        this.engine = engine;
    }

    public void readCommands() {
        Scanner sc = new Scanner(System.in);
        String command;
        
        while (true) {
            System.out.print("> ");
            
            // read from command line
            command = sc.nextLine();

            // if the command is quit, terminate the program       
            if (command.equals("quit")) {
                engine.shutdown();
                break;
            }

            // if the command begins with list, list all the connected clients
            if (command.startsWith("list")) {
                // call the getConnectedClients method from the server to retrieve the clients information
                ArrayList<String> results = engine.getConnectedClients();

                // print the clients information
                for (String result : results) {
                    System.out.println(result);
                }
                continue;
            }
            System.out.println("unrecognized command!");
        }
        sc.close();
    }
}