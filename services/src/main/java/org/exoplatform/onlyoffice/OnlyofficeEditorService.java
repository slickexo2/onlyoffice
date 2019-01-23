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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveService.java 00000 Feb 14, 2013 pnedonosko $
 */
public interface OnlyofficeEditorService {

  /**
   * Return existing editor configuration for given user and node. If editor not
   * open for given node or user then <code>null</code> will be returned. If
   * user not valid then OnlyofficeEditorException will be thrown.
   *
   * @param userId {@link String}
   * @param workspace {@link String}
   * @param path {@link String}
   * @return {@link Config} or <code>null</code>
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  Config getEditor(String userId, String workspace, String path) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Return existing editor for given temporal key. If editor or user not found
   * then <code>null</code> will be returned. If user not valid then
   * OnlyofficeEditorException will be thrown.
   *
   * @param userId the user id
   * @param key the key, see {@link #getEditor(String, String, String)}
   * @return the editor by key
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  Config getEditorByKey(String userId, String key) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Create an editor configuration for given user and node.
   *
   * @param userSchema the schema
   * @param userHost the host
   * @param userId {@link String}
   * @param workspace {@link String}
   * @param docId {@link String} a document reference in the workspace, see
   *          {@link #initDocument(String, String)}
   * @return {@link Config} instance in case of successful creation or
   *         <code>null</code> if local file type not supported.
   * @throws OnlyofficeEditorException if editor exception happened
   * @throws RepositoryException if storage exception happened
   */
  Config createEditor(String userSchema,
                      String userHost,
                      String userId,
                      String workspace,
                      String docId) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Update a configuration associated with given editor {@link Config}
   * instance. A {@link Node} from that the config was created will be updated.
   * This operation will close the editor and it will not be usable after that.
   * 
   * @param userId {@link String}
   * @param status {@link DocumentStatus}
   * @throws OnlyofficeEditorException if editor exception happened
   * @throws RepositoryException if storage exception happened
   */
  void updateDocument(String userId, DocumentStatus status) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Inits the document and returns an ID for use within editors. Node will be
   * saved by this method.
   *
   * @param node the node of the document
   * @return the string with document ID for use within editors
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  String initDocument(Node node) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Inits the document and returns an ID for use within editors. Node will be
   * saved by this method.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the string
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  String initDocument(String workspace, String path) throws OnlyofficeEditorException, RepositoryException;
  
  /**
   * Gets the editor page URL for opening at Platform server.
   *
   * @param schema the schema
   * @param host the host
   * @param workspace the workspace
   * @param docId the doc ID
   * @return the editor link
   */
  String getEditorLink(String schema, String host, String workspace, String docId);

  /**
   * Gets the document node by its ID and optionally a repository workspace.
   *
   * @param workspace the workspace, can be <code>null</code>, then a default
   *          one will be used
   * @param id the ID of a document
   * @return the document or <code>null</code> if nothing found
   * @throws RepositoryException the repository exception
   */
  Node getDocument(String workspace, String id) throws RepositoryException;

  /**
   * Get file content.
   *
   * @param userId {@link String}
   * @param fileKey {@link String}
   * @return {@link DocumentContent}
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  DocumentContent getContent(String userId, String fileKey) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Check does given host can download document content by this service. It's
   * optional feature, configurable and allow only configured Document server by
   * default.
   * 
   * @param hostName {@link String}
   * @return <code>true</code> if client host with given name can download
   *         document content, <code>false</code> otherwise.
   */
  boolean canDownloadBy(String hostName);

  /**
   * Local state of editing document.
   *
   * @param userId {@link String}
   * @param fileKey {@link String}
   * @return {@link ChangeState}
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  ChangeState getState(String userId, String fileKey) throws OnlyofficeEditorException;

  /**
   * Add listener to the service.
   *
   * @param listener the listener
   */
  void addListener(OnlyofficeEditorListener listener);

  /**
   * Remove listener from the service.
   *
   * @param listener the listener
   */
  void removeListener(OnlyofficeEditorListener listener);
}
