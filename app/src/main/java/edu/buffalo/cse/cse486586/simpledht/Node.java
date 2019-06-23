package edu.buffalo.cse.cse486586.simpledht;

import java.util.ArrayList;
import java.util.HashMap;

public class Node implements java.io.Serializable {

private String type;
private String nextPort;
private  String prevPort;
private String exactPort;
private String msgKey;
private String msgValue;
private HashMap<String,String> forAll;
private ArrayList<String> forSpecific;

    public ArrayList<String> getForSpecific() {
        return forSpecific;
    }

    public void setForSpecific(ArrayList<String> forSpecific) {
        this.forSpecific = forSpecific;
    }

    public HashMap<String, String> getForAll() {
        return forAll;
    }

    public void setForAll(HashMap<String, String> forAll) {
        this.forAll = forAll;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public void setMsgKey(String msgKey) {
        this.msgKey = msgKey;
    }

    public String getMsgValue() {
        return msgValue;
    }

    public void setMsgValue(String msgValue) {
        this.msgValue = msgValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNextPort() {
        return nextPort;
    }

    public void setNextPort(String nextPort) {
        this.nextPort = nextPort;
    }

    public String getPrevPort() {
        return prevPort;
    }

    public void setPrevPort(String prevPort) {
        this.prevPort = prevPort;
    }

    public String getExactPort() {
        return exactPort;
    }

    public void setExactPort(String exactPort) {
        this.exactPort= exactPort;
    }
}
