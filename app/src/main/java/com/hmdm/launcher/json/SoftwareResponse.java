package com.hmdm.launcher.json;

public class SoftwareResponse {
    private String stbType;
    private String softwareDownloadFtpUrl;
    private String fileCrc;
    private boolean isCurrent;
    private String fileName;
    private String fileSize;
    private String softwareVersion;

    public String getStbType() { return stbType; }
    public void setStbType(String value) { this.stbType = value; }

    public String getSoftwareDownloadFtpUrl() { return softwareDownloadFtpUrl; }
    public void setSoftwareDownloadFtpUrl(String value) { this.softwareDownloadFtpUrl = value; }

    public String getFileCrc() { return fileCrc; }
    public void setFileCrc(String value) { this.fileCrc = value; }

    public boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(boolean value) { this.isCurrent = value; }

    public String getFileName() { return fileName; }
    public void setFileName(String value) { this.fileName = value; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String value) { this.fileSize = value; }

    public String getSoftwareVersion() { return softwareVersion; }
    public void setSoftwareVersion(String value) { this.softwareVersion = value; }
}
