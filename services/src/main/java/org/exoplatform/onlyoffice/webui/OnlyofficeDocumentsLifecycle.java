/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.callModule;

import javax.jcr.Node;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Application;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Listener that will initialize Onlyoffice integration for ECMS Documents app.
 * <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeDocumentsLifecycle.java 00000 Mar 21, 2019 pnedonosko
 *          $
 */
public class OnlyofficeDocumentsLifecycle extends AbstractOnlyofficeLifecycle {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(OnlyofficeDocumentsLifecycle.class);

  /**
   * Instantiates a new Onlyoffice documents lifecycle.
   */
  public OnlyofficeDocumentsLifecycle() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    initExplorer(app, context);
    super.onEndRequest(app, context);
  }

  // ******* internals ******

  /**
   * Inits the explorer.
   *
   * @param app the app
   * @param context the context
   * @throws Exception the exception
   */
  protected void initExplorer(Application app, WebuiRequestContext webuiContext) throws Exception {
    UIJCRExplorer explorer = webuiContext.getUIApplication().findFirstComponentOfType(UIJCRExplorer.class);
    if (explorer != null) {
      try {
        OnlyofficeEditorService editorService = app.getApplicationServiceContainer()
                                                   .getComponentInstanceOfType(OnlyofficeEditorService.class);
        Node node = explorer.getCurrentNode();
        if (editorService.canEditDocument(node)) {
          String docId = editorService.initDocument(node);
          callModule("initExplorer('" + docId + "');");
        }
      } catch (JsonProcessingException e) {
        LOG.error("Couldn't create JSON from cometInfo object.", e);
      } catch (Exception e) {
        LOG.error("Couldn't init document of node. ", e);
      }
    }
  }
}
