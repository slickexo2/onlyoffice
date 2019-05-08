package org.exoplatform.onlyoffice.mock;

import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.drives.DriveData;

public class DocumentServiceMock implements DocumentService {

  @Override
  public Document findDocById(String id) throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getShortLinkInDocumentsApp(String workspaceName, String nodeId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLinkInDocumentsApp(String nodePath) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLinkInDocumentsApp(String nodePath, DriveData drive) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DriveData getDriveOfNode(String nodePath) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DriveData getDriveOfNode(String nodePath, String userId, List<String> memberships) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
