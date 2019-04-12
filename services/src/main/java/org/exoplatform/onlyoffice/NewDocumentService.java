package org.exoplatform.onlyoffice;

import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.resolver.ApplicationResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class NewDocumentService.
 */
public class NewDocumentService implements Startable {

  /** The Constant LOG. */
  protected static final Log      LOG          = ExoLogger.getLogger(NewDocumentService.class);

  /** The Constant DEFAULT_NAME. */
  private static final String     DEFAULT_NAME = "untitled";

  /** The document type plugin. */
  protected NewDocumentTypePlugin documentTypePlugin;

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  @Override
  public void start() {
    // Nothing

  }

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  @Override
  public void stop() {
    // Nothing
  }

  /**
   * Gets the types.
   *
   * @return the types
   */
  public List<NewDocumentType> getTypes() {
    return documentTypePlugin.getTypes();
  }

  /**
  * Adds the type plugin.
  *
  * @param plugin the plugin
  */
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

  /**
   * Creates the document.
   *
   * @param currentNode the current node
   * @param title the title
   * @param label the label
   * @throws Exception the exception
   */
  public void createDocument(Node currentNode, String title, String label) throws Exception {

    NewDocumentType selectedType = getDocumentTypeByLabel(label);

    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ApplicationResourceResolver appResolver = context.getApplication().getResourceResolver();
    ResourceResolver resolver = appResolver.getResourceResolver(selectedType.getPath());
    InputStream inputStream = resolver.getInputStream(selectedType.getPath());

    // Add node
    Node addedNode = currentNode.addNode(title, Utils.NT_FILE);

    // Set title
    if (!addedNode.hasProperty(Utils.EXO_TITLE)) {
      addedNode.addMixin(Utils.EXO_RSS_ENABLE);
    }
    addedNode.setProperty(Utils.EXO_TITLE, title);
    Node content = addedNode.addNode("jcr:content", "nt:resource");

    content.setProperty("jcr:data", inputStream);
    content.setProperty("jcr:mimeType", selectedType.getMimeType());
    content.setProperty("jcr:lastModified", new GregorianCalendar());
    currentNode.save();
  }

  /**
   * Gets the file name.
   *
   * @param title the title
   * @param label the label
   * @return the file name
   */
  public String getFileName(String title, String label) {
    NewDocumentType type = getDocumentTypeByLabel(label);
    title = Text.escapeIllegalJcrChars(title);
    if (StringUtils.isEmpty(title)) {
      title = DEFAULT_NAME;
    }
    String path = type.getPath();
    String extension = "";
    if (path.contains(".")) {
      extension = path.substring(path.lastIndexOf("."));
    }
    if (!title.endsWith(extension)) {
      title += extension;
    }
    return title;
  }

  /**
   * Gets the document type by label.
   *
   * @param label the label
   * @return the document type by label
   */
  public NewDocumentType getDocumentTypeByLabel(String label) {
    return getTypes().stream().filter(type -> label.equals(type.getLabel())).findAny().orElse(null);
  }
  
  /**
   * NewDocumentTypesConfig.
   */
  public static class NewDocumentTypesConfig {

    /** The document types. */
    protected List<NewDocumentType> types;

    /**
     * Gets the document types.
     *
     * @return the document types
     */
    public List<NewDocumentType> getTypes() {
      return types;
    }

    /**
     * Sets the document types.
     *
     * @param types the new types
     */
    public void setTypes(List<NewDocumentType> types) {
      this.types = types;
    }
  }

}
