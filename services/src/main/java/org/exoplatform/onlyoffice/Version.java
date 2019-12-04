package org.exoplatform.onlyoffice;

public class Version {

  private String   author              = "";

  private String   createdTime         = "";

  private String   displayName         = "";

  private String   fullName            = "";

  private String   name                = "";

  private String   relaviveCreatedTime = "";

  private String[] versionLabels       = new String[0];

  public String getAuthor() {
    return this.author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getCreatedTime() {
    return this.createdTime;
  }

  public String getRelaviveCreatedTime() {
    return this.relaviveCreatedTime;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getFullName() {
    return this.fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getVersionLabels() {
    return this.versionLabels;
  }

  public void setVersionLabels(String[] versionLabels) {
    this.versionLabels = versionLabels;
  }

  public void setcreatedTime(String createdTime) {
    this.createdTime = createdTime;
  }

  public void setRelaviveCreatedTime(String relaviveCreatedTime) {
    this.relaviveCreatedTime = relaviveCreatedTime;
  }

}
