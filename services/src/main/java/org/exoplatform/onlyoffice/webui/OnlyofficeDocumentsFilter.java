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

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.webui.application.WebuiApplication;

/**
 * Filter to add listener that will initialize Onlyoffice integration in
 * Platform apps. <br>
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeDocumentsFilter.java 00000 Mar 21, 2019 pnedonosko $
 */
public class OnlyofficeDocumentsFilter extends AbstractOnlyofficeWebFilter {

  /** The Constant LOG. */
  protected static final Log    LOG                  = ExoLogger.getLogger(OnlyofficeDocumentsLifecycle.class);

  /** The Constant ECMS_EXPLORER_APP_ID. */
  protected static final String ECMS_EXPLORER_APP_ID = "ecmexplorer/FileExplorerPortlet";

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    WebAppController controller = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WebAppController.class);
    WebuiApplication app = controller.getApplication(ECMS_EXPLORER_APP_ID);
    // XXX It's known that since portal start this app will not present at very
    // first request to it (Documents Explorer app), thus the filter will not
    // add the lifecycle and it will not initialize the app in the first
    // request.
    if (app != null) {
      // Initialize ECMS Explorer app, this will happen once per app lifetime
      @SuppressWarnings("rawtypes")
      final List<ApplicationLifecycle> lifecycles = app.getApplicationLifecycle();
      if (canAddLifecycle(lifecycles, OnlyofficeDocumentsLifecycle.class)) {
        synchronized (lifecycles) {
          if (canAddLifecycle(lifecycles, OnlyofficeDocumentsLifecycle.class)) {
            lifecycles.add(new OnlyofficeDocumentsLifecycle());
          }
        }
      }
    }
    chain.doFilter(request, response);
  }
}
