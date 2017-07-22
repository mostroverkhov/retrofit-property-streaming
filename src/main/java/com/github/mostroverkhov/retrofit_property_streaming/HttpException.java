package com.github.mostroverkhov.retrofit_property_streaming;

public class HttpException extends RuntimeException {

  private int code;
  private String statusMessage;

  public HttpException() {
  }

  public HttpException(String message,
      int code,
      String statusMessage) {
    super(message);
    this.code = code;
    this.statusMessage = statusMessage;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String details) {
    this.statusMessage = details;
  }

  @Override
  public String toString() {
    return "HttpException{" +
        "code=" + code +
        ", statusMessage='" + statusMessage + '\'' +
        ", message='" + getMessage() + '\'' +
        '}';
  }
}
