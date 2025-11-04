package com.fileretrieval.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fileretrieval.common.dto.DocPathFreqPair;
import com.fileretrieval.common.dto.IndexResult;
import com.fileretrieval.common.dto.SearchResult;

public class ClientProcessingEngine {
    // keep track of the connection (socket)
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private long clientId;

    public ClientProcessingEngine() { }

    public IndexResult indexFolder(String folderPath) {
        // make sure we are connected to server first
        if (socket == null || socket.isClosed()) {
            System.err.println("Not connected to server! Use 'connect <IP> <port>' first.");
            return new IndexResult(0.0, 0);
        }

        IndexResult result = new IndexResult(0.0, 0);
        // initialize total read bytes to 0
        long totalReadBytes = 0L;

        // get the start time
        long startTime = System.nanoTime();

        // crawl the folder path and extract all file paths
		// convert String folderPath to a Path
		Path folder = Paths.get(folderPath);

		try (Stream<Path> paths = Files.walk(folder)) {
			List<Path> filePaths = paths
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
	
			for (Path filePath : filePaths) {	
				// convert the type Path to type File for Scanner
				File file = filePath.toFile();
	
				// increment the total number of read bytes
				totalReadBytes += Files.size(filePath);
	
				// create HashMap for each file
				HashMap<String, Long> wordFrequencies = new HashMap<>();
	
				// for each file extract words/terms and count their frequencies
				// scan through each line
				try (Scanner fileScanner = new Scanner(file)) {
					while (fileScanner.hasNextLine()) {
						String line = fileScanner.nextLine();
						// split based on assignment definition of word
						String[] lineWords = line.split("[^a-zA-Z0-9_-]+");
	
						// add the words to the HashMap with their frequencies 
						for (String word : lineWords) {
							if (word.length() > 3) {
								// get current count + 1 or if new, default to 0 + 1
								wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0L) + 1);
							}
						}	
					}
				} catch (FileNotFoundException e) {
					System.err.println("File not found: " + e.getMessage());
				}

                // for each file prepare an INDEX REQUEST message and send to the server
                //       the document path, the client ID and the word frequencies
                out.println("INDEX REQUEST");
                out.println(clientId);
                out.println(filePath.toString());
                out.println(wordFrequencies.size());
                
                for (Map.Entry<String, Long> entry : wordFrequencies.entrySet()) {
                    out.println(entry.getKey() + "=" + entry.getValue());
                }

                // receive for each INDEX REQUEST message an INDEX REPLY message
                if (socket != null && !socket.isClosed()) {
                    try {
                        String response = in.readLine(); 
                        // don't print reply
                    } catch (IOException e) {
                        System.err.println("Error reading response from server!");
                        e.printStackTrace();
                    }
                }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

        // get the stop time and calculate the execution time
        long stopTime = System.nanoTime();
        long calcTime = stopTime - startTime;
        // convert calcTime to double and seconds
        double execTime = (double) calcTime / 1_000_000_000.0;

        // return the execution time and the total number of bytes read
        result.executionTime = execTime;
        result.totalBytesRead = totalReadBytes;

        return result;
    }
    
    public SearchResult search(ArrayList<String> terms) {
        // make sure we are connected to server first
        if (socket == null || socket.isClosed()) {
            System.err.println("Not connected to server! Use 'connect <IP> <port>' first.");
            return new SearchResult(0.0, new ArrayList<DocPathFreqPair>());
        }

        SearchResult result = new SearchResult(0.0, new ArrayList<DocPathFreqPair>());
        // get the start time
        long startTime = System.nanoTime();

        // prepare a SEARCH REQUEST message that includes the search terms and send it to the server
        out.println("SEARCH REQUEST");
        out.println(terms.size());
        
        for (String term : terms) {
            out.println(term);
        }

        // receive one or more SEARCH REPLY messages with the results of the search query
        if (socket != null && !socket.isClosed()) {
            try {
                int size = Integer.parseInt(in.readLine());

                for (int i = 0; i < size; i++) {
                    String line = in.readLine();
                    String[] parts = line.split("=");
                    String documentPath = parts[0];
                    long wordFrequency = Long.parseLong(parts[1]);
                    // add top 10 documents and frequencies to result
                    result.documentFrequencies.add(new DocPathFreqPair(documentPath, wordFrequency));
                }

            } catch (IOException e) {
                System.err.println("Error reading response from server!");
                e.printStackTrace();
            }
        }

        // get the stop time and calculate the execution time
        long stopTime = System.nanoTime();
        long calcTime = stopTime - startTime;
        // convert calcTime to double and seconds
        double execTime = (double) calcTime / 1_000_000_000.0;

        // return the execution time and the top 10 documents and frequencies
        result.executionTime = execTime;

        return result;
    }

    public long getInfo() {
        // return the client ID
        return clientId;
    }

    public void connect(String serverIP, String serverPort) {
        try {
            // create a new TCP/IP socket and connect to the server
            socket = new Socket(serverIP, Integer.parseInt(serverPort));

            // for handing I/O streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // send a REGISTER REQUEST message and receive a REGISTER REPLY message with the client ID
            out.println("REGISTER REQUEST");

            String response = in.readLine();
            clientId = Long.parseLong(response);
            System.out.println("Connection successful!");

        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + serverIP);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + serverIP);
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                // send a QUIT message to the server
                out.println("QUIT");

                // close streams
                if (in != null) in.close();
                if (out != null) out.close();

                // close the TCP/IP socket
                socket.close();
            } catch (IOException e) {
                System.err.println("Could not disconnect from server!");
                e.printStackTrace();
            }
        }
    }
}