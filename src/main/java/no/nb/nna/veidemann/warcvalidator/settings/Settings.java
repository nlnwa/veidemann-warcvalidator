package no.nb.nna.veidemann.warcvalidator.settings;

public class Settings {


    private String dbHost;

    private int dbPort;

    private String dbName;

    private String dbUser;

    private String dbPassword;

    private String warcDir;

    private String deliveryWarcDir;

    private String validWarcDir;

    private String invalidWarcDir;

    private String jhoveConfigPath;

    private int sleepTime;

    public String getDbHost() { return dbHost; }

    public void setDbHost(String dbHost) { this.dbHost = dbHost; }

    public int getDbPort() { return dbPort; }

    public void setDbPort(int dbPort) { this.dbPort = dbPort; }

    public String getDbName() { return dbName; }

    public void setDbName(String dbName) { this.dbName = dbName; }

    public String getDbUser() { return dbUser; }

    public void setDbUser(String dbUser) { this.dbUser = dbUser; }

    public String getDbPassword() { return dbPassword; }

    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getWarcDir() { return warcDir; }

    public void setWarcDir(String warcDir) { this.warcDir = warcDir; }

    public String getDeliveryWarcDir() { return deliveryWarcDir; }

    public void setDeliveryWarcDir(String deliveryWarcDir) { this.deliveryWarcDir = deliveryWarcDir; }

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

    public String getInvalidWarcDir() { return invalidWarcDir; }

    public void setInvalidWarcDir(String invalidWarcDir) { this.invalidWarcDir = invalidWarcDir; }

    public String getValidWarcDir() { return validWarcDir; }

    public void setValidWarcDir(String validWarcDir) { this.validWarcDir = validWarcDir; }
}
