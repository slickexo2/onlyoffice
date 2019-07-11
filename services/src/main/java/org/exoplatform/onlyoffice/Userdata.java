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

  /** The download. */
  protected Boolean download;

  /** The co edited. */
  protected Boolean coEdited;

  /**
   * Instantiates a new userdata.
   *
   * @param userId the user id
   * @param download the download
   * @param coEdited the co edited
   */
  public Userdata(String userId, Boolean download, Boolean coEdited) {
    super();
    this.userId = userId;
    this.download = download;
    this.coEdited = coEdited;
  }

  /**
   * Instantiates a new userdata.
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
   * Gets the download.
   *
   * @return the download
   */
  public Boolean isDownload() {
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
   * Gets the co edited.
   *
   * @return the co edited
   */
  public Boolean getCoEdited() {
    return coEdited;
  }

  /**
   * Sets the co edited.
   *
   * @param coEdited the new co edited
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
