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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.webui.application.WebuiApplication;

/**
 * Filter to add listener that will initialize Onlyoffice editor app. <br>
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorFilter.java 00000 Mar 21, 2019 pnedonosko $
 */
public class OnlyofficeEditorFilter implements Filter {

  /** The Constant LOG. */
  protected static final Logger LOG = LoggerFactory.getLogger(OnlyofficeEditorFilter.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    WebAppController controller = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WebAppController.class);
    WebuiApplication app = controller.getApplication(PortalApplication.PORTAL_APPLICATION_ID);
    if (app != null) {
      synchronized (app) {
        OnlyofficeEditorLifecycle lifecycle = new OnlyofficeEditorLifecycle();
        try {
          app.getApplicationLifecycle().add(lifecycle);
          chain.doFilter(request, response);
        } finally {
          app.getApplicationLifecycle().remove(lifecycle);
        }
      }
    }
  }
}
