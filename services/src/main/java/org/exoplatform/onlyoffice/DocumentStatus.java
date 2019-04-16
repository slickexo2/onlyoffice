
/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

// TODO: Auto-generated Javadoc
/**
 * Onlyoffice Config status as described in
 * <a href="http://api.onlyoffice.com/editors/callback">callback handler
 * documentation</a>. Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: DocumentStatus.java 00000 Feb 12, 2016 pnedonosko $
 */
public class DocumentStatus {

  /** The key. */
  protected String   key;

  /** The status. */
  protected Long     status;

  /** The url. */
  protected String   url;

  /** The users. */
  protected String[] users;

  /** The config. */
  protected Config   config;

  /** The error. */
  protected long     error;

  /** The userdata. */
  protected String   userdata;

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
   * @param key the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Gets the status.
   *
   * @return the status
   */
  public Long getStatus() {
    return status;
  }

  /**
   * Sets the status.
   *
   * @param status the status to set
   */
  public void setStatus(Long status) {
    this.status = status;
  }

  /**
   * Gets the url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url.
   *
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets the users.
   *
   * @return the users
   */
  public String[] getUsers() {
    return users;
  }

  /**
   * Sets the users.
   *
   * @param users the users to set
   */
  public void setUsers(String[] users) {
    this.users = users;
  }

  /**
   * Gets the config.
   * 
   * @return the config
   */
  public Config getConfig() {
    return config;
  }

  /**
   * Sets the config.
   * 
   * @param config the config to set
   */
  public void setConfig(Config config) {
    this.config = config;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public long getError() {
    return error;
  }

  /**
   * Sets the error.
   *
   * @param error the error to set
   */
  public void setError(long error) {
    this.error = error;
  }

  /**
   * Gets the last user (editor).
   *
   * @return the last user (editor)
   */
  public String getLastUser() {
    return users.length > 0 ? users[0] : null;
  }
  
  /**
   * Gets the userdata.
   *
   * @return the userdata
   */
  public String getUserdata() {
    return userdata;
  }

  /**
   * Sets the userdata.
   *
   * @param userdata the new userdata
   */
  public void setUserdata(String userdata) {
    this.userdata = userdata;
  }
}
