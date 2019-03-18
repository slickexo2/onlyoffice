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
package org.exoplatform.onlyoffice.webui;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeClientContext.java 00000 Mar 18, 2019 pnedonosko $
 */
public class OnlyofficeClientContext {

  /** The Constant JAVASCRIPT. */
  protected static final String JAVASCRIPT             = "OnlyofficeClientContext_Javascript".intern();

  protected static final String CLIENT_RESOURCE_PREFIX = "OnlyofficeEditorClient.";

  protected static final Log    LOG                    = ExoLogger.getLogger(OnlyofficeClientContext.class);

  private final RequireJS       require;

  /**
   * Instantiates a new onlyoffice client context.
   *
   * @param requestContext the request context
   */
  private OnlyofficeClientContext(WebuiRequestContext requestContext) {
    JavascriptManager js = requestContext.getJavascriptManager();
    this.require = js.require("SHARED/onlyoffice", "onlyoffice");

    //
    String messagesJson;
    try {
      ResourceBundleService i18nService = requestContext.getApplication()
                                                        .getApplicationServiceContainer()
                                                        .getComponentInstanceOfType(ResourceBundleService.class);
      ResourceBundle res = i18nService.getResourceBundle("locale.onlyoffice.OnlyofficeClient", requestContext.getLocale());
      Map<String, String> resMap = new HashMap<String, String>();
      for (Enumeration<String> keys = res.getKeys(); keys.hasMoreElements();) {
        String key = keys.nextElement();
        String bundleKey;
        if (key.startsWith(CLIENT_RESOURCE_PREFIX)) {
          bundleKey = key.substring(CLIENT_RESOURCE_PREFIX.length());
        } else {
          bundleKey = key;
        }
        resMap.put(bundleKey, res.getString(key));
      }
      messagesJson = new JsonGeneratorImpl().createJsonObjectFromMap(resMap).toString();
    } catch (JsonException e) {
      LOG.warn("Cannot serialize messages bundle JSON", e);
      messagesJson = "{}";
    } catch (Exception e) {
      LOG.warn("Cannot build messages bundle", e);
      messagesJson = "{}";
    }
    callOnModule(new StringBuilder("initMessages(").append(messagesJson).append(");").toString());
  }

  private RequireJS appRequireJS() {
    return require;
  }
  
  private void callOnModule(String code) {
    require.addScripts(new StringBuilder("onlyoffice.").append(code).append("\n").toString());
  }

  private void showClientError(String title, String message) {
    callOnModule(new StringBuilder("showError('").append(title).append("', '" + message + "');").toString());
  }

  private static OnlyofficeClientContext context() {
    OnlyofficeClientContext context;
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null || !OnlyofficeClientContext.class.isAssignableFrom(obj.getClass())) {
      synchronized (requestContext) {
        obj = requestContext.getAttribute(JAVASCRIPT);
        if (obj == null || !OnlyofficeClientContext.class.isAssignableFrom(obj.getClass())) {
          context = new OnlyofficeClientContext(requestContext);
          requestContext.setAttribute(JAVASCRIPT, context);
        } else {
          context = OnlyofficeClientContext.class.cast(obj);
        }
      }
    } else {
      context = OnlyofficeClientContext.class.cast(obj);
    }
    return context;
  }

  /**
   * Adds the script to be called on <code>onlyoffice</code> module. Finally it
   * will appear as <code>onlyoffice.myMethod(...)</code>, where myMethod(...) it's what given as code parameter.
   *
   * @param code the code of a method to invoke on onlyoffice module
   */
  public static void callModule(String code) {
    context().callOnModule(code);
  }
  
  /**
   * Return Web UI app's RequireJS instance.
   *
   * @return the require JS
   */
  public static RequireJS requireJS() {
    return context().appRequireJS();
  }

  public static void showError(String title, String message) {
    context().showClientError(title, message);
  }

}
