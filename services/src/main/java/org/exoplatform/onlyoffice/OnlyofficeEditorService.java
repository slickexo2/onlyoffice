/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.onlyoffice;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveService.java 00000 Feb 14, 2013 pnedonosko $
 */
public interface OnlyofficeEditorService {

  /**
   * Return existing editor for given user and node. If user not valid or editor not open for given node then
   * <code>null</code> will be returned.
   * 
   * @param userId {@link String}
   * @param workspace {@link String}
   * @param path {@link String}
   * @return {@link Config} or <code>null</code>
   * @throws OnlyofficeEditorException
   * @throws RepositoryException
   */
  Config getEditor(String userId, String workspace, String path) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Create an editor config for given config node.
   * 
   * @param userId {@link String}
   * @param workspace {@link String}
   * @param path {@link String}
   * @return {@link Config} instance in case of successful creation or <code>null</code> if local file type
   *         not supported.
   * @throws OnlyofficeEditorException if editor exception happened
   * @throws RepositoryException if storage exception happened
   */
  Config createEditor(String userId, String workspace, String path) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Update a config associated with given editor {@link Config} instance. A {@link Node} from that the
   * config was created will be updated. This operation will close the editor and it will not be usable
   * after that.
   * 
   * @param userId {@link String}
   * @param status {@link DocumentStatus}
   * @throws OnlyofficeEditorException if editor exception happened
   * @throws RepositoryException if storage exception happened
   */
  void updateDocument(String userId, DocumentStatus status) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Get file content.
   * 
   * @param userId {@link String}
   * @param fileKey {@link String}
   * @return {@link DocumentContent}
   * @throws OnlyofficeEditorException
   * @throws RepositoryException
   */
  DocumentContent getContent(String userId, String fileKey) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Check does given host can download document content by this service. It's optional feature, configurable
   * and allow only configured Document server by default.
   * 
   * @param hostName {@link String}
   * @return <code>true</code> if client host with given name can download document content,
   *         <code>false</code> otherwise.
   */
  boolean canDownloadBy(String hostName);

  /**
   * Local state of editing document.
   * 
   * @param userId {@link String}
   * @param fileKey {@link String}
   * @throws OnlyofficeEditorException
   * @return {@link ChangeState}
   */
  ChangeState getState(String userId, String fileKey) throws OnlyofficeEditorException;

  /**
   * Add listener to the service.
   * 
   * @param listener
   */
  void addListener(OnlyofficeEditorListener listener);

  /**
   * Remove listener from the service.
   * 
   * @param listener
   */
  void removeListener(OnlyofficeEditorListener listener);
}
