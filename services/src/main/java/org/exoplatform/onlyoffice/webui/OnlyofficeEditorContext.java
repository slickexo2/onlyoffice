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

  /** The Constant JAVASCRIPT. */
  protected static final String JAVASCRIPT = "OnlyofficeEditorContext_Javascript".intern();

  /** The Constant LOG. */
  protected static final Log    LOG        = ExoLogger.getLogger(OnlyofficeEditorContext.class);

  /**
   * Initialize request with Onlyoffice support for given JCR location.
   * 
   * @param requestContext {@link RequestContext}
   * @param workspace {@link String}
   * @param nodePath {@link String}
   * @throws OnlyofficeEditorException if cannot auth url from the provider
   */
  public static void init(RequestContext requestContext, String workspace, String nodePath) throws OnlyofficeEditorException {

    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null) {
      OnlyofficeEditorContext context = new OnlyofficeEditorContext(requestContext);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Init Onlyoffice editor for " + workspace + ":" + nodePath);
      }

      context.init(workspace, nodePath);

      requestContext.setAttribute(JAVASCRIPT, context);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context already initialized");
      }
    }
  }

  /**
   * Open.
   *
   * @param requestContext the request context
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
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
  
  /**
   * Show.
   *
   * @param requestContext the request context
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  public static void show(RequestContext requestContext) throws OnlyofficeEditorException {
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj != null) {
      OnlyofficeEditorContext context = (OnlyofficeEditorContext) obj;
      context.show();
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Request context not initialized");
      }
    }
  }

  /**
   * Close.
   *
   * @param requestContext the request context
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
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
   * @param requestContext the request context
   * @param title {@link String}
   * @param message {@link String}
   * @throws RepositoryException the repository exception
   * @throws OnlyofficeEditorException the onlyoffice editor exception
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

  /** The Constant messages. */
  private final static ThreadLocal<Map<String, String>> messages = new ThreadLocal<Map<String, String>>();

  /** The require. */
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

  /**
   * Inits the.
   *
   * @param workspace the workspace
   * @param nodePath the node path
   * @return the onlyoffice editor context
   */
  private OnlyofficeEditorContext init(String workspace, String nodePath) {
    require.addScripts("onlyoffice.init('" + workspace + "','" + nodePath + "');");
    return this;
  }

  /**
   * Open.
   *
   * @return the onlyoffice editor context
   */
  private OnlyofficeEditorContext open() {
    require.addScripts("onlyoffice.open();");
    return this;
  }
  
  /**
   * Show.
   *
   * @return the onlyoffice editor context
   */
  private OnlyofficeEditorContext show() {
    require.addScripts("onlyoffice.show();");
    return this;
  }
  
  /**
   * Close.
   *
   * @return the onlyoffice editor context
   */
  private OnlyofficeEditorContext close() {
    require.addScripts("onlyoffice.close();");
    return this;
  }

  /**
   * Show info.
   *
   * @param title the title
   * @param text the text
   * @return the onlyoffice editor context
   */
  private OnlyofficeEditorContext showInfo(String title, String text) {
    // require.addScripts("onlyoffice.showInfo('" + title + "','" + text + "');");
    return this;
  }
}
