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

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
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
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import java.util.Arrays;
import java.util.List;

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
public class OnlyofficeOpenManageComponent extends UIAbstractManagerComponent {

  protected static final Log                   LOG     = ExoLogger.getLogger(OnlyofficeOpenManageComponent.class);

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
      // new IsNotLockedFilter()
      new CanOpenOnlyofficeFilter() });

  public static class OnlyofficeOpenActionListener extends EventListener<OnlyofficeOpenManageComponent> {
    public void execute(Event<OnlyofficeOpenManageComponent> event) throws Exception {
      // OnlyofficeOpenManageComponent comp = event.getSource();
      WebuiRequestContext context = event.getRequestContext();
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);

      String workspace = explorer.getCurrentWorkspace();
      String path = explorer.getCurrentNode().getPath();
      editorsUI.open(context.getRemoteUser(), workspace, path);

      OnlyofficeEditorContext.init(context, workspace, path);
      OnlyofficeEditorContext.open(context);

      // Refresh UI components
      UIDocumentWorkspace docWorkspace = explorer.findFirstComponentOfType(UIDocumentWorkspace.class);
      context.addUIComponentToUpdateByAjax(docWorkspace);
      UIActionBar actionBar = explorer.findFirstComponentOfType(UIActionBar.class);
      context.addUIComponentToUpdateByAjax(actionBar);

      ////// TODO cleanup
      // UIDocumentInfo docInfo = docWorkspace.findComponentById(UIDocumentInfo.class.getSimpleName());
      // OnlyofficeEditor editor = docWorkspace.findFirstComponentOfType(OnlyofficeEditor.class);
      // if (editor == null) {
      // PDFViewer viewer = docInfo.getChild(PDFViewer.class);
      // if (viewer == null) {
      // // Show warning
      // UIApplication uiApp = comp.getAncestorOfType(UIApplication.class);
      // uiApp.addMessage(new ApplicationMessage("OnlyofficeEditor.message.ViewerNotFound",
      // null,
      // ApplicationMessage.WARNING));
      // event.getRequestContext().addUIComponentToUpdateByAjax(comp);
      // return;
      // }
      //
      // // hide viewer
      // viewer.setRendered(false);
      // // create editor and open it below
      // editor = docInfo.addChild(OnlyofficeEditor.class, null, null);
      // // docInfo.setRenderedChild(OnlyofficeEditor.class);
      // docWorkspace.setRenderedChild(UIDocumentContainer.class);
      // event.getRequestContext().addUIComponentToUpdateByAjax(docWorkspace);
      // } else {
      // // else, editor already shown - just (re)open it - this should not happen due to filter
      // event.getRequestContext().addUIComponentToUpdateByAjax(editor);
      // }
      // editor.open();
    }
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
    // filters.add(new AcceptFilter(editorsUI.canOpen(context.getRemoteUser(),
    // explorer.getCurrentWorkspace(),
    // explorer.getCurrentNode().getPath())));
    // return filters;
    // } catch (Exception e) {
    // LOG.error("Error reading current node in explorer", e);
    // return null;
    // }
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

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
