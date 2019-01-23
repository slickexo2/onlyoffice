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

import java.io.IOException;
import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;

/**
 * The Class EditorPortlet.
 */
public class EditorPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log        LOG = ExoLogger.getLogger(EditorPortlet.class);

  /** The onlyoffice. */
  private OnlyofficeEditorService onlyoffice;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws PortletException {
    super.init();
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    this.onlyoffice = container.getComponentInstanceOfType(OnlyofficeEditorService.class);
  }

  /**
   * View.
   *
   * @param request the request
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws PortletException the portlet exception
   */
  @RenderMode(name = "view")
  public void view(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    RequestContext rqContext = WebuiRequestContext.getCurrentInstance();
    RequireJS js = ((WebuiRequestContext) rqContext).getJavascriptManager().require("SHARED/onlyoffice", "onlyoffice");

    String docId = rqContext.getRequestParameter("docId");
    if (docId != null) {
      try {
        Config config = onlyoffice.createEditor(request.getScheme(), requestHost(request), request.getRemoteUser(), null, docId);
        if (config != null) {
          if (config.getEditorConfig().getLang() == null) {
            if (request.getLocale() != null) {
              // If user lang not defined use current request one
              config.getEditorConfig().setLang(request.getLocale().getLanguage());
            } else {
              // Otherwise use system default one
              config.getEditorConfig().setLang(Locale.getDefault().getLanguage());
            }
          }
          js.addScripts("onlyoffice.initEditor(" + config.toJSON() + ");");
        } else {
          js.addScripts("onlyoffice.showError('Error','Editor cannot be created. Please retry.');");
        }
      } catch (RepositoryException e) {
        LOG.error("Error reading document node by ID: " + docId, e);
        js.addScripts("onlyoffice.showError('Error','Cannot read the document. Please retry.');");
      } catch (OnlyofficeEditorException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (JsonException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      js.addScripts("onlyoffice.showError('Error','Wrong request: document ID required.');");
    }

    PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/pages/editor.jsp");
    prDispatcher.include(request, response);
  }

  /**
   * Request host.
   *
   * @param request the request
   * @return the string
   */
  protected String requestHost(PortletRequest request) {
    StringBuilder host = new StringBuilder(request.getServerName());
    int port = request.getServerPort();
    if (port >= 0 && port != 80 && port != 443) {
      host.append(':');
      host.append(port);
    }
    return host.toString();
  }
}
