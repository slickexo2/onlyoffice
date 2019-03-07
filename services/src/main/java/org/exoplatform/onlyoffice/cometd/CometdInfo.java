package org.exoplatform.onlyoffice.cometd;

public class CometdInfo {
  
  private String user;
  
  private String userToken;
  
  private String cometdPath;
  
  private String container;
  
  private String docId;
  
  
  public CometdInfo(String user, String userToken, String cometdPath, String container, String docId) {
    super();
    this.user = user;
    this.userToken = userToken;
    this.cometdPath = cometdPath;
    this.container = container;
    this.docId = docId;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getUserToken() {
    return userToken;
  }

  public void setUserToken(String userToken) {
    this.userToken = userToken;
  }

  public String getCometdPath() {
    return cometdPath;
  }

  public void setCometdPath(String cometdPath) {
    this.cometdPath = cometdPath;
  }

  public String getContainer() {
    return container;
  }

  public void setContainer(String container) {
    this.container = container;
  }

  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }
  
  
  

}
