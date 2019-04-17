package org.exoplatform.onlyoffice;

import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class Userdata.
 */
public class Userdata {
  
  /** The user id. */
  protected String  userId;

  /** The key. */
  protected String  key;

  /** The download. */
  protected Boolean download;

  /**
   * Instantiates a new userdata.
   *
   * @param userId the user id
   * @param key the key
   * @param download the download
   */
  public Userdata(String userId, String key, Boolean download) {
    super();
    this.userId = userId;
    this.key = key;
    this.download = download;
  }
  
  /**
   * Instantiates a new userdata.
   * 
   */
  public Userdata() {
    
  }

  /**
   * Gets the user id.
   *
   * @return the user id
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the user id.
   *
   * @param userId the new user id
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Gets the key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the key.
   *
   * @param key the new key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Gets the download.
   *
   * @return the download
   */
  public Boolean getDownload() {
    return download;
  }

  /**
   * Sets the download.
   *
   * @param download the new download
   */
  public void setDownload(Boolean download) {
    this.download = download;
  }
  
  /**
   * Return this config as JSON string.
   *
   * @return the string
   * @throws JsonException the json exception
   */
  public String toJSON() throws JsonException {
    JsonGeneratorImpl gen = new JsonGeneratorImpl();
    return gen.createJsonObject(this).toString();
  }

}
