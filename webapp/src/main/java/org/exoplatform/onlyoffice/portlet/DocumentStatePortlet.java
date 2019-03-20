package org.exoplatform.onlyoffice.portlet;

import static org.exoplatform.onlyoffice.webui.OnlyofficeClientContext.callModule;

import javax.portlet.GenericPortlet;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.cometd.CometdInfo;
import org.exoplatform.onlyoffice.cometd.CometdOnlyofficeService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.application.WebuiRequestContext;

public class DocumentStatePortlet extends GenericPortlet {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(DocumentStatePortlet.class);

  /**
   * Renders the portlet on a page
   */
  @Override
  protected void doView(final RenderRequest request, final RenderResponse response) {
    WebuiRequestContext webuiContext = (WebuiRequestContext) WebuiRequestContext.getCurrentInstance();
    UIJCRExplorer explorer = webuiContext.getUIApplication().getApplicationComponent(UIJCRExplorer.class);
    if (explorer != null) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();

      CometdOnlyofficeService cometdService = container.getComponentInstanceOfType(CometdOnlyofficeService.class);
      OnlyofficeEditorService editorService = container.getComponentInstanceOfType(OnlyofficeEditorService.class);
      ConversationState convo = ConversationState.getCurrent();
      String userId = null;
      if (convo != null && convo.getIdentity() != null) {
        userId = convo.getIdentity().getUserId();
      }
      String cometdPath = cometdService.getCometdServerPath();
      String userToken = cometdService.getUserToken(userId);
      String containerName = PortalContainer.getCurrentPortalContainerName();
      String docId = null;

      try {
        editorService.initDocument(explorer.getCurrentNode());
      } catch (Exception e) {
        LOG.error("Couldn't init document of node. ", e);
      } 
        
      CometdInfo cometdInfo = new CometdInfo(userId, userToken, cometdPath, containerName, docId);
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      String cometdInfoJson = null;
      try {
        cometdInfoJson = ow.writeValueAsString(cometdInfo);
      } catch (JsonProcessingException e) {
        LOG.error("Couldn't create json from cometInfo object. ", e);
      }
      LOG.info("DOCUMENT STATE PORTLET CALLING JS");
      callModule("initExplorer(" + cometdInfoJson + ");");
    }
  }
}
