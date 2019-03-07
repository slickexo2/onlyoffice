/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.onlyoffice;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The DocumentTypePlugin.
 */
public class DocumentTypePlugin extends BaseComponentPlugin {

  /** The Constant LOG. */
  protected static final Log  LOG                          = ExoLogger.getLogger(OnlyofficeEditorServiceImpl.class);

  /**  The DOCUMENT_TYPES_CONFIGURATION param *. */
  private static final String DOCUMENT_TYPES_CONFIGURATION = "document-types-configuration";

  /**  The mime types *. */
  protected List<String>      mimeTypes;

  /**
   * Initializes a DocumentTypePlugin.
   *
   * @param initParams the initParams
   */
  public DocumentTypePlugin(InitParams initParams) {
    ObjectParameter typesParam = initParams.getObjectParam(DOCUMENT_TYPES_CONFIGURATION);
    if (typesParam != null) {
      Object obj = typesParam.getObject();
      if (obj != null && OnlyofficeEditorServiceImpl.DocumentTypesConfig.class.isAssignableFrom(obj.getClass())) {
        this.mimeTypes = OnlyofficeEditorServiceImpl.DocumentTypesConfig.class.cast(obj).getMimeTypes();
      } else {
        this.mimeTypes = new ArrayList<String>();
        LOG.error("The mimetypes are not set");
      }
    }
  }

  /**
   * Gets the mimeTypes.
   *
   * @return the mimeTypes
   */
  public List<String> getMimeTypes() {
    return mimeTypes;
  }

}
