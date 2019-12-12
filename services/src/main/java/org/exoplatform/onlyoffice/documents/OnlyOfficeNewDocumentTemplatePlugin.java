package org.exoplatform.onlyoffice.documents;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.ecm.webui.component.explorer.documents.DocumentTemplate;
import org.exoplatform.ecm.webui.component.explorer.documents.NewDocumentService;
import org.exoplatform.ecm.webui.component.explorer.documents.NewDocumentTemplatePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class OnlyOfficeNewDocumentTemplatePlugin extends BaseComponentPlugin implements NewDocumentTemplatePlugin {

  /** The Constant LOG. */
  protected static final Log       LOG                              =
                                       ExoLogger.getLogger(OnlyOfficeNewDocumentTemplatePlugin.class);

  /**   The DOCUMENT_TYPES_CONFIGURATION param. */
  private static final String      DOCUMENT_TEMPLATES_CONFIGURATION = "document-templates-configuration";

  /** The document types. */
  protected List<DocumentTemplate> templates                        = Collections.emptyList();

  /**
   * Instantiates a new new document type plugin.
   *
   * @param initParams the init params
   */
  public OnlyOfficeNewDocumentTemplatePlugin(InitParams initParams) {
    ObjectParameter typesParam = initParams.getObjectParam(DOCUMENT_TEMPLATES_CONFIGURATION);
    if (typesParam != null) {
      Object obj = typesParam.getObject();
      if (obj != null && NewDocumentService.DocumentTemplatesConfig.class.isAssignableFrom(obj.getClass())) {
        this.templates = NewDocumentService.DocumentTemplatesConfig.class.cast(obj).getTemplates();
      } else {
        LOG.error("The document templates are not set");
      }
    }
  }

  @Override
  public String getProvider() {
    return name;
  }

  @Override
  public List<DocumentTemplate> getTemplates() {
    return templates;
  }

  @Override
  public Node createDocument(Node parent, String title, DocumentTemplate template) throws Exception {
    LOG.debug("Creating new document {} from template {}", title, template);
    NewDocumentService documentService = ExoContainerContext.getCurrentContainer()
                                                            .getComponentInstanceOfType(NewDocumentService.class);
    return documentService.createDocument(parent, title, template);
  }

}
