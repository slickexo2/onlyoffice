package org.exoplatform.onlyoffice;
import java.io.IOException;

import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.onlyoffice.rest.EditorService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class EditorServiceTest  extends BaseCommonsTestCase {
  
  EditorService editorService;
  
  @Override
  protected void beforeClass() {
    // this.setForceContainerReload(true); // We don't want reload container!
    super.beforeClass();

    this.container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    editorService = getService(EditorService.class);
  }
  @Test
  public void testREST() throws IOException {
    OnlyofficeEditorService editor = getService(OnlyofficeEditorService.class);
    editorService = getService(EditorService.class);
    System.out.println("EDITOR SERVICE " + editorService);
    System.out.println("EDIRODS " + editor);
  }
}
