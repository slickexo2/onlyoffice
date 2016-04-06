/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.onlyoffice.rest;

import org.exoplatform.onlyoffice.BadParameterException;
import org.exoplatform.onlyoffice.ChangeState;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.DocumentContent;
import org.exoplatform.onlyoffice.DocumentStatus;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.security.RolesAllowed;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 * REST service implementing Onlyoffice config storage service. <br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: EditorService.java 00000 Feb 12, 2016 pnedonosko $
 */
@Path("/onlyoffice/editor")
public class EditorService implements ResourceContainer {

  public static final String INIT_COOKIE_PATH = "/portal/rest/onlyoffice/editor";

  protected static final Log LOG              = ExoLogger.getLogger(EditorService.class);

  /**
   * Response builder for connect and state.
   */
  class EditorResponse extends ServiceResponse {
    Config config;

    String error;

    EditorResponse config(Config config) {
      this.config = config;
      return this;
    }

    EditorResponse error(String error) {
      this.error = error;
      return this;
    }

    EditorResponse error(String error, String host) {
      this.error = error;
      return this;
    }

    /**
     * @inherritDoc
     */
    @Override
    Response build() {
      if (config != null) {
        super.entity(config);
      } else if (error != null) {
        super.entity("{\"error\":\"" + error + "\"}");
      }
      return super.build();
    }
  }

  protected final OnlyofficeEditorService editors;

  protected final DriveServiceLocator     locator;

  protected final Map<UUID, Config>       initiated = new ConcurrentHashMap<UUID, Config>();

  /**
   * REST cloudDrives uses {@link OnlyofficeEditorService} for actual job.
   * 
   * @param editors
   * @param locator
   * @param jcrService
   * @param sessionProviders
   * @param finder
   */
  public EditorService(OnlyofficeEditorService editors, DriveServiceLocator locator) {
    this.editors = editors;
    this.locator = locator;
  }

  /**
   * Config status posted from the editor. WARNING! It is publicly accessible service as Onlyoffice will
   * access it
   * from the Documents Server.
   * 
   * @param uriInfo - request info
   * @param key - config key generated when requested editor config
   * @param status DocumentStatus
   * @return {@link Response}
   */
  @POST
  @Path("/status/{userId}/{key}")
  // @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@Context UriInfo uriInfo,
                         @PathParam("userId") String userId,
                         @PathParam("key") String key,
                         String statusText) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice document status: " + userId + "@" + key + " " + statusText);
    }

    EditorResponse resp = new EditorResponse();

    try {
      DocumentStatus status = new DocumentStatus();

      JSONParser parser = new JSONParser();
      Object obj = parser.parse(statusText);
      JSONObject jsonObj = (JSONObject) obj;
      String statusKey = (String) jsonObj.get("key");
      long statusCode = (long) jsonObj.get("status");
      String statusUrl = (String) jsonObj.get("url");
      JSONArray statusUsersArray = (JSONArray) jsonObj.get("users");
      String[] statusUsers = new String[statusUsersArray.size()];
      statusUsersArray.toArray(statusUsers);

      if (key != null && key.length() > 0) {
        if (userId != null && userId.length() > 0) {
          status.setKey(statusKey != null && statusKey.length() > 0 ? statusKey : key);
          status.setStatus(statusCode);
          status.setUrl(statusUrl);
          status.setUsers(statusUsers);
          
          try {
            editors.updateDocument(userId, status);
            resp.entity("{\"error\": 0}");
          } catch (BadParameterException e) {
            LOG.error("Bad parameter to update status for " + key, e);
            resp.error(e.getMessage()).status(Status.BAD_REQUEST);
          } catch (OnlyofficeEditorException e) {
            LOG.error("Error handling status for " + key, e);
            resp.error("Error handling status. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
          } catch (RepositoryException e) {
            LOG.error("Storage error while handling status for " + key, e);
            resp.error("Storage error.").status(Status.INTERNAL_SERVER_ERROR);
          } catch (Throwable e) {
            LOG.error("Runtime error while handling status for " + key, e);
            resp.error("Runtime error.").status(Status.INTERNAL_SERVER_ERROR);
          }
        } else {
          LOG.error("Error processing editor status. User not provided");
          resp.error("User not provided").status(Status.BAD_REQUEST);
        }
      } else {
        resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
      }
    } catch (ParseException e) {
      LOG.error("JSON parse error while handling status for " + key + ". JSON: " + statusText, e);
      resp.error("JSON parse error: " + e.getMessage()).status(Status.BAD_REQUEST);
    }
    return resp.build();
  }

  /**
   * Document content download link. WARNING! It is publicly accessible service as Onlyoffice will access it
   * from the Documents Server.
   * 
   * @param uriInfo - request info
   * @param key - file key generated by /config method
   * @return {@link Response}
   */
  @GET
  @Path("/content/{userId}/{key}")
  public Response content(@Context UriInfo uriInfo, @PathParam("userId") String userId, @PathParam("key") String key) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice document content: " + userId + "@" + key);
    }

    EditorResponse resp = new EditorResponse();
    if (key != null && key.length() > 0) {
      try {
        if (userId != null && userId.length() > 0) {
          DocumentContent content = editors.getContent(userId, key);
          resp.entity(content.getData()).type(content.getType()).ok();
        } else {
          LOG.error("Error downloading content. User identity not provided");
          resp.error("User not provided").status(Status.BAD_REQUEST);
        }
      } catch (BadParameterException e) {
        LOG.error("Bad parameter to downloading content for " + key, e);
        resp.error(e.getMessage()).status(Status.BAD_REQUEST);
      } catch (OnlyofficeEditorException e) {
        LOG.error("Error downloading content for " + key, e);
        resp.error("Error downloading content. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
      } catch (RepositoryException e) {
        LOG.error("Storage error while downloading content for " + key, e);
        resp.error("Storage error.").status(Status.INTERNAL_SERVER_ERROR);
      } catch (Throwable e) {
        LOG.error("Runtime error while downloading content for " + key, e);
        resp.error("Runtime error.").status(Status.INTERNAL_SERVER_ERROR);
      }
    } else {
      resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
    }
    return resp.build();
  }

  /**
   * Config configuration for Onlyoffice JS.
   * 
   * @param uriInfo - request with base URI
   * @param providerId - provider name see more in {@link CloudProvider}
   * @return response with
   */
  @GET
  @Path("/config/{workspace}/{path:.*}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response config(@Context UriInfo uriInfo,
                         @PathParam("workspace") String workspace,
                         @PathParam("path") String path) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice config: " + workspace + ":" + path);
    }

    EditorResponse resp = new EditorResponse();
    if (workspace != null) {
      if (path != null) {
        if (!path.startsWith("/")) {
          path = "/" + path;
        }
        try {
          ConversationState convo = ConversationState.getCurrent();
          if (convo != null) {
            String username = convo.getIdentity().getUserId();
            // String host = locator.getServiceHost(uriInfo.getRequestUri().getHost());
            // initiated.put(initId, new UserInit(localUser, host));

            Config config = editors.createEditor(username, workspace, path);
            if (LOG.isDebugEnabled()) {
              LOG.debug("> Onlyoffice document config: " + workspace + ":" + path + " -> " + config.getDocument().getKey());
            }
            resp.config(config).ok();
          } else {
            LOG.warn("ConversationState not set to create editor config");
            resp.error("User not authenticated").status(Status.UNAUTHORIZED);
          }
        } catch (BadParameterException e) {
          LOG.error("Bad parameter for creating editor config " + workspace + ":" + path, e);
          resp.error(e.getMessage()).status(Status.BAD_REQUEST);
        } catch (OnlyofficeEditorException e) {
          LOG.error("Error creating editor config " + workspace + ":" + path, e);
          resp.error("Error creating editor config. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
        } catch (RepositoryException e) {
          LOG.error("Storage error while creating editor config " + workspace + ":" + path, e);
          resp.error("Storage error.").status(Status.INTERNAL_SERVER_ERROR);
        } catch (Throwable e) {
          LOG.error("Runtime error while creating editor config " + workspace + ":" + path, e);
          resp.error("Error creating editor config.").status(Status.INTERNAL_SERVER_ERROR);
        }
      } else {
        resp.status(Status.BAD_REQUEST).error("Null path.");
      }
    } else {
      resp.status(Status.BAD_REQUEST).error("Null workspace.");
    }
    return resp.build();
  }

  /**
   * Editing document state in local storage.
   * 
   * @param uriInfo - request info
   * @param key - config key generated when requested editor config
   * @param status DocumentStatus
   * @return {@link Response}
   */
  @GET
  @Path("/state/{userId}/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response localState(@Context UriInfo uriInfo, @PathParam("userId") String userId, @PathParam("key") String key) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("> localState: " + userId + "@" + key);
    }

    EditorResponse resp = new EditorResponse();
    if (userId != null) {
      if (key != null) {
        try {
          ChangeState status = editors.getState(userId, key);
          resp.entity(status).ok();
        } catch (BadParameterException e) {
          LOG.error("Bad parameter for getting document state " + userId + "@" + key, e);
          resp.error(e.getMessage()).status(Status.BAD_REQUEST);
        } catch (OnlyofficeEditorException e) {
          LOG.error("Error getting document state " + userId + "@" + key, e);
          resp.error("Error getting document state. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
        } catch (Throwable e) {
          LOG.error("Runtime error while getting document state " + userId + "@" + key, e);
          resp.error("Error getting document state.").status(Status.INTERNAL_SERVER_ERROR);
        }
      } else {
        resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
      }
    } else {
      LOG.error("Error getting document state. User identity not provided");
      resp.error("User not provided").status(Status.BAD_REQUEST);
    }

    return resp.build();
  }

}
