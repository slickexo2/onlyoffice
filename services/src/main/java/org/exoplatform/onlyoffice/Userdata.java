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
  
  /** The forcesaved */
  protected Boolean forcesaved;
  
  /**  The comment. */
  protected String comment;

  /**
   * Instantiates a new userdata.
   *
   * @param userId the user id
   * @param download the download
   * @param coEdited the co edited
   * @param forcesaved the forcesaved
   * @param comment the comment
   */
  public Userdata(String userId, Boolean download, Boolean coEdited, Boolean forcesaved, String comment) {
    super();
    this.userId = userId;
    this.download = download;
    this.coEdited = coEdited;
    this.forcesaved = forcesaved;
    this.comment = comment;
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
   * Gets the forcesaved.
   *
   * @return the forcesaved
   */
  public Boolean isForcesaved() {
    return forcesaved;
  }

  /**
   * Sets the forcesaved.
   *
   * @param forcesaved the forcesaved
   */
  public void setForcesaved(Boolean forcesaved) {
    this.forcesaved = forcesaved;
  }

  /**
   * Gets the comment.
   *
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * Sets the comment.
   *
   * @param comment the new comment
   */
  public void setComment(String comment) {
    this.comment = comment;
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
