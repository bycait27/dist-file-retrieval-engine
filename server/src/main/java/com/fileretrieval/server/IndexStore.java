package com.fileretrieval.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// data structure that stores a document number and the number of times a word/term appears in the document
class DocFreqPair {
    public long documentNumber;
    public long wordFrequency;

    public DocFreqPair(long documentNumber, long wordFrequency) {
        this.documentNumber = documentNumber;
        this.wordFrequency = wordFrequency;
    }
}

public class IndexStore {
    // declare data structures that keep track of DocumentMap and TermInvertedIndex
    HashMap<String, Long> DocumentMap;
    HashMap<String, ArrayList<DocFreqPair>> TermInvertedIndex;
    // declare data structure for easier lookup in getDocument method
    HashMap<Long, String> ReverseDocumentMap;
    // declare and initialize two locks for DocumentMap and TermInvertedIndex
    private final Lock documentMapLock = new ReentrantLock();
    private final Lock termInvertedIndexLock = new ReentrantLock();

    // initialize documentNumber counter to 0
    long documentNumber = 0L;

    public IndexStore() {
        // initialize the DocumentMap and TermInvertedIndex members
        DocumentMap = new HashMap<>();
        TermInvertedIndex = new HashMap<>();
        // initialize DocumentIndexPair
        ReverseDocumentMap = new HashMap<>();
    }

    public long putDocument(String documentPath, long clientID) {
        long uniqueNumber;
        String fullKey = documentPath + "_" + clientID;
        // acquire lock before entering critical section
        documentMapLock.lock();

        // assign a unique number to the document path and return the number
        // make sure that only one thread at a time can access this method
        try {
            uniqueNumber = documentNumber;
            // append client socket ID to the document with delimiter "_"
            DocumentMap.put(fullKey, uniqueNumber);
            ReverseDocumentMap.put(documentNumber, fullKey);
            documentNumber++;
        } finally {
            // release the lock 
            documentMapLock.unlock();
        }
        return uniqueNumber;
    }

    public String getDocument(long documentNumber) {
        return ReverseDocumentMap.get(documentNumber);
    }

    public void updateIndex(long documentNumber, HashMap<String, Long> wordFrequencies) {
        // acquire lock before entering critical section 
        termInvertedIndexLock.lock();

        // update the TermInvertedIndex with the word frequencies of the specified document
        // make sure that only one thread at a time can access this method
        try {
            for (Map.Entry<String, Long> entry : wordFrequencies.entrySet()) {
                if (TermInvertedIndex.containsKey(entry.getKey())) {
                    // add pair to the existing ArrayList
                    DocFreqPair docFreqPair = new DocFreqPair(documentNumber, entry.getValue());
                    ArrayList<DocFreqPair> arrayList = TermInvertedIndex.get(entry.getKey());
                    arrayList.add(docFreqPair);
                } else {
                    // create a new ArrayList, add the pair to the list, and update index
                    DocFreqPair docFreqPair = new DocFreqPair(documentNumber, entry.getValue());
                    ArrayList<DocFreqPair> arrayList = new ArrayList<>();
                    arrayList.add(docFreqPair);
                    TermInvertedIndex.put(entry.getKey(), arrayList); 
                }
            }
        } finally {
            // release the lock 
            termInvertedIndexLock.unlock();
        }
    }

    public ArrayList<DocFreqPair> lookupIndex(String term) {
        ArrayList<DocFreqPair> results = new ArrayList<>();
        // return the document and frequency pairs for the specified term
        if (TermInvertedIndex.containsKey(term)) {
            results = TermInvertedIndex.get(term);
        }
        return results;
    }
}