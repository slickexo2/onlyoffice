package org.exoplatform.onlyoffice.documents;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.ecm.webui.component.explorer.documents.DocumentTemplate;
import org.exoplatform.ecm.webui.component.explorer.documents.NewDocumentService;
import org.exoplatform.ecm.webui.component.explorer.documents.NewDocumentTemplatePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class OnlyOfficeNewDocumentTemplatePlugin.
 */
public class OnlyOfficeNewDocumentTemplatePlugin extends BaseComponentPlugin implements NewDocumentTemplatePlugin {

  /** The Constant LOG. */
  protected static final Log         LOG                              =
                                         ExoLogger.getLogger(OnlyOfficeNewDocumentTemplatePlugin.class);

  /**   The DOCUMENT_TYPES_CONFIGURATION param. */
  private static final String        DOCUMENT_TEMPLATES_CONFIGURATION = "document-templates-configuration";

  /** The document types. */
  protected List<DocumentTemplate>   templates                        = Collections.emptyList();

  /** The provider. */
  protected String                   provider;

  /** The new document service. */
  protected final NewDocumentService newDocumentService;


  /**
   * Instantiates a new only office new document template plugin.
   *
   * @param newDocumentService the new document service
   * @param initParams the init params
   */
  public OnlyOfficeNewDocumentTemplatePlugin(NewDocumentService newDocumentService, InitParams initParams) {
    ObjectParameter typesParam = initParams.getObjectParam(DOCUMENT_TEMPLATES_CONFIGURATION);
    if (typesParam != null) {
      Object obj = typesParam.getObject();
      if (obj != null && NewDocumentService.DocumentTemplatesConfig.class.isAssignableFrom(obj.getClass())) {
        NewDocumentService.DocumentTemplatesConfig config = NewDocumentService.DocumentTemplatesConfig.class.cast(obj);
        this.templates = config.getTemplates();
        this.provider = config.getProvider();
      } else {
        LOG.error("The document templates are not set");
      }
    }
    this.newDocumentService = newDocumentService;
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  @Override
  public String getProvider() {
    return provider;
  }

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  @Override
  public List<DocumentTemplate> getTemplates() {
    return templates;
  }

  /**
   * Creates the document.
   *
   * @param parent the parent
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  @Override
  public Node createDocument(Node parent, String title, DocumentTemplate template) throws Exception {
    LOG.debug("Creating new document {} from template {}", title, template);
    return newDocumentService.createDocument(parent, title, template);
  }

}
