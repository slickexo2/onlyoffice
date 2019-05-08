package org.exoplatform.onlyoffice.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.lock.LockService;

public class LockServiceMock implements LockService{

  @Override
  public List<String> getPreSettingLockList() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public HashMap<String, Map<String, String>> getLockHolding() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void putToLockHoding(String userId, Map<String, String> lockedNodesInfo) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Map<String, String> getLockInformation(String userId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeLocksOfUser(String userId) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void removeLocks() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getLockTokenOfUser(Node node) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String createLockKey(Node node) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String createLockKey(Node node, String userId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLockToken(Node node) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void changeLockToken(String srcPath, Node newNode) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void changeLockToken(Node oldNode, Node newNode) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getOldLockKey(String srcPath, Node node) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
