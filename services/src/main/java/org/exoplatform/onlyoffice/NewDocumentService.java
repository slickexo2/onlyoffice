package org.exoplatform.onlyoffice;

import java.util.Map;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NewDocumentService implements Startable {

  /** The Constant LOG. */
  protected static final Log      LOG = ExoLogger.getLogger(NewDocumentService.class);

  protected NewDocumentTypePlugin documentTypePlugin;

  @Override
  public void start() {
    // Nothing

  }

  @Override
  public void stop() {
    // Nothing

  }

  /**
   * NewDocumentTypesConfig.
   */
  public static class NewDocumentTypesConfig {

    /** The document types. */
    protected Map<String, String> types;

    /**
     * Gets the document types.
     *
     * @return the document types
     */
    public Map<String, String> getTypes() {
      return types;
    }

    /**
     * Sets the document types.
     *
     * @param mimeTypes the new document types
     */
    public void setTypes(Map<String, String> types) {
      this.types = types;
    }
  }

  public void addTypePlugin(ComponentPlugin plugin) {
    Class<NewDocumentTypePlugin> pclass = NewDocumentTypePlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      documentTypePlugin = pclass.cast(plugin);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Set newDocumentTypePlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The newDocumentTypePlugin plugin is not an instance of " + pclass.getName());
    }
  }

}
