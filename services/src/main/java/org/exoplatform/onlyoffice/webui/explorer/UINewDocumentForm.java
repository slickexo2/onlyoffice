package org.exoplatform.onlyoffice.webui.explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/UINewDocument.gtmpl", events = {
    @EventConfig(listeners = UINewDocumentForm.SaveActionListener.class),
    @EventConfig(listeners = UINewDocumentForm.CancelActionListener.class, phase = Phase.DECODE) })
public class UINewDocumentForm extends UIForm implements UIPopupComponent {

  public static final String FIELD_TITLE_TEXT_BOX  = "titleTextBox";

  public static final String FIELD_TYPE_SELECT_BOX = "typeSelectBox";
  
  private static final String DEFAULT_NAME = "untitled";

  private static final Log   LOG                   = ExoLogger.getLogger(UINewDocumentForm.class.getName());

  private String             selectedType;

  /**
   * Constructor.
   *
   * @throws Exception
   */
  public UINewDocumentForm() throws Exception {
    // Title checkbox
    UIFormStringInput titleTextBox = new UIFormStringInput(FIELD_TITLE_TEXT_BOX, FIELD_TITLE_TEXT_BOX, null);
    this.addUIFormInput(titleTextBox);

    // Custom type selectbox
    UIFormSelectBox customTypeSelectBox = new UIFormSelectBox(FIELD_TYPE_SELECT_BOX, FIELD_TYPE_SELECT_BOX, null);
    customTypeSelectBox.setRendered(false);
    this.addUIFormInput(customTypeSelectBox);

    // Set action
    this.setActions(new String[] { "Save", "Cancel" });
  }

  static public class SaveActionListener extends EventListener<UINewDocumentForm> {
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
      if(StringUtils.isBlank(title)) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-invalid", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
        return;
      }
      
      // The name automatically determined from the title according to the current algorithm.
      String name = Text.escapeIllegalJcrChars(title);

      // Set default name if new title contain no valid character
      if (StringUtils.isEmpty(name)) {
        name = DEFAULT_NAME;
      }
      
      String selectedValue = typeSelectBox.getValue();
      uiDocumentForm.setSelectedType(selectedValue);
      
      // Add node
      Node addedNode = currentNode.addNode(name, Utils.NT_FILE);

      // Set title
      if (!addedNode.hasProperty(Utils.EXO_TITLE)) {
        addedNode.addMixin(Utils.EXO_RSS_ENABLE);
      }
      addedNode.setProperty(Utils.EXO_TITLE, title);
      
      addedNode.addNode("jcr:content", "nt:resource");
      
      // TODO: add data from existing template files in resources
      // set mimeType based on selectBox.
      addedNode.getNode("jcr:content").setProperty("jcr:data", "");
      addedNode.getNode("jcr:content").setProperty("jcr:mimeType", new MimeTypeResolver().getMimeType(title));
      addedNode.getNode("jcr:content").setProperty("jcr:lastModified", new GregorianCalendar());
      
      currentNode.save();

      uiExplorer.updateAjax(event);
    }
  }

  static public class CancelActionListener extends EventListener<UINewDocumentForm> {
    public void execute(Event<UINewDocumentForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  @Override
  public void activate() {
    try {
      UIFormSelectBox customTypeSelectBox = this.getUIFormSelectBox(FIELD_TYPE_SELECT_BOX);

      // TODO: refactor available file types
      List<String> documentTypes = new ArrayList<>();
      documentTypes.add("Word");
      documentTypes.add("Excel");
      documentTypes.add("PowerPoint");

      customTypeSelectBox.setRendered(true);
      fillCustomTypeSelectBox(documentTypes);

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e.getMessage());
      }
    }

  }

  private void fillCustomTypeSelectBox(List<String> documentTypes) throws Exception {
    UIFormSelectBox customTypeSelectBox = this.getUIFormSelectBox(FIELD_TYPE_SELECT_BOX);
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    for (String documentType : documentTypes) {
      String label = this.getLabel(documentType.replace(":", "_"));
      options.add(new SelectItemOption<String>(label, documentType));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    customTypeSelectBox.setOptions(options);

  }

  @Override
  public void deActivate() {
    // TODO Auto-generated method stub
  }

  /**
   * Get selected Document Type.
   *
   * @return the selectedType
   */
  public String getSelectedType() {
    return selectedType;
  }

  /**
   * Set selected document type.
   *
   * @param selectedType the selectedType to set
   */
  private void setSelectedType(String selectedType) {
    this.selectedType = selectedType;
  }

}
