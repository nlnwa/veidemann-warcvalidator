package no.nb.nna.veidemann.warcvalidator.settings;

public class Settings {

    private String warcDir;

    private String validWarcDir;

    private String invalidWarcDir;

    private String jhoveConfigPath;

    private int sleepTime;

    private boolean deleteReportIfValid;

    private boolean skipMove;

    public String getWarcDir() {
        return warcDir;
    }

    public void setWarcDir(String warcDir) {
        this.warcDir = warcDir;
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

    public boolean isDeleteReportIfValid() {
        return deleteReportIfValid;
    }

    public void setDeleteReportIfValid(boolean deleteReportIfValid) {
        this.deleteReportIfValid = deleteReportIfValid;
    }

    public boolean isSkipMove() {
        return skipMove;
    }

    public void setSkipMove(boolean skipMove) {
        this.skipMove = skipMove;
    }
}
