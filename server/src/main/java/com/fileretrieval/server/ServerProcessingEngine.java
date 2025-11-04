package com.fileretrieval.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerProcessingEngine {
    private IndexStore store;
    private Dispatcher dispatcher;
    private ArrayList<Thread> threads;
    private HashMap<Long, Socket> clientSockets;
    private int maxNumConnections;

    // declare locks for clientSockets and threads
    private final Lock clientSocketLock = new ReentrantLock();
    private final Lock threadLock = new ReentrantLock();

    public ServerProcessingEngine(IndexStore store) {
        this.store = store;
        threads = new ArrayList<Thread>();
        clientSockets = new HashMap<Long, Socket>();
        maxNumConnections = 50; // can't have more than 50 clients
    }

    public void addConnectedClient(long clientID, Socket clientSocket) {
        // acquire lock before entering critical section
        clientSocketLock.lock();

        // make sure only one thread at a time can add a client to clientSockets
        try {
            // add client socket to the list
            clientSockets.put(clientID, clientSocket);
        } finally {
            // release the lock
            clientSocketLock.unlock();
        }
    }

    public void removeConnectedClient(long clientID) {
        // acquire lock before entering critical section
        clientSocketLock.lock();

        // make sure only one thread at a time can remove a client from clientSockets
        try {
            // remove client socket from the list
            clientSockets.remove(clientID);
        } finally {
            // release the lock
            clientSocketLock.unlock();
        }
    }

    public void initialize(int serverPort) {
        // acquire lock before entering critical section 
        threadLock.lock();

        try {
            // create and start the Dispatcher thread
            dispatcher = new Dispatcher(this, serverPort, maxNumConnections);
            Thread dispatcherThread = new Thread(dispatcher);
            threads.add(dispatcherThread);
            dispatcherThread.start();
        } finally {
            // release the lock
            threadLock.unlock();
        }
    }

    public void spawnWorker(Socket clientSocket) {
        // acquire lock before entering critical section 
        threadLock.lock();

        try {
            // check if max connections will be exceeded
            if (threads.size() >= maxNumConnections) {
                System.out.println("Cannot exceed " + maxNumConnections + " client connections");
                try {
                    clientSocket.close(); // close rejected socket
                } catch (IOException e) {
                    System.err.println("error closing rejected socket");
                }
                return; // early return, don't spawn worker
            }

            // create and start new Server Worker thread
            ServerWorker serverWorker = new ServerWorker(store, this, clientSocket);
            Thread serverWorkerThread = new Thread(serverWorker);
            threads.add(serverWorkerThread);
            serverWorkerThread.start();
        } finally {
            // release the lock
            threadLock.unlock();
        }
    }

    public void shutdown() {
        ArrayList<Thread> threadsToJoin;

        // copy list while holding lock 
        threadLock.lock();

        try {
            // signal the Dispatcher thread to shutdown
            dispatcher.setTerminate();
            // close the server socket to unblock accept()
            dispatcher.closeServerSocket();
            // use copy of threads
            threadsToJoin = new ArrayList<>(threads);
        } finally {
            // release the lock 
            threadLock.unlock();
        }
        // join Dispatcher and Server Worker threads outside lock (faster)
        for (Thread t : threadsToJoin) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.err.println("Could not join thread");
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> getConnectedClients() {
        ArrayList<String> list = new ArrayList<>();

        // acquire lock before entering critical section
        clientSocketLock.lock();

        // make sure that only one thread can access and modify list of client sockets
        try {
            // extract information from list of client sockets
            for (Map.Entry<Long, Socket> entry : clientSockets.entrySet()) {
                Long clientID = entry.getKey();
                Socket clientSocket = entry.getValue();

                // obtain IP and port
                String IP = clientSocket.getInetAddress().getHostAddress();
                int port = clientSocket.getPort();

                // build the formatted string (for ServerAppInterface)
                String clientInfo = "client " + clientID + ":" + IP + ":" + port;
                list.add(clientInfo);
            }
        } finally {
            // release the lock
            clientSocketLock.unlock();
        }
        // return the connected clients information 
        return list;
    }
}