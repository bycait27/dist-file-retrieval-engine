package com.fileretrieval.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fileretrieval.common.dto.DocPathFreqPair;
import com.fileretrieval.common.protocol.MessageType;

public class ServerWorker implements Runnable {
    private IndexStore store;
    private ServerProcessingEngine engine;
    private Socket clientSocket;

    public ServerWorker(IndexStore store, ServerProcessingEngine engine, Socket clientSocket) {
        this.store = store;
        this.engine = engine;
        this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
        long currentWorkerID = 0L; // track the client ID

        // receive a message from the client
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String clientMessage;

            while((clientMessage = in.readLine()) != null) {
                MessageType message = null;
                // assign correct message type to client message
                try {
                   message = MessageType.fromWireFormat(clientMessage); 
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown message type");
                    continue;
                }

                // handle different message types
                switch (message) {
        //       if the message is a REGISTER REQUEST, then
        //       generate a new client ID and return a REGISTER REPLY message containing the client ID
                    case REGISTER_REQUEST:
                        UUID uuid = UUID.randomUUID();
                        long clientID = uuid.getMostSignificantBits() & Long.MAX_VALUE;
                        engine.addConnectedClient(clientID, clientSocket);
                        currentWorkerID = clientID;
                        out.println(clientID);
                        break;
        //       if the message is an INDEX REQUEST, then
        //       extract the document path, client ID and word frequencies from the message(s)
        //       get the document number associated with the document path (call putDocument)
        //       update the index store with the word frequencies and the document number
        //       return an acknowledgement INDEX REPLY message
                    case INDEX_REQUEST:
                        long clientIdFromClient = Long.parseLong(in.readLine());
                        String documentPath = in.readLine();
                        int pairsSize = Integer.parseInt(in.readLine());

                        HashMap<String, Long> wordFrequencies = new HashMap<>();

                        // iterate over lines by number of wordFrequency pairs
                        for (int i = 0; i < pairsSize; i++) {
                            String line = in.readLine();
                            String[] entry = line.split("=");
                            String key = entry[0];
                            long value = Long.parseLong(entry[1]);
                            wordFrequencies.put(key, value);
                        }

                        long documentNumber = store.putDocument(documentPath, clientIdFromClient);

                        store.updateIndex(documentNumber, wordFrequencies);

                        out.println("IndexStore updated successfully!");
                        break;
        //       if the message is a SEARCH REQUEST, then
        //       extract the terms from the message
        //       for each term get the pairs of documents and frequencies from the index store
        //       combine the returned documents and frequencies from all of the specified terms
        //       sort the document and frequency pairs and keep only the top 10
        //       for each document number get from the index store the document path
        //       return a SEARCH REPLY message containing the top 10 results
                    case SEARCH_REQUEST:
                        int termsSize = Integer.parseInt(in.readLine());
                        ArrayList<ArrayList<DocFreqPair>> docFreqPairList = new ArrayList<>();
                        ArrayList<DocPathFreqPair> results = new ArrayList<>();

                        for (int i = 0; i < termsSize; i++) {
                            String term = in.readLine();
                            docFreqPairList.add(store.lookupIndex(term));
                        }

                        // account for different terms cases
                        if (docFreqPairList.isEmpty()) {
                            // do nothing
                        } else if (docFreqPairList.size() == 1) {
                            for (DocFreqPair pair : docFreqPairList.get(0)) {
                                // get document path from IndexStore 
                                String docPath = store.getDocument(pair.documentNumber);
                                // create new pair with document path
                                DocPathFreqPair newPair = new DocPathFreqPair(docPath, pair.wordFrequency);
                                
                                // add the new pair to the result
                                results.add(newPair);
                            }
                        } else {
                            // extract the first term list of documents and frequency pairs
                            ArrayList<DocFreqPair> termsList = docFreqPairList.get(0);
                            // create new HashMap for the shared documents and their combined frequencies
                            HashMap<Long, Long> shared = new HashMap<>();
                    
                            // for each pair in the extracted list, check if the document appears in the other lists
                            for (DocFreqPair pair : termsList) {
                                // keep track of frequency total and make sure document is in all lists
                                Long frequencyAcc = pair.wordFrequency;
                                boolean inAllTermsLists = true;
                                for (int i = 1; i < docFreqPairList.size(); i++) {
                                    // other terms lists
                                    ArrayList<DocFreqPair> otherTermsList = docFreqPairList.get(i);
                                    // assume document is not in the list
                                    boolean inThisTermList = false;
                                    // check pairs in the other terms lists
                                    for (DocFreqPair otherPair : otherTermsList) {
                                        // check if the documents match
                                        if (otherPair.documentNumber == pair.documentNumber) {
                                            frequencyAcc += otherPair.wordFrequency;
                                            inThisTermList = true;
                                            break;
                                        } 
                                    }
                                    // document was not in all terms lists
                                    if (!inThisTermList) {
                                        inAllTermsLists = false;
                                        break;
                                    }
                                }
                                // check if all lists have the document
                                if (inAllTermsLists) {
                                    // add the documentNumber and total frequency to the shared HashMap
                                    shared.put(pair.documentNumber, frequencyAcc);
                                }
                            }
                            // create new DocPathFreqPairs for the final result
                            for (Map.Entry<Long, Long> entry : shared.entrySet()) {
                                String docPath = store.getDocument(entry.getKey());
                                Long wordFrequency = entry.getValue();
                                DocPathFreqPair newPair = new DocPathFreqPair(docPath, wordFrequency); 
                                results.add(newPair);
                            }
                        }

                        // sort the document and frequency pairs and keep only the top 10
                        results.sort((pair1, pair2) -> Long.compare(pair2.wordFrequency, pair1.wordFrequency)); 	

                        if (results.size() > 10) {
                            results = new ArrayList<>(results.subList(0, 10));
                        }

                        out.println(results.size());

                        for (DocPathFreqPair pair : results) {
                            out.println(pair.documentPath + "=" + pair.wordFrequency);
                        }
                        break;
        //       if the message is a QUIT message, then finish running
                    case QUIT:
                        return;
                }
            }
        } catch (IOException e) {
            System.err.println("Error receiving message from client!");
            e.printStackTrace();
        } finally {
            if (currentWorkerID != 0L) {
                engine.removeConnectedClient(currentWorkerID);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket!");
                e.printStackTrace();
            }
            System.out.println("Client disconnected.");
        }
    }
}