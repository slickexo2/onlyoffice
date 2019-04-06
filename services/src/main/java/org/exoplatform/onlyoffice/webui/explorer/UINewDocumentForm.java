package org.exoplatform.onlyoffice.webui.explorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIFolderForm;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/groovy/webui/component/explorer/popup/action/UINewDocument.gtmpl", events = {
    @EventConfig(listeners = UINewDocumentForm.SaveActionListener.class),
    @EventConfig(listeners = UINewDocumentForm.OnChangeActionListener.class),
    @EventConfig(listeners = UINewDocumentForm.CancelActionListener.class, phase = Phase.DECODE) })
public class UINewDocumentForm extends UIForm implements UIPopupComponent {

  public static final String FIELD_TITLE_TEXT_BOX         = "titleTextBox";

  public static final String FIELD_CUSTOM_TYPE_SELECT_BOX = "customTypeSelectBox";

  private static final Log   LOG                          = ExoLogger.getLogger(UINewDocumentForm.class.getName());

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
    UIFormSelectBox customTypeSelectBox = new UIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX, FIELD_CUSTOM_TYPE_SELECT_BOX, null);
    customTypeSelectBox.setRendered(false);
    this.addUIFormInput(customTypeSelectBox);

    // Set action
    this.setActions(new String[] { "Save", "Cancel" });
  }

  public static class OnChangeActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      // TODO: Implement
    }
  }

  static public class SaveActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      // TODO: Implement
    }
  }

  static public class CancelActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  @Override
  public void activate() {
    try {
      UIFormSelectBox customTypeSelectBox = this.getUIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX);

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
    UIFormSelectBox customTypeSelectBox = this.getUIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX);
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
