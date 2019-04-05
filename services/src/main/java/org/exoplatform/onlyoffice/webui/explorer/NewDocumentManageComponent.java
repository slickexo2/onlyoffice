package org.exoplatform.onlyoffice.webui.explorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotCategoryFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotNtFileFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIDropDownControl;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

@ComponentConfigs({
    @ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
        @EventConfig(listeners = NewDocumentManageComponent.NewDocumentActionListener.class) }),
    @ComponentConfig(type = UIDropDownControl.class, 
    id = "DocumentTypesDropdown", 
    template = "system:/groovy/webui/core/UIDropDownControl.gtmpl",
    events = {
        @EventConfig(listeners = NewDocumentManageComponent.ChangeOptionActionListener.class)
      }
    ) 
})
public class NewDocumentManageComponent extends UIAbstractManagerComponent {

  /** The Constant LOG. */
  protected static final Log                   LOG     = ExoLogger.getLogger(NewDocumentManageComponent.class);

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new IsNotNtFileFilter(),
                                                                                                 new CanAddNodeFilter(), 
                                                                                                 new IsNotCategoryFilter(), 
                                                                                                 new IsNotLockedFilter(), 
                                                                                                 new IsCheckedOutFilter(),
                                                                                                 new IsNotTrashHomeNodeFilter(), 
                                                                                                 new IsNotInTrashFilter(), 
                                                                                                 new IsNotEditingDocumentFilter()
                                                                                                });

  public NewDocumentManageComponent() throws Exception {
    super();

    List<SelectItemOption<String>> documentTypes = new ArrayList<SelectItemOption<String>>(2);
    documentTypes.add(new SelectItemOption<String>("Word", "Word"));
    documentTypes.add(new SelectItemOption<String>("Excel", "Excel"));

    UIDropDownControl uiDropDownControl = null;

    uiDropDownControl = createUIComponent(UIDropDownControl.class, "DocumentTypesDropdown", null);

    uiDropDownControl.setOptions(documentTypes);

    uiDropDownControl.setParent(this);

  }

  /**
   * The listener interface for receiving onlyofficeOpenAction events. The class
   * that is interested in processing a onlyofficeOpenAction event implements
   * this interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addOnlyofficeOpenActionListener</code> method. When the
   * onlyofficeOpenAction event occurs, that object's appropriate method is
   * invoked.
   */
  public static class NewDocumentActionListener extends UIActionBarActionListener<NewDocumentManageComponent> {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<NewDocumentManageComponent> event) throws Exception {
      // This code will not be invoked
    }
  }

  /**
   * Gets the filters.
   *
   * @return the filters
   */
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    if (name.equals("NewDocument")) {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
      if (uiExplorer != null) {
        // TODO: implement
        return "javascript:console.log('Create a new document.')";
      } else {
        LOG.warn("Cannot find ancestor of type UIJCRExplorer in component " + this + ", parent: " + this.getParent());
      }
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

  public static class ChangeOptionActionListener extends EventListener<UIDropDownControl> {

    public void execute(Event<UIDropDownControl> event) throws Exception {
     // TODO : implement
     
   }
 }

}
