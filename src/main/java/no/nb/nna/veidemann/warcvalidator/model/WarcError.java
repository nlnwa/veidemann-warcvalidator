package no.nb.nna.veidemann.warcvalidator.model;

import java.util.HashMap;

public class WarcError {

    private String filename;
    private String status;
    private HashMap messages;
    private HashMap nonCompliantWarcId;

    public WarcError(String filename, String status, HashMap messages, HashMap nonCompliantWarcId) {
        this.filename = filename;
        this.status = status;
        this.messages = messages;
        this.nonCompliantWarcId = nonCompliantWarcId;
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

    public HashMap getMessages() {
        return messages;
    }

    public void setMessages(HashMap messages) {
        this.messages = messages;
    }

    public HashMap getNonCompliantWarcId() {
        return nonCompliantWarcId;
    }

    public void setNonCompliantWarcId(HashMap nonCompliantWarcId) {
        this.nonCompliantWarcId = nonCompliantWarcId;
    }

}
