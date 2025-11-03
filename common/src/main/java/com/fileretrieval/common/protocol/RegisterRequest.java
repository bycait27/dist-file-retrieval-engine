package com.fileretrieval.common.protocol;

public class RegisterRequest {
    public MessageType type = MessageType.REGISTER_REQUEST;
    public long clientID;

    public RegisterRequest() {}
}