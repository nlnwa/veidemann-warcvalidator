package no.nb.nna.veidemann.warcvalidator.model;

import java.util.ArrayList;
import java.util.HashMap;

public class WarcError {

    private String filename;
    private String status;
    private ArrayList<HashMap<String, String>> messages;
    private ArrayList<String> nonCompliantWarcIds;

    public WarcError(String filename, String status, ArrayList<HashMap<String, String>> messages, ArrayList<String> nonCompliantWarcIds) {
        this.filename = filename;
        this.status = status;
        this.messages = messages;
        this.nonCompliantWarcIds = nonCompliantWarcIds;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ArrayList<HashMap<String, String>> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<HashMap<String, String>> messages) {
        this.messages = messages;
    }

    public ArrayList<String> getNonCompliantWarcId() {
        return nonCompliantWarcIds;
    }

    public void setNonCompliantWarcId(ArrayList<String> nonCompliantWarcId) {
        this.nonCompliantWarcIds = nonCompliantWarcId;
    }

}
