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

import javax.jcr.Node;

import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

/**
 * Created by The eXo Platform SAS
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
    // In Javascript we can work on DOM element with this ID (see in
    // FileUIActivity.gtmpl), use it in jQuery: #activityContainer${activity.id}

    // TODO add javascript/markup to initialize this activity Edit Online
    // button and preview at end of the activity form rendering (see end of
    // integration's FileUIActivity.gtmpl)
    if (getFilesCount() == 1) {
      Node node = getContentNode(0);
      if (node != null) {
        String editorLink = editorService.getEditorLink(node);
        if (editorLink != null) {
          // TODO init Edit Online button for this activity in .statusAction of
          // activity elem
        }
      }
    }

    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      // TODO We want init all preview links that template render as:
      // #Preview${activity.id}-$index",
      Node node = getContentNode(index);
      if (node != null) {
        String editorLink = editorService.getEditorLink(node);
        if (editorLink != null) {
          // TODO init the preview onclick to add Edit Online button for this
          // doc preview when the preview will be clicked by user.
          // This click handler should wait until the preview will load and
          // render the DOM itself, only then add the button
        }
      }
    }
    super.end();
  }

}
