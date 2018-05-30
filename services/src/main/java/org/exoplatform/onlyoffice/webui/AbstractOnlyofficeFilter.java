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

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJcrExplorerContainer;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AbstractOnlyofficeFilter.java 00000 Mar 1, 2016 pnedonosko $
 */
public abstract class AbstractOnlyofficeFilter extends UIExtensionAbstractFilter {

  /**
   * Instantiates a new abstract onlyoffice filter.
   */
  public AbstractOnlyofficeFilter() {
  }

  /**
   * Instantiates a new abstract onlyoffice filter.
   *
   * @param messageKey the message key
   */
  public AbstractOnlyofficeFilter(String messageKey) {
    super(messageKey);
  }

  /**
   * Instantiates a new abstract onlyoffice filter.
   *
   * @param messageKey the message key
   * @param type the type
   */
  public AbstractOnlyofficeFilter(String messageKey, UIExtensionFilterType type) {
    super(messageKey, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    if (context != null) {
      Node contextNode = (Node) context.get(Node.class.getName());

      UIContainer contextContainer = null;
      
      UIJCRExplorer jcrExplorer = (UIJCRExplorer) context.get(UIJCRExplorer.class.getName());
      if (jcrExplorer != null) {
        contextContainer = jcrExplorer;
      }
      
      // search in ECMS explorer first
      if (contextNode == null) {
        if (jcrExplorer != null) {
          contextNode = jcrExplorer.getCurrentNode();
        }

        if (contextNode == null) {
          WebuiRequestContext reqContext = WebuiRequestContext.getCurrentInstance();
          UIApplication uiApp = reqContext.getUIApplication();
          UIJcrExplorerContainer jcrExplorerContainer = uiApp.getChild(UIJcrExplorerContainer.class);
          if (jcrExplorerContainer != null) {
            contextContainer = jcrExplorer = jcrExplorerContainer.getChild(UIJCRExplorer.class);
            contextNode = jcrExplorer.getCurrentNode();
          }

          // case of file preview in Social activity stream
          if (contextNode == null) {
            UIActivitiesContainer uiActivitiesContainer = uiApp.findFirstComponentOfType(UIActivitiesContainer.class);
            if (uiActivitiesContainer != null) {
              contextContainer = uiActivitiesContainer;
              PopupContainer uiPopupContainer = uiActivitiesContainer.getPopupContainer();
              if (uiPopupContainer != null) {
                UIBaseNodePresentation docViewer = uiPopupContainer.findComponentById("UIDocViewer");
                if (docViewer != null) {
                  contextNode = docViewer.getNode();
                }
              }
            }
          }
        }
      }

      if (contextNode != null) {
        String userId = WebuiRequestContext.getCurrentInstance().getRemoteUser();
        return accept(userId, contextNode, contextContainer);
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }

  /**
   * Accept.
   *
   * @param userId the user id
   * @param node the node
   * @param container the container
   * @return true, if successful
   * @throws RepositoryException the repository exception
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  protected abstract boolean accept(String userId, Node node, UIContainer container) throws RepositoryException, OnlyofficeEditorException;

}
