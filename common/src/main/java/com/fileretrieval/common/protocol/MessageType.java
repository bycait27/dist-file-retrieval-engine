package com.fileretrieval.common.protocol;

public enum MessageType {
  REGISTER_REQUEST("REGISTER REQUEST"),
  INDEX_REQUEST("INDEX REQUEST"),
  SEARCH_REQUEST("SEARCH REQUEST"),
  QUIT("QUIT");

  private final String wireFormat;

  MessageType(String wireFormat) {
    this.wireFormat = wireFormat;
  }

  public String toWireFormat() {
    return wireFormat;
  }

  public static MessageType fromWireFormat(String wire) {
    for (MessageType type : values()) {
      if (type.wireFormat.equals(wire)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown message type: " + wire);
  }
}
