package org.exoplatform.onlyoffice;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.onlyoffice.cometd.CometdOnlyofficeService;
import org.exoplatform.onlyoffice.mock.ExoContinuationBayeuxMock;
import org.exoplatform.onlyoffice.test.AbstractResourceTest;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.junit.Test;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class CometdOnlyofficeServiceTest extends AbstractResourceTest {

  /** The Constant LOG. */
  protected static final Log        LOG        = ExoLogger.getLogger(CometdOnlyofficeServiceTest.class);

  protected CometdOnlyofficeService   cometdOnlyofficeService;

  protected ExoContinuationBayeuxMock   exoBayeux;

  protected OnlyofficeEditorService   onlyofficeEditorService;

  /**
   * Before class.
   */
  @Override
  public void beforeClass() {
    super.beforeClass();
    ExoContainerContext.setCurrentContainer(container);
    this.onlyofficeEditorService = getService(OnlyofficeEditorService.class);
    this.exoBayeux = getService(ExoContinuationBayeuxMock.class);
    this.cometdOnlyofficeService = getService(CometdOnlyofficeService.class);
  }

  /**
   * Test get cometd server path
   */
  @Test
  public void testgetCometdServerPath() {
    // When
    String cometdServerPath = cometdOnlyofficeService.getCometdServerPath();

    // Then
    assertEquals("/cometd/cometd", cometdServerPath);
  }

  /**
   * Test get user token
   */
  @Test
  public void testGetUserToken() {
    // When
    String userToken = cometdOnlyofficeService.getUserToken("john");

    // Then
    assertNotNull(userToken);
  }

}
