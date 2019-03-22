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
package org.exoplatform.onlyoffice.portlet;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.callModule;

import javax.portlet.GenericPortlet;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.webui.OnlyofficeContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;

public class DocumentStatePortlet extends GenericPortlet {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(DocumentStatePortlet.class);

  /**
   * Renders the portlet on a page.
   *
   * @param request the request
   * @param response the response
   */
  @Override
  protected void doView(final RenderRequest request, final RenderResponse response) {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    // These attributes saved in portal context by OnlyofficeDocumentsLifecycle
    String docWs = (String) context.getAttribute(OnlyofficeContext.DOCUMENT_WORKSPACE_ATTRIBUTE);
    String docPath = (String) context.getAttribute(OnlyofficeContext.DOCUMENT_PATH_ATTRIBUTE);
    if (docWs != null && docPath != null) {
      try {
        OnlyofficeEditorService editorService = context.getApplication()
                                                       .getApplicationServiceContainer()
                                                       .getComponentInstanceOfType(OnlyofficeEditorService.class);

        String docId = editorService.getDocumentId(docWs, docPath);
        if (docId != null) {
          callModule("initExplorer('" + docId + "');");
        }
      } catch (Exception e) {
        LOG.error("Couldn't init document of node {}:{}", docWs, docPath, e);
      }
    }
  }

}
