package org.exoplatform.onlyoffice;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The DocumentTypePlugin
 *
 */
public class DocumentTypePlugin extends BaseComponentPlugin {

  /** The Constant LOG. */
  protected static final Log  LOG                          = ExoLogger.getLogger(OnlyofficeEditorServiceImpl.class);

  /** The DOCUMENT_TYPES_CONFIGURATION param **/
  private static final String DOCUMENT_TYPES_CONFIGURATION = "document-types-configuration";

  /** The mime types **/
  protected List<String>      mimeTypes;

  /**
   * Initializes a DocumentTypePlugin
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
   * Gets the mimeTypes
   * @return the mimeTypes
   */
  public List<String> getMimeTypes() {
    return mimeTypes;
  }

}
