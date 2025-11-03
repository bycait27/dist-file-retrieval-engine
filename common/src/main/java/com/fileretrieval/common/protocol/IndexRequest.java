package com.fileretrieval.common.protocol;

import java.util.Map;

public class IndexRequest {
    public MessageType type = MessageType.INDEX_REQUEST;
    public long clientID;
    public String documentPath;
    public Map<String, Long> wordFrequencies;

    public IndexRequest() {}
}