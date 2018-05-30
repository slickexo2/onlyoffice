
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
package org.exoplatform.onlyoffice.webui;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentContainer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Allow extension only in file view mode of JCR explorer.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileViewFilter.java 00000 May 23, 2018 pnedonosko $
 */
public class FileViewFilter extends AbstractOnlyofficeFilter {

  /**
   * Instantiates a new file view filter.
   */
  public FileViewFilter() {
    super();
  }

  /**
   * Instantiates a new file view filter.
   *
   * @param messageKey the message key
   * @param type the type
   */
  public FileViewFilter(String messageKey, UIExtensionFilterType type) {
    super(messageKey, type);
  }

  /**
   * Instantiates a new file view filter.
   *
   * @param messageKey the message key
   */
  public FileViewFilter(String messageKey) {
    super(messageKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(String userId, Node node, UIContainer container) throws RepositoryException, OnlyofficeEditorException {
    boolean acceptView = false;
    if (container != null) {
      // This logic assumes UIJCRExplorer as container
      UIWorkingArea uiWorkingArea = container.getChild(UIWorkingArea.class);
      if (uiWorkingArea != null) {
        UIDocumentContainer uiDocumentContainer = uiWorkingArea.findFirstComponentOfType(UIDocumentContainer.class);
        if (uiDocumentContainer != null && uiDocumentContainer.isRendered()) {
          acceptView = true;
        }
      }
    }
    return acceptView;
  }
}
