package com.fileretrieval.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Dispatcher implements Runnable {
    private ServerProcessingEngine engine;
    private String address;
    private Integer port;
    private Integer maxNumConnections;
    private volatile boolean terminate;
    private ServerSocket serverSocket;


    public Dispatcher(ServerProcessingEngine engine, int port, int maxNumConnections) {
        this.engine = engine;
        this.address = "0.0.0.0";
        this.port = port;
        this.maxNumConnections = maxNumConnections;
    }

    public void setTerminate() {
        this.terminate = true;
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket");
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        terminate = false;

        try {
            // create a TCP/IP socket and listen for new connections
            this.serverSocket = new ServerSocket(port, maxNumConnections, InetAddress.getByName(address));

            while (!terminate) {
                // accept connection from client
                Socket clientSocket = serverSocket.accept();

                // when new connection comes through create a new Server Worker thread for the new connection
                // use the engine spawnWorker method to create a new Server Worker thread
                engine.spawnWorker(clientSocket);
            }
        } catch (UnknownHostException e) {
            System.err.println("Could not compute IP address");
            e.printStackTrace();
        } catch (IOException e) {
	        // only print an error and stack trace if not exiting normally
            if (!terminate) { 
	            System.err.println("Socket error");
                e.printStackTrace();
	    }
        }
    }
}