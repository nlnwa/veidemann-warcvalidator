package no.nb.nna.veidemann.warcvalidator.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class WarcError {

    private String filename;
    private String status;
    private ArrayList<HashMap<String, String>> messages;
    private ArrayList<String> nonCompliantWarcIds;
    private OffsetDateTime timestamp;

    public WarcError(String filename, String status, ArrayList<HashMap<String, String>> messages,
                     ArrayList<String> nonCompliantWarcIds, OffsetDateTime timestamp) {
        this.filename = filename;
        this.status = status;
        this.messages = messages;
        this.nonCompliantWarcIds = nonCompliantWarcIds;
        this.timestamp = timestamp;
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

    public OffsetDateTime getTimestamp() { return timestamp; }

    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
