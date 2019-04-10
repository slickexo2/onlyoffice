package org.exoplatform.onlyoffice;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NewDocumentTypePlugin extends BaseComponentPlugin {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(NewDocumentTypePlugin.class);
  
  /**  The DOCUMENT_TYPES_CONFIGURATION param *. */
  private static final String DOCUMENT_TYPES_CONFIGURATION = "document-types-configuration";
  
  /** The document types. */
  protected Map<String, String> types;
  
  public NewDocumentTypePlugin(InitParams initParams) {
    ObjectParameter typesParam = initParams.getObjectParam(DOCUMENT_TYPES_CONFIGURATION);
    if (typesParam != null) {
      Object obj = typesParam.getObject();
      if (obj != null && NewDocumentService.NewDocumentTypesConfig.class.isAssignableFrom(obj.getClass())) {
        this.types = NewDocumentService.NewDocumentTypesConfig.class.cast(obj).getTypes();
      } else {
        this.types = new HashMap<>();
        LOG.error("The new document types are not set");
      }
    }
  }

}
