package org.exoplatform.onlyoffice;

import java.util.*;

public class Version {


  private String              author_                = "";

  private Long            createdTime_;

  private String              displayName            = "";

  private String              name_                  = "";

  private String[]            versionLabels_         = new String[0];




  public String getAuthor() {
    return this.author_;
  }

  public Long getCreatedTime() {
    return this.createdTime_;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName_) {
    this.displayName = displayName_;
  }

  public void setName_(String name_) {
    this.name_ = name_;
  }

  public void setAuthor_(String author_) {
    this.author_ = author_;
  }

  public void setcreatedTime_(Long createdTime_) {
    this.createdTime_ = createdTime_;
  }

  public void setVersionLabels_(String[] versionLabels_) {
    this.versionLabels_ = versionLabels_;
  }

  public String getName() {
    return this.name_;
  }


  public String[] getVersionLabels() {
    return this.versionLabels_;
  }


}
