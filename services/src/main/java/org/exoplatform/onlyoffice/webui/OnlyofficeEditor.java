
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
import org.exoplatform.onlyoffice.webui.OnlyofficeEditor.OnCloseActionListener;
import org.exoplatform.onlyoffice.webui.OnlyofficeEditor.OnErrorActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.form.UIForm;

import java.util.Arrays;
import java.util.List;

/**
 * An UI for Onlyoffice editor in ECMS explorer.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditor.java 00000 Mar 3, 2016 pnedonosko $
 * 
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/OnlyofficeEditor.gtmpl",
                 events = { @EventConfig(listeners = OnCloseActionListener.class),
                     @EventConfig(listeners = OnErrorActionListener.class) })
public class OnlyofficeEditor extends UIForm {

  protected static final Log                   LOG     = ExoLogger.getLogger(OnlyofficeEditor.class);

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      // new IsNotLockedFilter()
      new CanShowOnlyofficeFilter() });

  public static class OnCloseActionListener extends EventListener<OnlyofficeEditor> {
    public void execute(Event<OnlyofficeEditor> event) throws Exception {
      WebuiRequestContext context = event.getRequestContext();
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);

      explorer.getSession().refresh(false);
      // TODO do we need this whole refresh here?
      // explorer.refreshExplorer();
      String workspace = explorer.getCurrentWorkspace();
      String path = explorer.getCurrentNode().getPath();
      editorsUI.closed(context.getRemoteUser(), workspace, path);

      OnlyofficeEditorContext.init(context, workspace, path);
    }
  }

  public static class OnErrorActionListener extends EventListener<OnlyofficeEditor> {
    public void execute(Event<OnlyofficeEditor> event) throws Exception {
      WebuiRequestContext context = event.getRequestContext();
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);

      explorer.getSession().refresh(false);
      // TODO do we need this whole refresh here?
      // explorer.refreshExplorer();
      String workspace = explorer.getCurrentWorkspace();
      String path = explorer.getCurrentNode().getPath();
      editorsUI.reset(context.getRemoteUser(), workspace, path);

      OnlyofficeEditorContext.init(context, workspace, path);
    }
  }

  /**
   * 
   */
  public OnlyofficeEditor() {
  }

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
    // TODO cleanup
    // List<UIExtensionFilter> filters = new ArrayList<UIExtensionFilter>(FILTERS);
    // WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    // UIJCRExplorer explorer = context.getUIApplication().findFirstComponentOfType(UIJCRExplorer.class);
    // OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);
    //
    // try {
    // // show only if already open or closing in UI service
    // filters.add(new AcceptFilter(editorsUI.canShow(context.getRemoteUser(),
    // explorer.getCurrentWorkspace(),
    // explorer.getCurrentNode().getPath())));
    // return filters;
    // } catch (Exception e) {
    // LOG.error("Error reading current node in explorer", e);
    // return null;
    // }
  }

  public void initContext() throws Exception {
    initContext(WebuiRequestContext.getCurrentInstance());
  }
  
  protected void initContext(RequestContext context) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    if (uiExplorer != null) {
      // we store current node in the context
      String path = uiExplorer.getCurrentNode().getPath();
      String workspace = uiExplorer.getCurrentNode().getSession().getWorkspace().getName();
      OnlyofficeEditorContext.init(context, workspace, path);
      
      OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);
      if (editorsUI.isOpen(context.getRemoteUser(), workspace, path)) {
        OnlyofficeEditorContext.open(context);
      }
    } else {
      LOG.error("Cannot find ancestor of type UIJCRExplorer in component " + this + ", parent: " + this.getParent());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    String id = super.getId();
    if (id == null) {
      id = this.getClass().getSimpleName();
    }
    return id;
  }
}
