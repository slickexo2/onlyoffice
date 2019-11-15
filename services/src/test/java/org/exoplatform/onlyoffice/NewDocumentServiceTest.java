package org.exoplatform.onlyoffice;

import java.util.List;

import org.junit.Test;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.onlyoffice.documents.NewDocumentService;
import org.exoplatform.onlyoffice.documents.NewDocumentType;
import org.exoplatform.onlyoffice.test.AbstractResourceTest;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class NewDocumentServiceTest extends AbstractResourceTest {

  protected NewDocumentService newDocumentService;

  /**
   * Before class.
   */
  @Override
  public void beforeClass() {
    super.beforeClass();
    ExoContainerContext.setCurrentContainer(container);
    this.newDocumentService = getService(NewDocumentService.class);
  }

  /**
   * Test get document type
   */
  @Test
  public void testGetDocumentType() throws Exception {

    // When
    List<NewDocumentType> DocumentType = newDocumentService.getTypes();

    // Then
    assertNotNull(DocumentType);
    assertEquals(3, DocumentType.size());
    assertEquals("MicrosoftOfficeDocument", DocumentType.get(0).getLabel());
    assertEquals("classpath:files/template.docx", DocumentType.get(0).getPath());
    assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentType.get(0).getMimeType());
    assertEquals("MicrosoftOfficeSpreadsheet", DocumentType.get(1).getLabel());
    assertEquals("classpath:files/template.xlsx", DocumentType.get(1).getPath());
    assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", DocumentType.get(1).getMimeType());
    assertEquals("MicrosoftOfficePresentation", DocumentType.get(2).getLabel());
    assertEquals("classpath:files/template.pptx", DocumentType.get(2).getPath());
    assertEquals("application/vnd.openxmlformats-officedocument.presentationml.presentation", DocumentType.get(2).getMimeType());
  }

  /**
   * Test get file name
   */
  @Test
  public void testGetFileName() throws Exception {

    // Given
    List<NewDocumentType> DocumentType = newDocumentService.getTypes();
    String labelDocx = DocumentType.get(0).getLabel();
    String labelPptx = DocumentType.get(2).getLabel();
    String labelXlsx = DocumentType.get(1).getLabel();

    // Then
    String fileNameDocx = newDocumentService.getFileName("title", labelDocx);
    String fileNamePptx = newDocumentService.getFileName("title", labelPptx);
    String fileNameXlsx = newDocumentService.getFileName("title", labelXlsx);

    // When
    assertNotNull(fileNameDocx);
    assertEquals("title.docx", fileNameDocx);
    assertNotNull(fileNamePptx);
    assertEquals("title.pptx", fileNamePptx);
    assertNotNull(fileNameXlsx);
    assertEquals("title.xlsx", fileNameXlsx);
  }
}
