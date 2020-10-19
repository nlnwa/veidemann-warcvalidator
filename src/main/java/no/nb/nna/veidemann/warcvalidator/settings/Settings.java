package no.nb.nna.veidemann.warcvalidator.settings;

public class Settings {

    private String warcDir;

    private String deliveryWarcDir;

    private String validWarcDir;

    private String invalidWarcDir;

    private String jhoveConfigPath;

    private String deliveryPermissions;

    private String deliveryGroupId;

    private int sleepTime;

    public String getWarcDir() {
        return warcDir;
    }

    public void setWarcDir(String warcDir) {
        this.warcDir = warcDir;
    }

    public String getDeliveryWarcDir() {
        return deliveryWarcDir;
    }

    public void setDeliveryWarcDir(String deliveryWarcDir) {
        this.deliveryWarcDir = deliveryWarcDir;
    }

    public String getJhoveConfigPath() {
        return jhoveConfigPath;
    }

    public void setJhoveConfigPath(String jhoveConfigPath) {
        this.jhoveConfigPath = jhoveConfigPath;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public String getInvalidWarcDir() {
        return invalidWarcDir;
    }

    public void setInvalidWarcDir(String invalidWarcDir) {
        this.invalidWarcDir = invalidWarcDir;
    }

    public String getValidWarcDir() {
        return validWarcDir;
    }

    public void setValidWarcDir(String validWarcDir) {
        this.validWarcDir = validWarcDir;
    }

    public String getDeliveryPermissions() {
        return this.deliveryPermissions;
    }

    public void setDeliveryPermissions(String permission) {
        this.deliveryPermissions = permission;
    }


    public String getDeliveryGroupId() {
        return deliveryGroupId;
    }

    public void setDeliveryGroupId(String deliveryGroupId) {
        this.deliveryGroupId = deliveryGroupId;
    }
}
