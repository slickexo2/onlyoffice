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

import javax.portlet.GenericPortlet;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.onlyoffice.webui.OnlyofficeContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.RequestContext;
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
    // This code will be executed once per a portlet app, thus first time when
    // navigated to a portal page, all other calls inside the page (ajax
    // calls), will not cause rendering of this portlet, except if this will not
    // be issued explicitly.
    if (LOG.isDebugEnabled()) {
      RequestContext context = WebuiRequestContext.getCurrentInstance();
      if (context.getParentAppRequestContext() != null) {
        context = context.getParentAppRequestContext();
      }
      // These attributes saved in portal context by
      // OnlyofficeDocumentsLifecycle
      String userId = (String) context.getAttribute(OnlyofficeContext.USERID_ATTRIBUTE);
      if (userId != null) {
        String nodeWs = (String) context.getAttribute(OnlyofficeContext.DOCUMENT_WORKSPACE_ATTRIBUTE);
        String nodePath = (String) context.getAttribute(OnlyofficeContext.DOCUMENT_PATH_ATTRIBUTE);
        LOG.debug("Work in documents explorer for {} ({}), node: {}:{}", userId, request.getRemoteUser(), nodeWs, nodePath);
      }
    }
  }

}
