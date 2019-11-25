package org.exoplatform.onlyoffice;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.*;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import java.util.*;

public class VersionNode {
  private static final Log LOG = ExoLogger.getLogger(org.exoplatform.ecm.jcr.model.VersionNode.class.getName());
  private static final String                                          EXO_LAST_MODIFIED_DATE = "exo:lastModifiedDate";
  private              List<VersionNode> children_              = new ArrayList();
  private            Calendar                                        createdTime_;
  private              String                                          name_ = "";
  private              String                                          displayName = "";
  private              String                                          path_ = "";
  private              String                                          ws_ = "";
  private              String                                          uuid_;
  private              String[]                                        versionLabels_ = new String[0];
  private              String                                          author_ = "";

  private void addVersions(Node node, Session session) throws RepositoryException {
    if (node instanceof Version) {
      Version version = (Version)node;
      this.versionLabels_ = version.getContainingHistory().getVersionLabels(version);
    } else {
      int maxVersion = 0;
      Map<String, String> mapVersionName = new HashMap();
      int maxIndex;
      if (node.isNodeType("mix:versionDisplayName")) {
        if (node.hasProperty("exo:maxVersion")) {
          maxVersion = (int)node.getProperty("exo:maxVersion").getLong();
        }

        if (node.hasProperty("exo:versionList")) {
          Value[] values = node.getProperty("exo:versionList").getValues();
          Value[] var6 = values;
          maxIndex = values.length;

          for(int var8 = 0; var8 < maxIndex; ++var8) {
            Value value = var6[var8];
            mapVersionName.put(value.getString().split(":")[0], value.getString().split(":")[1]);
          }
        }
      }

      Version rootVersion = node.getVersionHistory().getRootVersion();
      VersionIterator allVersions = node.getVersionHistory().getAllVersions();
      maxIndex = 0;

      while(allVersions.hasNext()) {
        Version version = allVersions.nextVersion();
        if (!version.getUUID().equals(rootVersion.getUUID())) {
          int versionIndex = Integer.parseInt(version.getName());
          maxIndex = Math.max(maxIndex, versionIndex);
          String versionOffset = (String)mapVersionName.get(version.getName());
          VersionNode versionNode = new VersionNode(version, session);
          if (versionOffset != null) {
            versionNode.setDisplayName(String.valueOf(Integer.parseInt(versionOffset) - 1));
          } else {
            versionNode.setDisplayName(String.valueOf(versionIndex - 1));
          }

          this.children_.add(versionNode);
        }
      }

      this.name_ = String.valueOf(maxIndex + 1);
      this.displayName = maxVersion > 0 ? String.valueOf(maxVersion - 1) : String.valueOf(maxIndex);
      this.versionLabels_ = node.getVersionHistory().getVersionLabels(rootVersion);
    }

  }


  public VersionNode(Node node, Session session) {
    Version version = node instanceof Version ? (Version)node : null;

    try {
      Property property = this.getProperty(node, "exo:lastModifiedDate");
      if (property != null) {
        this.createdTime_ = property.getDate();
      }

      this.name_ = version == null ? "" : version.getName();
      this.path_ = node.getPath();
      this.ws_ = node.getSession().getWorkspace().getName();
      this.uuid_ = node.getUUID();
      Property prop = this.getProperty(node, "exo:lastModifier");
      this.author_ = prop == null ? null : prop.getString();
      if (version == null) {
        if (node.isNodeType("mix:versionable")) {
          this.addVersions(node, session);
        }
      } else {
        this.addVersions(node, session);
      }
    } catch (Exception var6) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", var6);
      }
    }

  }

  private Property getProperty(Node node, String propName) throws RepositoryException {
    Property property = null;
    if (node.hasProperty(propName)) {
      property = node.getProperty(propName);
    } else if (node.hasNode("jcr:frozenNode") && node.getNode("jcr:frozenNode").hasProperty(propName)) {
      property = node.getNode("jcr:frozenNode").getProperty(propName);
    }

    return property;
  }

  public String getName() {
    return this.name_;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName_) {
    this.displayName = displayName_;
  }

  public String getWs() {
    return this.ws_;
  }

  public String getPath() {
    return this.path_;
  }

  public int getChildrenSize() {
    return this.children_.size();
  }

  public List<VersionNode> getChildren() {
    return this.children_;
  }

  public Calendar getCreatedTime() {
    return this.createdTime_;
  }

  public String getAuthor() {
    return this.author_;
  }

  public String[] getVersionLabels() {
    return this.versionLabels_;
  }
}
