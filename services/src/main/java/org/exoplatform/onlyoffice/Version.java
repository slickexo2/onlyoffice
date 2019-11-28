package org.exoplatform.onlyoffice;

public class Version {


  private String              author              = "";

  private String            createdTime;

  private String              displayName            = "";

  private String              name                 = "";

  private String              fullName                 = "";

  private String[]            versionLabels         = new String[0];




  public String getAuthor() {
    return this.author;
  }

  public String getCreatedTime() {
    return this.createdTime;
  }

  public String getFullName() {
    return this.fullName;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setFullName(String fullName) { this.fullName = fullName; }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setcreatedTime(String createdTime) {
    this.createdTime = createdTime;
  }

  public void setVersionLabels(String[] versionLabels) {
    this.versionLabels = versionLabels;
  }

  public String getName() {
    return this.name;
  }


  public String[] getVersionLabels() {
    return this.versionLabels;
  }


}
