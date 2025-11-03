package com.fileretrieval.common.dto;

public class IndexResult {
  public double executionTime;
  public long totalBytesRead;

  public IndexResult(double executionTime, long totalBytesRead) {
    this.executionTime = executionTime;
    this.totalBytesRead = totalBytesRead;
  }
}
