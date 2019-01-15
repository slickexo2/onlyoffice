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

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanEditDocFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * Open Onlyoffice editor in file view.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeComponent.java 00000 Mar 01, 2016 pnedonosko $
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class,
                 events = { @EventConfig(listeners = OnlyofficeOpenManageComponent.OnlyofficeOpenActionListener.class) })
public class OnlyofficeOpenManageComponent extends AbstractOnlyofficeManageComponent {

  /** The Constant LOG. */
  protected static final Log                   LOG     = ExoLogger.getLogger(OnlyofficeOpenManageComponent.class);

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      // new IsNotLockedFilter() // TODO should we care?
      new CanEditDocFilter(),
      new CanOpenOnlyofficeFilter() });

  /**
   * The listener interface for receiving onlyofficeOpenAction events.
   * The class that is interested in processing a onlyofficeOpenAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addOnlyofficeOpenActionListener</code> method. When
   * the onlyofficeOpenAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  public static class OnlyofficeOpenActionListener extends UIActionBarActionListener<OnlyofficeOpenManageComponent> {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<OnlyofficeOpenManageComponent> event) throws Exception {
      WebuiRequestContext context = event.getRequestContext();
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);

      String workspace = explorer.getCurrentWorkspace();
      String path = explorer.getCurrentNode().getPath();
      // call open() explicitly here for UI filter reason (to show Close menu), when document will be loading
      // to the editor it also will be called by the service's createEditor() invoked via REST service
      editorsUI.open(context.getRemoteUser(), workspace, path);

      OnlyofficeEditorContext.init(context, workspace, path);
      OnlyofficeEditorContext.open(context);

      // Refresh UI components
      UIDocumentWorkspace docWorkspace = explorer.findFirstComponentOfType(UIDocumentWorkspace.class);
      context.addUIComponentToUpdateByAjax(docWorkspace);
      UIActionBar actionBar = explorer.findFirstComponentOfType(UIActionBar.class);
      context.addUIComponentToUpdateByAjax(actionBar);
    }
  }

  /**
   * Gets the filters.
   *
   * @return the filters
   */
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }
}
