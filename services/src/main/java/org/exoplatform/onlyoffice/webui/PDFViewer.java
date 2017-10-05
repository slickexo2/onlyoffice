
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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event.Phase;

import javax.portlet.PortletRequest;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PDFViewer.java 00000 Jan 31, 2016 pnedonosko $
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:resources/templates/PDFJSViewer.gtmpl", events = {
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.NextPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.PreviousPageActionListener.class,
                 phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.GotoPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.RotateRightPageActionListener.class,
                 phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.RotateLeftPageActionListener.class,
                 phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.ScalePageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.DownloadFileActionListener.class,
                 phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.ZoomInPageActionListener.class,
                 phase = Phase.DECODE),
    @EventConfig(listeners = org.exoplatform.ecm.webui.viewer.PDFViewer.ZoomOutPageActionListener.class,
                 phase = Phase.DECODE) })
@Deprecated
public class PDFViewer extends org.exoplatform.ecm.webui.viewer.PDFViewer {

  /** The Constant LOG. */
  protected static final Log LOG                                  = ExoLogger.getLogger(PDFViewer.class);

  /** XXX CONSTANTS COPIED FROM ECMS's PDFViewer because it is private there! See also {@link #isNotModernBrowser()}. */
  protected static final int MIN_IE_SUPPORTED_BROWSER_VERSION     = 9;

  /** The Constant MIN_FF_SUPPORTED_BROWSER_VERSION. */
  protected static final int MIN_FF_SUPPORTED_BROWSER_VERSION     = 20;

  /** The Constant MIN_CHROME_SUPPORTED_BROWSER_VERSION. */
  protected static final int MIN_CHROME_SUPPORTED_BROWSER_VERSION = 20;
  
  /**
   * Instantiates a new PDF viewer.
   *
   * @throws Exception the exception
   */
  public PDFViewer() throws Exception {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    // require JS and invoke its method to init the editor controls
    initContext(context);
    
    super.processRender(context);
  }
  
  /**
   * Inits the context.
   *
   * @param context the context
   * @throws Exception the exception
   */
  protected void initContext(WebuiRequestContext context) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // we store current node in the context
      String path = uiExplorer.getCurrentNode().getPath();
      String workspace = uiExplorer.getCurrentNode().getSession().getWorkspace().getName();
      OnlyofficeEditorContext.init(context, workspace, path);
    } else {
      LOG.error("Cannot find ancestor of type UIJCRExplorer in component " + this + ", parent: " + this.getParent());
    }
  }

  /**
   * XXX METHOD COPIED FROM ECMS's PDFViewer because it is private there!
   * Check if client has modern browser (IE9+, FF20+, Chrome 20+).
   *
   * @return true, if is not modern browser
   * @throws Exception the exception
   */
  protected boolean isNotModernBrowser() throws Exception {
    PortletRequestContext requestContext = PortletRequestContext.getCurrentInstance();
    PortletRequest portletRequest = requestContext.getRequest();
    String userAgent = portletRequest.getProperty("user-agent");
    boolean isChrome = (userAgent.indexOf("Chrome/") != -1);
    boolean isMSIE = (userAgent.indexOf("MSIE") != -1);
    boolean isFirefox = (userAgent.indexOf("Firefox/") != -1);
    String version = "1";
    if (isFirefox) {
      // Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:13.0) Gecko/20100101 Firefox/13.0
      version = userAgent.replaceAll("^.*?Firefox/", "").replaceAll("\\.\\d+", "");
    } else if (isChrome) {
      // Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52
      // Safari/536.5
      version = userAgent.replaceAll("^.*?Chrome/(\\d+)\\..*$", "$1");
    } else if (isMSIE) {
      // Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727;
      // .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E)
      version = userAgent.replaceAll("^.*?MSIE\\s+(\\d+).*$", "$1");
    }

    boolean unsupportedBrowser = (isFirefox && Integer.parseInt(version) < MIN_FF_SUPPORTED_BROWSER_VERSION)
        || (isChrome && Integer.parseInt(version) < MIN_CHROME_SUPPORTED_BROWSER_VERSION)
        || (isMSIE && Integer.parseInt(version) < MIN_IE_SUPPORTED_BROWSER_VERSION);
    return unsupportedBrowser;
  }

}
