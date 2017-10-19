/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

import java.net.InetAddress;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.security.RolesAllowed;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
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

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(EditorService.class);

  /**
   * Response builder for connect and state.
   */
  class EditorResponse extends ServiceResponse {

    /** The config. */
    Config config;

    /** The error. */
    String error;

    /**
     * Config.
     *
     * @param config the config
     * @return the editor response
     */
    EditorResponse config(Config config) {
      this.config = config;
      return this;
    }

    /**
     * Error.
     *
     * @param error the error
     * @return the editor response
     */
    EditorResponse error(String error) {
      this.error = error;
      return this;
    }

    /**
     * Error.
     *
     * @param error the error
     * @param host the host
     * @return the editor response
     */
    EditorResponse error(String error, String host) {
      this.error = error;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the response
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

  /** The editors. */
  protected final OnlyofficeEditorService editors;

  /** The initiated. */
  protected final Map<UUID, Config>       initiated = new ConcurrentHashMap<UUID, Config>();

  /**
   * REST cloudDrives uses {@link OnlyofficeEditorService} for actual job.
   *
   * @param editors the editors
   */
  public EditorService(OnlyofficeEditorService editors) {
    this.editors = editors;
  }

  /**
   * Config status posted from the Document Server. <br>
   * WARNING! It is publicly accessible service but access from the Documents Server host can be restricted
   * (by default).
   *
   * @param uriInfo - request info
   * @param request the request
   * @param userId the user id
   * @param key - config key generated when requested editor config
   * @param statusText the status text
   * @return {@link Response}
   */
  @POST
  @Path("/status/{userId}/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@Context UriInfo uriInfo,
                         @Context HttpServletRequest request,
                         @PathParam("userId") String userId,
                         @PathParam("key") String key,
                         String statusText) {

    String clientHost = getClientHost(request);
    String clientIp = getClientIpAddr(request);

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice document status: " + userId + "@" + key + " " + statusText + " from " + clientHost + "(" + clientIp + ")");
    }

    EditorResponse resp = new EditorResponse();
    if (editors.canDownloadBy(clientHost) || editors.canDownloadBy(clientIp)) {
      try {
        DocumentStatus status = new DocumentStatus();

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(statusText);
        JSONObject jsonObj = (JSONObject) obj;
        String statusKey = (String) jsonObj.get("key");
        long statusCode = (long) jsonObj.get("status");
        String statusUrl = (String) jsonObj.get("url");
        // Oct 2017: When Document server calls with status 4 (user closed w/o modification), the users array
        // will be null
        JSONArray statusUsersArray = (JSONArray) jsonObj.get("users");
        @SuppressWarnings("unchecked")
        String[] statusUsers = statusUsersArray != null ? (String[]) statusUsersArray.toArray(new String[statusUsersArray.size()]) : new String[0];

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
              LOG.warn("Bad parameter to update status for " + key + ". " + e.getMessage());
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
            LOG.warn("Error processing editor status. User not provided");
            resp.error("User not provided").status(Status.BAD_REQUEST);
          }
        } else {
          resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
        }
      } catch (ParseException e) {
        LOG.warn("JSON parse error while handling status for " + key + ". JSON: " + statusText, e);
        resp.error("JSON parse error: " + e.getMessage()).status(Status.BAD_REQUEST);
      }
    } else {
      LOG.warn("Attempt to update status by not allowed host: " + clientHost + "(" + clientIp + ")");
      resp.error("Not a document server").status(Status.UNAUTHORIZED);
    }
    return resp.build();
  }

  /**
   * Document content download link. <br>
   * WARNING! It is publicly accessible service but access from the Documents Server host can be restricted
   * (by default).
   *
   * @param uriInfo - request info
   * @param request the request
   * @param userId the user id
   * @param key - file key generated by /config method
   * @return {@link Response}
   */
  @GET
  @Path("/content/{userId}/{key}")
  public Response content(@Context UriInfo uriInfo,
                          @Context HttpServletRequest request,
                          @PathParam("userId") String userId,
                          @PathParam("key") String key) {
    String clientHost = getClientHost(request);
    String clientIp = getClientIpAddr(request);

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice document content: " + userId + "@" + key + " to " + clientHost + "(" + clientIp + ")");
    }

    EditorResponse resp = new EditorResponse();
    if (editors.canDownloadBy(clientHost) || editors.canDownloadBy(clientIp)) {
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
          LOG.warn("Bad parameter to downloading content for " + key + ". " + e.getMessage());
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
    } else {
      LOG.warn("Attempt to download content by not allowed host: " + clientHost + "(" + clientIp + ")");
      resp.error("Not a document server").status(Status.UNAUTHORIZED);
    }
    return resp.build();
  }

  /**
   * Config configuration for Onlyoffice JS.
   *
   * @param uriInfo - request with base URI
   * @param workspace the workspace
   * @param path the path
   * @return response with
   */
  @GET
  @Path("/config/{workspace}/{path:.*}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response config(@Context UriInfo uriInfo, @PathParam("workspace") String workspace, @PathParam("path") String path) {

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
            URI requestUri = uriInfo.getRequestUri();
            Config config = editors.createEditor(requestUri.getScheme(), requestHost(requestUri), username, workspace, path);
            if (LOG.isDebugEnabled()) {
              LOG.debug("> Onlyoffice document config: " + workspace + ":" + path + " -> " + config.getDocument().getKey());
            }
            resp.config(config).ok();
          } else {
            LOG.warn("ConversationState not set to create editor config");
            resp.error("User not authenticated").status(Status.UNAUTHORIZED);
          }
        } catch (BadParameterException e) {
          LOG.warn("Bad parameter for creating editor config " + workspace + ":" + path + ". " + e.getMessage());
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
   * @param userId the user id
   * @param key - config key generated when requested editor config
   * @return {@link Response}
   */
  @GET
  @Path("/state/{userId}/{key}")
  @RolesAllowed("users")
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
          LOG.warn("Bad parameter for getting document state " + userId + "@" + key + ". " + e.getMessage());
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
      LOG.warn("Error getting document state. User identity not provided");
      resp.error("User not provided").status(Status.BAD_REQUEST);
    }

    return resp.build();
  }

  /**
   * Gets the client ip addr.
   *
   * @param request the request
   * @return the client ip addr
   */
  protected String getClientIpAddr(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("Proxy-Client-IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("WL-Proxy-Client-IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_CLIENT_IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_FORWARDED");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
    if (isValidHost(ip)) {
      return ip;
    }
    // http://stackoverflow.com/questions/1634782/what-is-the-most-accurate-way-to-retrieve-a-users-correct-ip-address-in-php
    ip = request.getHeader("HTTP_FORWARDED_FOR");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_FORWARDED");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("REMOTE_ADDR");
    if (isValidHost(ip)) {
      return ip;
    }
    // last chance to get it from Servlet request
    ip = request.getRemoteAddr();
    if (isValidHost(ip)) {
      return ip;
    }
    return null;
  }

  /**
   * Gets the client host.
   *
   * @param request the request
   * @return the client host
   */
  protected String getClientHost(HttpServletRequest request) {
    String host = request.getHeader("X-Forwarded-Host");
    if (isValidHost(host)) {
      return host;
    }
    // Solution based on X-Forwarded-For proposed in #3 to work correctly behind reverse proxy (production)
    String clientIp = request.getHeader("X-Forwarded-For");
    if (notEmpty(clientIp)) {
      // In case of several proxies: X-Forwarded-For: client, proxy1, proxy2
      int commaIdx = clientIp.indexOf(',');
      if (commaIdx > 0 && commaIdx < clientIp.length() - 1) {
        // use only client IP
        clientIp = clientIp.substring(0, commaIdx);
      }
    } else {
      // And a case of nginx, try X-Real-IP
      clientIp = request.getHeader("X-Real-IP");
    }
    if (notEmpty(clientIp)) {
      try {
        // XXX For this to work, in server.xml, enableLookups="true"
        host = InetAddress.getByName(clientIp).getHostName();
        if (notEmpty(host)) { // host here still may be an IP due to security restriction
          return host;
        }
      } catch (Exception e) {
        LOG.warn("Cannot obtain client hostname by its IP " + clientIp + ": " + e.getMessage());
      }
    }
    host = request.getRemoteHost();
    if (isValidHost(host)) {
      return host;
    }
    return null;
  }
  
  /**
   * Check string is not empty.
   *
   * @param str the str
   * @return true, if not empty, false otherwise
   */
  protected boolean notEmpty(String str) {
    return str != null && str.length() > 0;
  }

  /**
   * Checks if is valid host. It's a trivial check for <code>null</code>, non empty string and not "unknown"
   * text.
   *
   * @param host the host name or IP address
   * @return true, if is valid host
   */
  protected boolean isValidHost(String host) {
    if (host != null && host.length() > 0 && !"unknown".equalsIgnoreCase(host)) {
      return true;
    }
    return false;
  }

  /**
   * Request host.
   *
   * @param requestUri the request uri
   * @return the string
   */
  protected String requestHost(URI requestUri) {
    StringBuilder host = new StringBuilder(requestUri.getHost());
    if (requestUri.getPort() != 80 && requestUri.getPort() != 443) {
      host.append(':');
      host.append(requestUri.getPort());
    }
    return host.toString();
  }

}
