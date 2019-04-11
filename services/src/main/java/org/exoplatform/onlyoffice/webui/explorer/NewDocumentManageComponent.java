package org.exoplatform.onlyoffice.webui.explorer;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotNtFileFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * The Class NewDocumentManageComponent.
 */
@ComponentConfig(events = { @EventConfig(listeners = NewDocumentManageComponent.NewDocumentActionListener.class) })
public class NewDocumentManageComponent extends UIAbstractManagerComponent {

  /** The Constant LOG. */
  protected static final Log                   LOG     = ExoLogger.getLogger(NewDocumentManageComponent.class);

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new IsNotNtFileFilter(),
                                                                                                 new CanAddNodeFilter(),
                                                                                                 new IsNotLockedFilter(),
                                                                                                 new IsCheckedOutFilter(),
                                                                                                 new IsNotTrashHomeNodeFilter(),
                                                                                                 new IsNotInTrashFilter(),
                                                                                                 new IsNotEditingDocumentFilter()
                                                                                                });
  
  /**
   * The listener interface for receiving newDocumentAction events. The class
   * that is interested in processing a newDocumentAction event implements
   * this interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addnewDocumentActionListener</code> method. When the
   * newDocumentAction event occurs, that object's appropriate method is
   * invoked.
   *
   * @see NewDocumentActionEvent
   */
  public static class NewDocumentActionListener extends UIActionBarActionListener<NewDocumentManageComponent> {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<NewDocumentManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      addDocument(event, uiExplorer);
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
   * Adds the document.
   *
   * @param event the event
   * @param uiExplorer the ui explorer
   * @throws Exception the exception
   */
  public static void addDocument(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer) throws Exception {
    UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
    UINewDocumentForm documentForm = uiExplorer.createUIComponent(UINewDocumentForm.class, null, null);
    UIPopupContainer.activate(documentForm, 530, 220, false);
    event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
