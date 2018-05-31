
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

import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CanCloseOnlyofficeFilter.java 00000 Mar 1, 2016 pnedonosko $
 */
public class CanCloseOnlyofficeFilter extends FileViewFilter {

  /**
   * Instantiates a new can close onlyoffice filter.
   */
  public CanCloseOnlyofficeFilter() {
  }

  /**
   * Instantiates a new can close onlyoffice filter.
   *
   * @param messageKey the message key
   */
  public CanCloseOnlyofficeFilter(String messageKey) {
    super(messageKey);
  }

  /**
   * Instantiates a new can close onlyoffice filter.
   *
   * @param messageKey the message key
   * @param type the type
   */
  public CanCloseOnlyofficeFilter(String messageKey, UIExtensionFilterType type) {
    super(messageKey, type);
  }

  /**
   * {@inheritDoc}
   */
  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean accept(String userId, Node node, UIContainer container) throws RepositoryException, OnlyofficeEditorException {
    OnlyofficeEditorUIService editorsUI = WCMCoreUtils.getService(OnlyofficeEditorUIService.class);
    return super.accept(userId, node, container) && editorsUI.canClose(userId, node.getSession().getWorkspace().getName(), node.getPath());
  }

}
