package org.exoplatform.onlyoffice.cometd;

/** The CometdInfo class is used to pass necessary cometd information to a client */
public class CometdInfo {
  
  /** The user */
  private String user;
  
  /** The userToken */
  private String userToken;
  
  /** The cometdPath */
  private String cometdPath;
  
  /** The container */
  private String container;
  
  /** The docId */
  private String docId;
  
  /**
   * Instantiates a CometdInfo object
   * 
   * @param user the user
   * @param userToken the userToken
   * @param cometdPath the cometdPath
   * @param container the container
   * @param docId the docId
   */
  public CometdInfo(String user, String userToken, String cometdPath, String container, String docId) {
    super();
    this.user = user;
    this.userToken = userToken;
    this.cometdPath = cometdPath;
    this.container = container;
    this.docId = docId;
  }

  /**
   * Gets the user
   * 
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * Sets the user
   * 
   * @param user the user
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Gets the userToken
   * 
   * @return the userToken
   */
  public String getUserToken() {
    return userToken;
  }

  /**
   * Sets the userToken
   * 
   * @param userToken the userToken
   */
  public void setUserToken(String userToken) {
    this.userToken = userToken;
  }

  /**
   * Gets the cometdPath
   * 
   * @return the cometdPath
   */
  public String getCometdPath() {
    return cometdPath;
  }

  /**
   * Sets the cometdPath
   * 
   * @param cometdPath the cometdPath
   */
  public void setCometdPath(String cometdPath) {
    this.cometdPath = cometdPath;
  }

  /**
   * Gets the container
   * 
   * @return the container
   */
  public String getContainer() {
    return container;
  }

  /**
   * Sets the container
   * 
   * @param container the container
   */
  public void setContainer(String container) {
    this.container = container;
  }

  /**
   * Gets the docId
   * 
   * @return the docId
   */
  public String getDocId() {
    return docId;
  }

  /**
   * Sets the docId
   * 
   * @param docId the docId
   */
  public void setDocId(String docId) {
    this.docId = docId;
  }
  
}
