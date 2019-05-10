package org.exoplatform.onlyoffice;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import com.sun.xml.bind.Util;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.onlyoffice.rest.EditorService;
import org.exoplatform.onlyoffice.test.OnlyofficeMockHttpServletRequest;
import org.exoplatform.services.rest.impl.ContainerRequest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.tools.DummyContainerResponseWriter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class EditorServiceTest extends BaseCommonsTestCase {

  protected static final String RESOURCE_URL = "/onlyoffice/editor";

  protected static final String user         = "john";

  protected static final String key          = "666ea5ae-2732-32d8-862e-904ff56a16f9";

  protected EditorService       editorService;

  protected RequestHandlerImpl  requestHandler;

  protected String              statusJsonPayload;

  protected String              contentToken;

  protected String              statusToken;

  protected String              contentWrongToken;

  protected String              statusWrongToken;

  @Override
  protected void beforeClass() {
    super.beforeClass();
    this.container = PortalContainer.getInstance();
    ExoContainerContext.setCurrentContainer(container);
    requestHandler = getContainer().getComponentInstanceOfType(RequestHandlerImpl.class);

    Map<String, Object> contentPayload = new HashMap<>();
    Map<String, Object> statusPayload = new HashMap<>();

    contentPayload.put("key", key);

    statusPayload.put("key", key);
    statusPayload.put("satus", 1);
    statusPayload.put("url", "localhost");
    statusPayload.put("error", 0);
    statusJsonPayload = "{ \"key\": \"" + key + "\", \"status\": 1, \"url\": \"localhost\", \"error\": 0 }";

    String secretKey = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";
    String wrongSecretKey = "000W5444u3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM10000";

    contentToken = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", contentPayload)
                       .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                       .compact();

    statusToken = Jwts.builder()
                      .setSubject("exo-onlyoffice")
                      .claim("payload", statusPayload)
                      .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                      .compact();

    contentWrongToken = Jwts.builder()
                            .setSubject("exo-onlyoffice")
                            .claim("payload", contentPayload)
                            .signWith(Keys.hmacShaKeyFor(wrongSecretKey.getBytes()))
                            .compact();

    statusWrongToken = Jwts.builder()
                           .setSubject("exo-onlyoffice")
                           .claim("payload", statusPayload)
                           .signWith(Keys.hmacShaKeyFor(wrongSecretKey.getBytes()))
                           .compact();
  }

  @Test
  public void testContent() throws Exception {

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + contentToken);
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + user + "/" + key, "", headers, null);
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("{\"error\":\"File key not found " + key + "\"}", response.getEntity());
  }

  @Test
  public void testContentWrongToken() throws Exception {

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + contentWrongToken);
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + user + "/" + key, "", headers, null);
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  @Test
  public void testContentNoToken() throws Exception {
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + user + "/" + key, "", null, null);
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  @Test
  public void testStatus() throws Exception {
    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + statusToken);
    ContainerResponse response = service("POST",
                                         RESOURCE_URL + "/status/" + user + "/" + key,
                                         "",
                                         headers,
                                         statusJsonPayload.getBytes());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("{\"error\":\"File key not found " + key + "\"}", response.getEntity());
  }

  @Test
  public void testStatusWrongToken() throws Exception {

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + statusWrongToken);
    ContainerResponse response = service("POST",
                                         RESOURCE_URL + "/status/" + user + "/" + key,
                                         "",
                                         headers,
                                         statusJsonPayload.getBytes());
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  @Test
  public void testStatusNoToken() throws Exception {

    ContainerResponse response = service("POST",
                                         RESOURCE_URL + "/status/" + user + "/" + key,
                                         "",
                                         null,
                                         statusJsonPayload.getBytes());
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  protected ContainerResponse service(String method,
                                      String requestURI,
                                      String baseURI,
                                      Map<String, List<String>> headers,
                                      byte[] data) throws Exception {

    if (headers == null) {
      headers = new MultivaluedMapImpl();
    }

    ByteArrayInputStream in = null;
    if (data != null) {
      in = new ByteArrayInputStream(data);
    }

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest =
                                   new OnlyofficeMockHttpServletRequest("", in, in != null ? in.available() : 0, method, headers);
    envctx.put(HttpServletRequest.class, httpRequest);
    EnvironmentContext.setCurrent(envctx);
    ContainerRequest request = new ContainerRequest(method,
                                                    new URI(requestURI),
                                                    new URI(baseURI),
                                                    in,
                                                    new InputHeadersMap(headers));
    ContainerResponse response = new ContainerResponse(new DummyContainerResponseWriter());
    requestHandler.handleRequest(request, response);
    return response;
  }

}
