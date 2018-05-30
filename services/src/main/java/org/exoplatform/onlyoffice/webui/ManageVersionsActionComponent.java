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
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.onlyoffice.webui.ManageVersionsActionComponent.ManageVersionsActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;

/**
 * Overrides original ECMS component to update action bar when opening Versions view (this will set correct
 * Edit/Close menu item).<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ManageVersionsActionComponent.java 00000 May 23, 2018 pnedonosko $
 * 
 */
@ComponentConfig(events = { @EventConfig(listeners = ManageVersionsActionListener.class) })
public class ManageVersionsActionComponent extends org.exoplatform.ecm.webui.component.explorer.control.action.ManageVersionsActionComponent {

  /**
   * The listener interface for receiving manageVersionsAction events.
   * The class that is interested in processing a manageVersionsAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addManageVersionsActionListener<code> method. When
   * the manageVersionsAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ManageVersionsActionListener extends
                                                   org.exoplatform.ecm.webui.component.explorer.control.action.ManageVersionsActionComponent.ManageVersionsActionListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void processEvent(Event<org.exoplatform.ecm.webui.component.explorer.control.action.ManageVersionsActionComponent> event) throws Exception {
      super.processEvent(event);

      // Also update action bar to refresh Onlyoffice menu items
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIActionBar uiActionBar = uiExplorer.findFirstComponentOfType(UIActionBar.class);
      if (uiActionBar != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiActionBar);
      }
    }
  }

}
