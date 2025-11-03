package com.fileretrieval.common.dto;

public class DocPathFreqPair {
  public String documentPath;
  public long wordFrequency;

  public DocPathFreqPair(String documentPath, long wordFrequency) {
    this.documentPath = documentPath;
    this.wordFrequency = wordFrequency;
  }
}
