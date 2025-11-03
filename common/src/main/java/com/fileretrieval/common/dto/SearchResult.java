package com.fileretrieval.common.dto;

import java.util.ArrayList;

public class SearchResult {
  public double executionTime;
  public ArrayList<DocPathFreqPair> documentFrequencies;

  public SearchResult(double executionTime, ArrayList<DocPathFreqPair> documentFrequencies) {
    this.executionTime = executionTime;
    this.documentFrequencies = documentFrequencies;
  }
}
