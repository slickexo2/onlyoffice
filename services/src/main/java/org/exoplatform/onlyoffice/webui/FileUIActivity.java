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
package org.exoplatform.onlyoffice.webui;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.cometd.CometdInfo;
import org.exoplatform.onlyoffice.cometd.CometdOnlyofficeService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileUIActivity.java 00000 Feb 20, 2019 pnedonosko $
 */
@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/ecm/social-integration/plugin/space/FileUIActivity.gtmpl", events = {
        @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class) }), })
public class FileUIActivity extends org.exoplatform.wcm.ext.component.activity.FileUIActivity {

  /** The Constant LOG. */
  private static final Log                LOG = ExoLogger.getLogger(FileUIActivity.class);

  /** The editor service. */
  protected final OnlyofficeEditorService editorService;

  /**
   * Instantiates a new file UI activity with Edit Online button for office
   * documents.
   *
   * @throws Exception the exception
   */
  public FileUIActivity() throws Exception {
    super();
    this.editorService = this.getApplicationComponent(OnlyofficeEditorService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() throws Exception {
    CometdOnlyofficeService cometdService = this.getApplicationComponent(CometdOnlyofficeService.class);
    String userId = Utils.getViewerIdentity().getId();
    
    CometdInfo cometdInfo = new CometdInfo();
    cometdInfo.setUser(userId);
    cometdInfo.setCometdPath(cometdService.getCometdServerPath());
    cometdInfo.setUserToken(cometdService.getUserToken(userId));
    cometdInfo.setContainer(PortalContainer.getCurrentPortalContainerName());
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    ResourceBundle resourceBundle = requestContext.getApplicationResourceBundle();

    String editLabel = resourceBundle.getString("UIActionBar.tooltip.OnlyofficeOpen");

    Map<Node, String> editorLinks = new HashMap<>();

    if (getFilesCount() == 1) {
      Node node = getContentNode(0);
      if (node != null) {
        editorLinks.computeIfAbsent(node, n -> {
          try {
            return editorService.getEditorLink(n);
          } catch (OnlyofficeEditorException | RepositoryException e) {
            LOG.error(e);
            return null;
          }
        });
        String editorLink = editorLinks.get(node);
        cometdInfo.setDocId(node.getUUID());
        String cometdInfoJson = ow.writeValueAsString(cometdInfo);
        js.require("SHARED/onlyoffice", "onlyoffice")
          .addScripts("onlyoffice.initActivity(" + cometdInfoJson + ", '" + getActivity().getId() + "','" + editorLink + "', '" + editLabel
                + "');");
      }
    }

    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      Node node = getContentNode(index);
      if (node != null) {
        editorLinks.computeIfAbsent(node, n -> {
          try {
            return editorService.getEditorLink(n);
          } catch (OnlyofficeEditorException | RepositoryException e) {
            LOG.error(e);
            return null;
          }
        });
        String editorLink = editorLinks.get(node);
        if (editorLink != null) {
          js.require("SHARED/onlyoffice", "onlyoffice")
            .addScripts("onlyoffice.initPreview('" + getActivity().getId() + "','" + editorLink + "','" + index + "', '"
                + editLabel + "');");
        }
      }
    }
    super.end();
  }

}
