package com.fileretrieval.server;

public class FileRetrievalServer
{
    public static void main( String[] args )
    {
        // check for arguments
        if (args.length < 1) {
            System.out.println("Usage: java FileRetrievalServer <port>");
            return;
        }

        int serverPort;

        // assign valid integer to serverPort
        try {
            serverPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port must be a valid integer");
            return;
        }

        // make sure that serverPort is a non-privileged part
        if (serverPort < 1024) {
            System.out.println("Please enter a non-privileged port number (>= 1024)");
            return;
        } 

        IndexStore store = new IndexStore();
        ServerProcessingEngine engine = new ServerProcessingEngine(store);
        ServerAppInterface appInterface = new ServerAppInterface(engine);
        
        // create a thread that creates and server TCP/IP socket and listens to connections
        engine.initialize(serverPort);

        // read commands from the user
        appInterface.readCommands();
    }
}