
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

import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

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

  /** The coedited. */
  protected Boolean  coEdited;

  /** The saved */
  protected Boolean  saved;

  /** The userId (used for saving the document content under this user) */
  protected String   userId;

  /**
   * Gets the config.
   * 
   * @return the config
   */
  public Config getConfig() {
    return config;
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
   * Gets the last user (editor).
   *
   * @return the last user (editor)
   */
  public String getLastUser() {
    return users.length > 0 ? users[0] : null;
  }

  /**
   * Sets the config.
   *
   * @param config the new config
   */
  protected void setConfig(Config config) {
    this.config = config;
  }

  /**
   * Gets the key.
   *
   * @return the key
   */
  protected String getKey() {
    return key;
  }

  /**
   * Gets the status.
   *
   * @return the status
   */
  protected Long getStatus() {
    return status;
  }

  /**
   * Gets the url.
   *
   * @return the url
   */
  protected String getUrl() {
    return url;
  }

  /**
   * Gets the users.
   *
   * @return the users
   */
  protected String[] getUsers() {
    return users;
  }

  /**
   * Gets the coEdited.
   *
   * @return the coEdited
   */
  protected Boolean isCoedited() {
    return coEdited;
  }

  /**
   * Gets the isSaved.
   *
   * @return the isSaved
   */
  protected Boolean isSaved() {
    return saved;
  }

  /**
   * Gets the userId.
   *
   * @return the userId
   */
  protected String getUserId() {
    return userId;
  }

  /**
   * The Class Builder.
   */
  public static class Builder {

    /** The document status. */
    private DocumentStatus documentStatus;

    /**
     * Instantiates a new builder.
     */
    public Builder() {
      documentStatus = new DocumentStatus();
    }

    /**
     * Key.
     *
     * @param key the key
     * @return the builder
     */
    public Builder key(String key) {
      documentStatus.key = key;
      return this;
    }

    /**
     * Status.
     *
     * @param status the status
     * @return the builder
     */
    public Builder status(Long status) {
      documentStatus.status = status;
      return this;
    }

    /**
     * Url.
     *
     * @param url the url
     * @return the builder
     */
    public Builder url(String url) {
      documentStatus.url = url;
      return this;
    }

    /**
     * Users.
     *
     * @param users the users
     * @return the builder
     */
    public Builder users(String[] users) {
      documentStatus.users = users;
      return this;
    }

    /**
     * Config.
     *
     * @param config the config
     * @return the builder
     */
    public Builder config(Config config) {
      documentStatus.config = config;
      return this;
    }

    /**
     * Error.
     *
     * @param error the error
     * @return the builder
     */
    public Builder error(long error) {
      documentStatus.error = error;
      return this;
    }

    /**
     * Co edited.
     *
     * @param coEdited the co edited
     * @return the builder
     */
    public Builder coEdited(Boolean coEdited) {
      documentStatus.coEdited = coEdited;
      return this;
    }

    public Builder userdata(Userdata userdata) {
      if (userdata != null) {
        documentStatus.userId = userdata.userId;
        documentStatus.coEdited = userdata.coEdited;
        documentStatus.saved = userdata.isDownload();
      }
      return this;
    }

    /**
     * Builds the.
     *
     * @return the document status
     */
    public DocumentStatus build() {
      return documentStatus;
    }

    /**
     * Reset.
     */
    public void reset() {
      documentStatus = new DocumentStatus();
    }
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
