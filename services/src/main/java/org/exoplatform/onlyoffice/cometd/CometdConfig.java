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

import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The CometdInfo class is used to pass necessary cometd information to a
 * client.
 */
public class CometdConfig {

  /** The token. */
  private String token;

  /** The path. */
  private String path;

  /** The container name. */
  private String containerName;

  /**
   * Instantiates a new client config.
   */
  public CometdConfig() {
  }

  /**
   * Gets the token.
   *
   * @return the cometd token
   */
  public String getToken() {
    return token;
  }

  /**
   * Sets the token.
   *
   * @param token the new cometd token
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Gets the path.
   *
   * @return the cometdPath
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param cometdPath the cometdPath
   */
  public void setPath(String cometdPath) {
    this.path = cometdPath;
  }

  /**
   * Gets the container name.
   *
   * @return the container
   */
  public String getContainerName() {
    return containerName;
  }

  /**
   * Sets the container name.
   *
   * @param container the container
   */
  public void setContainerName(String container) {
    this.containerName = container;
  }

  /**
   * To JSON.
   *
   * @return the string
   * @throws JsonException the json exception
   */
  public String toJSON() throws JsonException {
    return new JsonGeneratorImpl().createJsonObject(this).toString();
    // return new
    // ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(this);
  }
}
