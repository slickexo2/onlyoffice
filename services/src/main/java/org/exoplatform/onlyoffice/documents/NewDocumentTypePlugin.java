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
package org.exoplatform.onlyoffice.documents;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.onlyoffice.documents.NewDocumentService.NewDocumentTypesConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class NewDocumentTypePlugin.
 */
public class NewDocumentTypePlugin extends BaseComponentPlugin {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(NewDocumentTypePlugin.class);
  
  /**   The DOCUMENT_TYPES_CONFIGURATION param. */
  private static final String DOCUMENT_TYPES_CONFIGURATION = "document-types-configuration";
  
  /** The document types. */
  protected List<NewDocumentType> types;
  
  /**
   * Gets the types.
   *
   * @return the types
   */
  public List<NewDocumentType> getTypes() {
    return types;
  }

  /**
   * Instantiates a new new document type plugin.
   *
   * @param initParams the init params
   */
  public NewDocumentTypePlugin(InitParams initParams) {
    ObjectParameter typesParam = initParams.getObjectParam(DOCUMENT_TYPES_CONFIGURATION);
    if (typesParam != null) {
      Object obj = typesParam.getObject();
      if (obj != null && NewDocumentService.NewDocumentTypesConfig.class.isAssignableFrom(obj.getClass())) {
        this.types = NewDocumentService.NewDocumentTypesConfig.class.cast(obj).getTypes();
      } else {
        this.types = new ArrayList<>();
        LOG.error("The new document types are not set");
      }
    }
  }

}
