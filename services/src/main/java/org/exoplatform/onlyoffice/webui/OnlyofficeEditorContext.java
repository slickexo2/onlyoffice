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

package org.exoplatform.onlyoffice.webui;

import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Initialize Onlyoffice support in portal request.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorContext.java 00000 Jan 31, 2016 pnedonosko $
 */
public class OnlyofficeEditorContext {

  protected static final String JAVASCRIPT = "OnlyofficeEditorContext_Javascript".intern();

  protected static final Log    LOG        = ExoLogger.getLogger(OnlyofficeEditorContext.class);

  /**
   * Initialize request with Onlyoffice support for given JCR location.
   * 
   * @param requestContext {@link RequestContext}
   * @param workspace {@link String}
   * @param nodePath {@link String}
   * @throws OnlyofficeEditorException if cannot auth url from the provider
   */
  public static void init(RequestContext requestContext,
                          String workspace,
                          String nodePath) throws OnlyofficeEditorException {

    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null) {
      OnlyofficeEditorContext context = new OnlyofficeEditorContext(requestContext);

      LOG.info("Init Onlyoffice editor for " + workspace + ":" + nodePath);

      context.init(workspace, nodePath);

      // Map<String, String> contextMessages = messages.get();
      // if (contextMessages != null) {
      // for (Map.Entry<String, String> msg : contextMessages.entrySet()) {
      // context.showInfo(msg.getKey(), msg.getValue());
      // }
      // contextMessages.clear();
      // }

      requestContext.setAttribute(JAVASCRIPT, context);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context already initialized");
      }
    }
  }

  public static void open(RequestContext requestContext) throws OnlyofficeEditorException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      OnlyofficeEditorContext context = (OnlyofficeEditorContext) obj;
      context.open();
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context not initialized");
      }
    }
  }

  public static void close(RequestContext requestContext) throws OnlyofficeEditorException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      OnlyofficeEditorContext context = (OnlyofficeEditorContext) obj;
      context.close();
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context not initialized");
      }
    }
  }

  /**
   * Show info notification to the user.
   * 
   * @param title {@link String}
   * @param message {@link String}
   * @throws RepositoryException
   * @throws CloudDriveException
   */
  public static void showInfo(RequestContext requestContext, String title, String message) throws RepositoryException,
                                                                                           OnlyofficeEditorException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      OnlyofficeEditorContext context = (OnlyofficeEditorContext) obj;
      context.showInfo(title, message);
    } else {
      // store the message in thread local
      if (LOG.isDebugEnabled()) {
        LOG.debug("Context not initialized. Adding info message to local cache.");
      }

      Map<String, String> contextMessages = messages.get();
      if (contextMessages == null) {
        contextMessages = new LinkedHashMap<String, String>();
        messages.set(contextMessages);
      }
      contextMessages.put(title, message);
    }
  }

  // static variables

  private final static ThreadLocal<Map<String, String>> messages = new ThreadLocal<Map<String, String>>();

  // instance methods

  private final RequireJS                               require;

  /**
   * Internal constructor.
   * 
   * @param requestContext {@link RequestContext}
   */
  private OnlyofficeEditorContext(RequestContext requestContext) {
    JavascriptManager jsMan = ((WebuiRequestContext) requestContext).getJavascriptManager();
    this.require = jsMan.require("SHARED/onlyoffice", "onlyoffice");
  }

  private OnlyofficeEditorContext init(String workspace, String nodePath) {
    require.addScripts("onlyoffice.init('" + workspace + "','" + nodePath + "');");
    return this;
  }

  private OnlyofficeEditorContext open() {
    require.addScripts("onlyoffice.open();");
    return this;
  }

  private OnlyofficeEditorContext close() {
    require.addScripts("onlyoffice.close();");
    return this;
  }

  private OnlyofficeEditorContext showInfo(String title, String text) {
    // require.addScripts("onlyoffice.showInfo('" + title + "','" + text + "');");
    return this;
  }
}
