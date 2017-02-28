/**
 * The MIT License
 * Copyright © 2017 DTL
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.dtls.fairdatapoint.service.impl;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import nl.dtls.fairdatapoint.service.OrcidServiceException;
import org.apache.logging.log4j.LogManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.stereotype.Service;

/**
 * Service layer for communicating with ORCID api
 *
 * @author Rajaram Kaliyaperumal <rr.kaliyaperumal@gmail.com>
 * @author Kees Burger <kees.burger@dtls.nl>
 * @since 2017-02-28
 * @version 0.1
 */
@Service("orcidService")
public class OrcidService {

    private final static org.apache.logging.log4j.Logger LOGGER
            = LogManager.getLogger(OrcidService.class);
    private final String orcidUrl = "https://orcid.org/oauth/token";
    private final String clientId = "APP-2P194EDZ02TF7E4A";
    private final String clientSecret = "04f6637f-37e3-48ad-9ba7-086c48c0ab65";
    private final String grantType = "authorization_code";
    private final String redirectUri = 
            "http://127.0.0.1:8084/fdp/accessControl";

    public IRI getOrcidUri(@Nonnull String code) throws OrcidServiceException {
        IRI orcidUri = null;
        try {
            URL url = new URL(orcidUrl);
            HashMap<String, String> params = new HashMap<>();
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            params.put("grant_type", grantType);
            params.put("redirect_uri", redirectUri);
            params.put("code", code);
            Set set = params.entrySet();
            Iterator i = set.iterator();
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (postData.length() != 0) {
                    postData.append('&');
                }
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(
                        param.getValue()), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", 
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", 
                    String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (String line = null; (line = reader.readLine()) != null;) {
                builder.append(line).append("\n");
            }
            reader.close();
            conn.disconnect();
            LOGGER.info("INSTAGRAM token returned: " + 
                    builder.toString());
            Map jsonJavaRootObject = new Gson().fromJson(builder.toString(), 
                    Map.class);
            String orcid = (String) jsonJavaRootObject.get("orcid");
            String orcidUriPrefix = "http://orcid.org/";
            ValueFactory valueFactory = SimpleValueFactory.getInstance();
            orcidUri = valueFactory.createIRI(orcidUriPrefix + orcid);
        } catch (IOException ex) {
            String msg = "Error getting orcid url." + ex.getMessage();
            LOGGER.error(code);
            throw (new OrcidServiceException(msg));
        }
        return orcidUri;
    }

}
