package org.exoplatform.onlyoffice.test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.test.mock.MockHttpServletRequest;

public class OnlyofficeMockHttpServletRequest extends MockHttpServletRequest {


  public OnlyofficeMockHttpServletRequest(String url, InputStream data, int length, String method, Map<String, List<String>> headers) {
    super(url, data, length, method, headers);
  }
   /**
    * {@inheritDoc}
    */
   public String getServerName() {
     try {
      return super.getServerName();
     } catch (Exception e) {

     }
     return "localhost";
   }

     /**
    * {@inheritDoc}
    */
   public int getServerPort() {
     try {
      return super.getServerPort();
     } catch (Exception e) {

     }
     return 8080;
   }

}
