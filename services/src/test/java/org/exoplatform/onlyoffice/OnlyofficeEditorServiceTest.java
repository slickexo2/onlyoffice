package org.exoplatform.onlyoffice;

import java.util.Calendar;
import java.util.HashSet;

import javax.jcr.Node;

import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class OnlyofficeEditorServiceTest extends BaseCommonsTestCase {

  /** The Constant LOG. */
  protected static final Log    LOG                    = ExoLogger.getLogger(OnlyofficeEditorServiceTest.class);
  
  protected OnlyofficeEditorService editorService;
  protected SessionProviderService sessionProviderService;

  /**
   * Before class.
   */
  @Override
  public void beforeClass() {
    super.beforeClass();
    ExoContainerContext.setCurrentContainer(container);
    this.editorService = getContainer().getComponentInstanceOfType(OnlyofficeEditorService.class);
    this.sessionProviderService = getContainer().getComponentInstanceOfType(SessionProviderService.class);
  }

  @Test
  public void testCreateEditor() throws Exception {
    startSessionAs("john");
    Node node = session.getRootNode().addNode("Test Document.docx", "nt:file");
    node.addMixin("mix:referenceable");
    Node contentNode = node.addNode("jcr:content", "nt:unstructured");
    contentNode.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
    contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
    contentNode.setProperty("jcr:data", "testContent");
    session.save();
    
    String docId = node.getUUID();
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, docId);
    String editorURL = "http://127.0.0.1:8080/portal/intranet/oeditor?docId=" + docId;
    
    assertNotNull(config);
    
    assertTrue(config.isCreated());
    assertFalse(config.isClosing());
    assertFalse(config.isOpen());
    assertFalse(config.isClosed());
    assertNull(config.getError());
    assertEquals(docId, config.getDocId());
    assertEquals(editorURL, config.getEditorUrl());
    assertEquals("/Test Document.docx", config.getPath());
    
    assertNotNull(config.getDocument());
    assertEquals("Test Document.docx", config.getDocument().getTitle());
    assertEquals("docx", config.getDocument().getFileType());
    assertEquals("john", config.getDocument().getInfo().getAuthor());
    
    assertNotNull(config.getEditorConfig());
    assertNotNull(config.getEditorConfig().getUser());
    assertEquals("John", config.getEditorConfig().getUser().getFirstname());
    assertEquals("Smith", config.getEditorConfig().getUser().getLastname());
  }
  
  @Test(expected = OnlyofficeEditorException.class)
  public void testCreateEditorIncorrectFile() throws Exception {
    startSessionAs("john");
    Node node = session.getRootNode().addNode("Test Document.docx", "nt:base");
    node.addMixin("mix:referenceable");
    session.save();
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());  
  }

  protected void startSessionAs(String user) {
    HashSet<MembershipEntry> memberships = new HashSet<MembershipEntry>();
    memberships.add(new MembershipEntry("/platform/administrators"));
    Identity identity = new Identity(user, memberships);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
    sessionProviderService.setSessionProvider(null, new SessionProvider(state));
  }

}
