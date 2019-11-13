package org.exoplatform.onlyoffice.mock;


import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.exoplatform.services.jcr.RepositoryService;

  @SuppressWarnings("all")
  public class ExoContinuationBayeuxMock extends EXoContinuationBayeux {

    public ExoContinuationBayeuxMock(RepositoryService repoService) {
      super(repoService);
    }

    @Override
    public void dispose() {
    }

  }

