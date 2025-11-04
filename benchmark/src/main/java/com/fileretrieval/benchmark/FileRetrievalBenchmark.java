package com.fileretrieval.benchmark;

import java.util.ArrayList;
import com.fileretrieval.client.ClientProcessingEngine;
import com.fileretrieval.common.dto.DocPathFreqPair;
import com.fileretrieval.common.dto.IndexResult;
import com.fileretrieval.common.dto.SearchResult;

class BenchmarkWorker implements Runnable {
    private ClientProcessingEngine engine;
    private String serverIP;
    private String serverPort;
    private String datasetPath;
    private long totalReadBytes;

    public BenchmarkWorker(String serverIP, String serverPort, String datasetPath) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.datasetPath = datasetPath;
    }

    public Long getTotalBytesRead() {
        return totalReadBytes;
    }

    @Override
    public void run() {
        engine = new ClientProcessingEngine();
        // connect client
        engine.connect(serverIP, serverPort);

        // index the dataset
        IndexResult indexResult = engine.indexFolder(datasetPath);
        // get total bytes read
        totalReadBytes = indexResult.totalBytesRead;
    }

    public void search(ArrayList<String> terms) {
        // perform search operations on the ClientProcessingEngine
        System.out.println("Searching for " + String.join(" ", terms));
        SearchResult searchResult = engine.search(terms);

        // print the results and performance
        System.out.println("Search completed in " + String.format("%.3f", searchResult.executionTime) + " seconds");
        System.out.println("Search results (top 10):");
        for (DocPathFreqPair pair : searchResult.documentFrequencies) {
				// split by last underscore to separate path from clientID
                int lastUnderscore = pair.documentPath.lastIndexOf('_');
                String pathPart = pair.documentPath.substring(0, lastUnderscore);
                String clientID = pair.documentPath.substring(lastUnderscore + 1);

                // find where "folder" starts in the path
                // this extracts "folderX/..." portion
                String[] pathSegments = pathPart.split("/");
                int folderIndex = -1;

                for (int i = 0; i < pathSegments.length; i++) {
                    if (pathSegments[i].startsWith("folder")) {
                        folderIndex = i;
                        break;
                    }
                }

                // build relative path from folder onwards
                String relativePath;

                if (folderIndex != -1) {
                    relativePath = String.join("/",
                    java.util.Arrays.copyOfRange(pathSegments, folderIndex, pathSegments.length));
                } else {
                    // fallback: show last 3 segments
                    int start = Math.max(0, pathSegments.length - 3);
                    relativePath = String.join("/",
                        java.util.Arrays.copyOfRange(pathSegments, start, pathSegments.length));
                }
                System.out.println("* client " + clientID + ":" + relativePath + ":" + pair.wordFrequency);
            }
    }

    public void disconnect() {
        // disconnect the ClientProcessingEngine from the server
        engine.disconnect();
    }
}

public class FileRetrievalBenchmark {
    public static void main(String[] args)
    {
        String serverIP;
        String serverPort;
        int numberOfClients;
        ArrayList<String> clientsDatasetPath = new ArrayList<>();

        // store terms in a terms list for search queries
        ArrayList<String> queries = new ArrayList<>();
        // add queries to test with
        queries.add("the");
        queries.add("child-like");
        queries.add("vortex");
        queries.add("moon AND vortex");
        queries.add("distortion AND adaptation");

        // check for correct number of arguments
        if (args.length < 3) {
            System.out.println("Usage: java FileRetrievalBenchmark <serverIP> <serverPort> <numOfClients> <datasetPath1> [datasetPath2] ...");
            return;
        }

        // extract the arguments from args
        serverIP = args[0];
        serverPort = args[1];
        numberOfClients = Integer.parseInt(args[2]);

        // check that there are enough dataset paths
        if (args.length < 3 + numberOfClients) {
            System.out.println("Error: Expected " + numberOfClients + " dataset paths, but only " + (args.length - 3) + " provided");
            return;
        }

        // loop through paths by number of clients and add them to the ArrayList 
        for (int i = 0; i < numberOfClients; i++) {
            clientsDatasetPath.add(args[3 + i]);
        }

        // measure the execution start time
        long startTime = System.nanoTime();

        // create and start benchmark worker threads equal to the number of clients
        ArrayList<BenchmarkWorker> workers = new ArrayList<>();
        for (int i = 0; i < numberOfClients; i++) {
            BenchmarkWorker worker = new BenchmarkWorker(serverIP, serverPort, clientsDatasetPath.get(i));
            workers.add(worker);
        }

        ArrayList<Thread> threads = new ArrayList<>();
        for (BenchmarkWorker worker : workers) {
            Thread thread = new Thread(worker);
            threads.add(thread);
            thread.start();
        }

        // join the benchmark worker threads
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // measure the execution stop time and print the performance
        long stopTime = System.nanoTime();
        long calcTime = stopTime - startTime;
        // convert calcTime to double and seconds
        double execTime = (double) calcTime / 1_000_000_000.0;

        long totalReadBytes = 0L;
        for (BenchmarkWorker worker : workers) {
            totalReadBytes += worker.getTotalBytesRead();
        }

        // print the execution time and the total number of bytes read
        System.out.println("Completed indexing " + totalReadBytes + " bytes of data");
        System.out.println("Completed indexing in " + String.format("%.3f", execTime) + " seconds");

        // run search queries on the first client (benchmark worker thread number 1)
        for (String query : queries) {
            String[] parts = query.split(" ");
            ArrayList<String> terms = new ArrayList<>();
            for (String part : parts) {
                if (!part.equals("AND") && !part.isEmpty()) {
                    terms.add(part);
                }
            }
            workers.get(0).search(terms);
        }

        // disconnect all clients (all benchmark worker threads)
        for (BenchmarkWorker worker : workers) {
            worker.disconnect();
        }
    }
}