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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import java.util.Arrays;
import java.util.List;

/**
 * Close Onlyoffice editor and refresh the file view.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeCloseManageComponent.java 00000 Mar 01, 2016 pnedonosko $
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
    @EventConfig(listeners = OnlyofficeCloseManageComponent.OnlyofficeCloseActionListener.class) })
public class OnlyofficeCloseManageComponent extends UIAbstractManagerComponent {

  /** The Constant LOG. */
  protected static final Log                   LOG     = ExoLogger.getLogger(OnlyofficeCloseManageComponent.class);

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      // TODO new IsNotLockedFilter()
      new CanCloseOnlyofficeFilter() });

  /**
   * The listener interface for receiving onlyofficeCloseAction events.
   * The class that is interested in processing a onlyofficeCloseAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addOnlyofficeCloseActionListener</code> method. When
   * the onlyofficeCloseAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see OnlyofficeCloseActionEvent
   */
  public static class OnlyofficeCloseActionListener extends UIActionBarActionListener<OnlyofficeCloseManageComponent> {
    
    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<OnlyofficeCloseManageComponent> event) throws Exception {
      WebuiRequestContext context = event.getRequestContext();
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);

      String workspace = explorer.getCurrentWorkspace();
      String path = explorer.getCurrentNode().getPath();
      editorsUI.close(context.getRemoteUser(), workspace, path);

      OnlyofficeEditorContext.init(context, workspace, path);
      OnlyofficeEditorContext.close(context);

      event.getSource().setRendered(false); // hide this menu, TODO is it required?

      // Refresh UI components: only this menu
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    // init context where this action appears
    initContext(PortalRequestContext.getCurrentInstance());
    return super.renderEventURL(ajax, name, beanId, params);
  }

  /**
   * Inits the context.
   *
   * @param context the context
   * @throws Exception the exception
   */
  protected void initContext(RequestContext context) throws Exception {
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
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
