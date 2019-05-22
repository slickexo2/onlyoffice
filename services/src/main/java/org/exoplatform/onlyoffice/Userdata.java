/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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

  /** The co edited. */
  protected Boolean coEdited;
  
  /**
   * Instantiates a new userdata.
   *
   * @param userId the user id
   * @param key the key
   * @param download the download
   * @param coEdited the co edited
   */
  public Userdata(String userId, String key, Boolean download, Boolean coEdited) {
    super();
    this.userId = userId;
    this.key = key;
    this.download = download;
    this.coEdited = coEdited;
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
   * Gets the coEdited.
   *
   * @return the coEdited
   */
  public Boolean getCoEdited() {
    return coEdited;
  }

  /**
   * Sets the coEdited.
   *
   * @param coEdited the coEdited
   */
  public void setCoEdited(Boolean coEdited) {
    this.coEdited = coEdited;
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
