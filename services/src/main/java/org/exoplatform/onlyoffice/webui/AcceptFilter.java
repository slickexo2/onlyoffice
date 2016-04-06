
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

import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AcceptFilter.java 00000 Mar 1, 2016 pnedonosko $
 * 
 */
public class AcceptFilter extends AbstractOnlyofficeFilter {

  protected final boolean accept;

  /**
   * 
   */
  public AcceptFilter() {
    this.accept = false;
  }

  /**
   * 
   */
  public AcceptFilter(boolean accept) {
    this.accept = accept;
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
  protected boolean accept(String userId, Node node) throws RepositoryException, OnlyofficeEditorException {
    if (accept) {
      return true;
    }
    return false;
  }

}
