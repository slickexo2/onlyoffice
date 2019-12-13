package org.exoplatform.onlyoffice.documents;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.editorLink;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.ecm.webui.component.explorer.documents.NewDocumentEditorPlugin;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class OnlyOfficeNewDocumentEditorPlugin.
 */
public class OnlyOfficeNewDocumentEditorPlugin extends BaseComponentPlugin implements NewDocumentEditorPlugin {

  /** The Constant PROVIDER_PARAM. */
  protected static final String PROVIDER_PARAM = "provider";

  /** The Constant LOG. */
  protected static final Log    LOG            = ExoLogger.getLogger(OnlyOfficeNewDocumentEditorPlugin.class);

  /** The provider. */
  protected String              provider;

  /**
   * Instantiates a new only office new document editor plugin.
   *
   * @param initParams the init params
   */
  public OnlyOfficeNewDocumentEditorPlugin(InitParams initParams) {
    ValueParam providerParam = initParams.getValueParam(PROVIDER_PARAM);
    if (providerParam != null) {
      this.provider = providerParam.getValue();
    }
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
   * @param node the node
   * @throws Exception the exception
   */
  @Override
  public void onDocumentCreated(Node node) throws Exception {
    LOG.info("On Document Created Invoked {}", node);
    OnlyofficeEditorService editorService = ExoContainerContext.getCurrentContainer()
                                                               .getComponentInstanceOfType(OnlyofficeEditorService.class);
    String link = editorService.getEditorLink(node);
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
   */
  @Override
  public void beforeDocumentCreate() {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    js.require("SHARED/onlyoffice", "onlyoffice").addScripts("onlyoffice.initNewDocument();");
  }

}
