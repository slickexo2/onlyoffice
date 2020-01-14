package org.exoplatform.onlyoffice.documents;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.editorLink;

import javax.jcr.Node;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.cms.documents.DocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentEditorPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class OnlyOfficeNewDocumentEditorPlugin.
 */
public class OnlyOfficeNewDocumentEditorPlugin extends BaseComponentPlugin implements NewDocumentEditorPlugin {

  /** The Constant PROVIDER_PARAM. */
  protected static final String           PROVIDER_PARAM = "provider";

  /** The Constant LOG. */
  protected static final Log              LOG            = ExoLogger.getLogger(OnlyOfficeNewDocumentEditorPlugin.class);

  /** The provider. */
  protected String                        provider;

  /** The editor service. */
  protected final OnlyofficeEditorService editorService;

  /**
   * Instantiates a new only office new document editor plugin.
   *
   * @param editorService the editor service
   * @param initParams the init params
   */
  public OnlyOfficeNewDocumentEditorPlugin(OnlyofficeEditorService editorService, InitParams initParams) {
    ValueParam providerParam = initParams.getValueParam(PROVIDER_PARAM);
    if (providerParam != null) {
      this.provider = providerParam.getValue();
    }
    this.editorService = editorService;
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
   * On document created.
   *
   * @param workspace the workspace
   * @param path the path
   * @throws Exception the exception
   */
  @Override
  public void onDocumentCreated(String workspace, String path) throws Exception {
    Node document = editorService.getDocument(workspace, path);
    LOG.debug("Opening editor page for document {}", document);
    String link = editorService.getEditorLink(document);
    if (link != null) {
      link = "'" + editorLink(link, "documents") + "'";
    } else {
      link = "null";
    }

    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    js.require("SHARED/onlyoffice", "onlyoffice").addScripts("onlyoffice.initEditorPage(" + link + ");");

  }

  /**
   * On document create.
   *
   * @param template the template
   * @param parentPath the parent path
   * @param title the title
   */
  @Override
  public void beforeDocumentCreate(DocumentTemplate template, String parentPath, String title) {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    js.require("SHARED/onlyoffice", "onlyoffice").addScripts("onlyoffice.initNewDocument();");
  }

}
