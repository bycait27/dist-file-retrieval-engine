package com.fileretrieval.client;

import java.util.ArrayList;
import java.util.Scanner;

import com.fileretrieval.common.dto.DocPathFreqPair;
import com.fileretrieval.common.dto.IndexResult;
import com.fileretrieval.common.dto.SearchResult;

public class ClientAppInterface {
    private ClientProcessingEngine engine;

    public ClientAppInterface(ClientProcessingEngine engine) {
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
                engine.disconnect();
                break;
            }

            // if the command begins with connect, connect to the given server
            if (command.startsWith("connect")) {
                // parse command for given IP address and port number
                String[] parts = command.split(" ");
                // make sure there are enough arguments
                if (parts.length < 3) {
                    System.out.println("Usage: connect <IP> <port>");
                    continue;
                }
                String IP = parts[1];
                String port = parts[2];

                // call connect on the processing engine
                engine.connect(IP, port);

                continue;
            }

            // if the command begins with get_info, print the client ID
            if (command.startsWith("get_info")) {
                // parse command and call getInfo on the processing engine
                long ID = engine.getInfo();
                
                // print the client ID
                System.out.println("client ID: " + ID);

                continue;
            }
            
            // if the command begins with index, index the files from the specified directory
            if (command.startsWith("index")) {
                // parse command and call indexFolder on the processing engine
                // start after "index "
                String folderPath = command.substring(6);

                IndexResult result = engine.indexFolder(folderPath);

                // print the execution time and the total number of bytes read
                System.out.println("Completed indexing " + result.totalBytesRead + " bytes of data");
                System.out.println("Completed indexing in " + String.format("%.3f", result.executionTime) + " seconds");
                
                continue;
            }

            // if the command begins with search, search for files that matches the query
            if (command.startsWith("search")) {
                // parse command and call search on the processing engine
                // start after " search "
				String[] parts = command.substring(7).split(" ");
				ArrayList<String> terms = new ArrayList<>();
				    
				// only add words that don't include "AND" to terms ArrayList
				for (String part : parts) {
					if (!part.equals("AND") && !part.isEmpty()) {
						terms.add(part);
					}
				}	
		
				SearchResult result = engine.search(terms);

                // print the execution time and the top 10 search results
                System.out.println("Search completed in " + String.format("%.3f", result.executionTime) + " seconds");
				System.out.println("Search results (top 10):");
				for (DocPathFreqPair pair : result.documentFrequencies) {
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
                continue;
            }
            System.out.println("unrecognized command!");
        }
        sc.close();
    }
}