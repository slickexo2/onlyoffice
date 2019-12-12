package org.exoplatform.onlyoffice.documents;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.editorLink;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.ecm.webui.component.explorer.documents.NewDocumentEditorPlugin;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;

public class OnlyOfficeNewDocumentEditorPlugin extends BaseComponentPlugin implements NewDocumentEditorPlugin {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(OnlyOfficeNewDocumentEditorPlugin.class);

  @Override
  public String getProvider() {
    return name;
  }

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

  @Override
  public void onDocumentCreate() {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    js.require("SHARED/onlyoffice", "onlyoffice").addScripts("onlyoffice.initNewDocument();");
  }

}
