package com.fileretrieval.common.protocol;

import java.util.List;

public class SearchRequest {
    public MessageType type = MessageType.SEARCH_REQUEST;
    public List<String> terms;

    public SearchRequest() {}
}