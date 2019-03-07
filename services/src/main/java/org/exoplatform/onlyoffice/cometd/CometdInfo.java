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
package org.exoplatform.onlyoffice.cometd;

/**
 *  The CometdInfo class is used to pass necessary cometd information to a client.
 */
public class CometdInfo {
  
  /**  The user. */
  private String user;
  
  /**  The userToken. */
  private String userToken;
  
  /**  The cometdPath. */
  private String cometdPath;
  
  /**  The container. */
  private String container;
  
  /**  The docId. */
  private String docId;
  
  /**
   * Instantiates a CometdInfo object.
   *
   * @param user the user
   * @param userToken the userToken
   * @param cometdPath the cometdPath
   * @param container the container
   * @param docId the docId
   */
  public CometdInfo(String user, String userToken, String cometdPath, String container, String docId) {
    this.user = user;
    this.userToken = userToken;
    this.cometdPath = cometdPath;
    this.container = container;
    this.docId = docId;
  }
  
  /**
   * Instantiates a CometdInfo object.
   */
  public CometdInfo() {
    
  }

  /**
   * Gets the user.
   *
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * Sets the user.
   *
   * @param user the user
   */
  public void setUser(String user) {
    this.user = user;
  }

  /**
   * Gets the userToken.
   *
   * @return the userToken
   */
  public String getUserToken() {
    return userToken;
  }

  /**
   * Sets the userToken.
   *
   * @param userToken the userToken
   */
  public void setUserToken(String userToken) {
    this.userToken = userToken;
  }

  /**
   * Gets the cometdPath.
   *
   * @return the cometdPath
   */
  public String getCometdPath() {
    return cometdPath;
  }

  /**
   * Sets the cometdPath.
   *
   * @param cometdPath the cometdPath
   */
  public void setCometdPath(String cometdPath) {
    this.cometdPath = cometdPath;
  }

  /**
   * Gets the container.
   *
   * @return the container
   */
  public String getContainer() {
    return container;
  }

  /**
   * Sets the container.
   *
   * @param container the container
   */
  public void setContainer(String container) {
    this.container = container;
  }

  /**
   * Gets the docId.
   *
   * @return the docId
   */
  public String getDocId() {
    return docId;
  }

  /**
   * Sets the docId.
   *
   * @param docId the docId
   */
  public void setDocId(String docId) {
    this.docId = docId;
  }
  
}
