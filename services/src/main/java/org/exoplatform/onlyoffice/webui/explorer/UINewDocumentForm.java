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
package org.exoplatform.onlyoffice.webui.explorer;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.documents.NewDocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * The Class UINewDocumentForm.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/UINewDocument.gtmpl", events = {
    @EventConfig(listeners = UINewDocumentForm.SaveActionListener.class),
    @EventConfig(listeners = UINewDocumentForm.CancelActionListener.class, phase = Phase.DECODE) })
public class UINewDocumentForm extends UIForm implements UIPopupComponent {

  /** The Constant FIELD_TITLE_TEXT_BOX. */
  public static final String   FIELD_TITLE_TEXT_BOX  = "titleTextBox";

  /** The Constant FIELD_TYPE_SELECT_BOX. */
  public static final String   FIELD_TYPE_SELECT_BOX = "typeSelectBox";

  /** The Constant LOG. */
  protected static final Log   LOG                   = ExoLogger.getLogger(UINewDocumentForm.class.getName());

  /** The document service. */
  protected NewDocumentService documentService;

  /**
   * Constructor.
   *
   */
  public UINewDocumentForm() {
    this.documentService = this.getApplicationComponent(NewDocumentService.class);
    // Title textbox
    UIFormStringInput titleTextBox = new UIFormStringInput(FIELD_TITLE_TEXT_BOX, FIELD_TITLE_TEXT_BOX, null);
    this.addUIFormInput(titleTextBox);

    // Type selectbox
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    documentService.getTypes().forEach(elem -> {
      options.add(new SelectItemOption<String>(elem.getLabel(), elem.getLabel()));
    });

    UIFormSelectBox typeSelectBox = new UIFormSelectBox(FIELD_TYPE_SELECT_BOX, FIELD_TYPE_SELECT_BOX, options);
    typeSelectBox.setRendered(true);
    this.addUIFormInput(typeSelectBox);

    // Set action
    this.setActions(new String[] { "Save", "Cancel" });
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class SaveActionListener extends EventListener<UINewDocumentForm> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<UINewDocumentForm> event) throws Exception {
      UINewDocumentForm uiDocumentForm = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiDocumentForm.getAncestorOfType(UIApplication.class);
      UIFormSelectBox typeSelectBox = uiDocumentForm.getUIFormSelectBox(FIELD_TYPE_SELECT_BOX);

      // Get title
      String title = uiDocumentForm.getUIStringInput(FIELD_TITLE_TEXT_BOX).getValue();

      // Validate input
      Node currentNode = uiExplorer.getCurrentNode();
      if (uiExplorer.nodeIsLocked(currentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
        return;
      }
      if (StringUtils.isBlank(title)) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.name-invalid", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
        return;
      }

      // It is used to check sameNameSiblings in case when the
      // RepositoryException is thrown
      title = uiDocumentForm.documentService.getFileName(title, typeSelectBox.getValue());
      Node createdDocument = null;
      try {
        createdDocument = uiDocumentForm.documentService.createDocument(currentNode, title, typeSelectBox.getValue());
      } catch (ConstraintViolationException cve) {
        Object[] arg = { typeSelectBox.getValue() };
        throw new MessageException(new ApplicationMessage("UINewDocumentForm.msg.constraint-violation",
                                                          arg,
                                                          ApplicationMessage.WARNING));
      } catch (AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.repository-exception-permission",
                                                null,
                                                ApplicationMessage.WARNING));
      } catch (ItemExistsException re) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.not-allow-sameNameSibling",
                                                null,
                                                ApplicationMessage.WARNING));
      } catch (RepositoryException re) {
        String key = "UINewDocumentForm.msg.repository-exception";
        NodeDefinition[] definitions = currentNode.getPrimaryNodeType().getChildNodeDefinitions();
        boolean isSameNameSiblingsAllowed = false;
        for (NodeDefinition def : definitions) {
          if (def.allowsSameNameSiblings()) {
            isSameNameSiblingsAllowed = true;
            break;
          }
        }
        if (currentNode.hasNode(title) && !isSameNameSiblingsAllowed) {
          key = "UINewDocumentForm.msg.not-allow-sameNameSibling";
        }
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
      } catch (NumberFormatException nume) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.numberformat-exception",
                                                null,
                                                ApplicationMessage.WARNING));
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
      }

      OnlyofficeEditorService editorService = ExoContainerContext.getCurrentContainer()
                                                                 .getComponentInstanceOfType(OnlyofficeEditorService.class);
      String link = editorService.getEditorLink(createdDocument);
      link = link != null ? "'" + link + "'" : "null";

      WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
      JavascriptManager js = requestContext.getJavascriptManager();
      js.require("SHARED/onlyoffice", "onlyoffice").addScripts("onlyoffice.initEditorPage(" + link + ");");

      uiExplorer.updateAjax(event);

    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class CancelActionListener extends EventListener<UINewDocumentForm> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<UINewDocumentForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  /**
   * Activate.
   */
  @Override
  public void activate() {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    js.require("SHARED/onlyoffice", "onlyoffice").addScripts("onlyoffice.initNewDocument();");
  }

  /**
   * De activate.
   */
  @Override
  public void deActivate() {
    // Nothing
  }
}
