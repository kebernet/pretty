package com.reachcall.pretty.http;

import com.reachcall.pretty.Pretty;
import com.reachcall.pretty.config.Match;
import com.reachcall.pretty.config.Resolver;

import com.reachcall.util.Pool;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnBinaryMessage;
import org.eclipse.jetty.websocket.WebSocket.OnFrame;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketServlet;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author kebernet
 */
public class ProxyServlet extends WebSocketServlet {

    private static final Logger LOG = Logger.getLogger(Pretty.class.getCanonicalName());
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final long serialVersionUID = 1L;
    private static final String HEADER_REMOTE_HOST = "X-Remote-Host";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_LENTH = "Content-Length";
    private static final String HEADER_HOST = "Host";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_ORIGINAL_REQUEST_URL = "X-Original-Request-URL";
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final ThreadLocal<HostInterceptor> HOST = new ThreadLocal<HostInterceptor>();
    private final Pool<DefaultHttpClient> clients = new Pool<DefaultHttpClient>(2 * 60 * 1000) {
        @Override
        protected DefaultHttpClient create() {
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
            schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

            PoolingClientConnectionManager cm = new PoolingClientConnectionManager(schemeRegistry);
            cm.setMaxTotal(20);
            cm.setDefaultMaxPerRoute(20);

            DefaultHttpClient dc = new DefaultHttpClient(cm);
            dc.getParams()
                    .setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
            dc.getParams()
                    .setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);

            return dc;
        }
    };
    private Resolver resolver;
    private String affinityCookie;
    private String affinityCookiePrefix;
    private int maxUploadBytes = 50 * 1024 * 1024;

    public ProxyServlet() {
    }

    @SuppressWarnings("unchecked")
    public void copyHeaders(HttpServletRequest req, HttpRequestBase method) {
        Enumeration names = req.getHeaderNames();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            if (name.equalsIgnoreCase(HEADER_CONTENT_LENTH)) {
                continue;
            }

            Enumeration values = req.getHeaders(name);

            while (values.hasMoreElements()) {
                String value = (String) values.nextElement();
                LOG.log(Level.FINER, "{0} : {1}", new Object[]{name, value});

                if (name.equalsIgnoreCase(HEADER_HOST) || name.equalsIgnoreCase(HEADER_X_FORWARDED_FOR)) {
                    continue;
                } else {
                    method.addHeader(name, value);
                }
            }

            setVHost(req.getHeader("Host"));
        }
        method.addHeader(HEADER_REMOTE_HOST, req.getRemoteAddr());
        String xff = req.getHeader(HEADER_X_FORWARDED_FOR);

        if (xff == null) {
            xff = "";
        } else {
            xff = xff + ", ";
        }

        method.addHeader(HEADER_X_FORWARDED_FOR, xff + req.getRemoteHost());

        if (req.getHeader(HEADER_X_ORIGINAL_REQUEST_URL) == null) {
            method.addHeader(HEADER_X_ORIGINAL_REQUEST_URL, req.getRequestURL().toString());
        }
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Match match = (Match) req.getAttribute("match");
        HttpDelete method = new HttpDelete(match.toURL());
        copyHeaders(req, method);
        this.execute(match, method, req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        Match match = (Match) req.getAttribute("match");
        HttpGet method = new HttpGet(match.toURL());
        copyHeaders(req, method);
        this.execute(match, method, req, resp);
    }

    @Override
    public void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Match match = (Match) req.getAttribute("match");
        HttpHead method = new HttpHead(match.toURL());
        copyHeaders(req, method);
        this.execute(match, method, req, resp);
    }

    @SuppressWarnings("unchecked")
    private void doMultipart(HttpPost method, HttpServletRequest req)
            throws ServletException, FileUploadException, UnsupportedEncodingException, IOException {
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
        diskFileItemFactory.setSizeThreshold(this.getMaxFileUploadSize());
        diskFileItemFactory.setRepository(TEMP_DIR);

        ServletFileUpload upload = new ServletFileUpload(diskFileItemFactory);

        List<FileItem> fileItems = (List<FileItem>) upload.parseRequest(req);

        MultipartEntity entity = new MultipartEntity();

        for (FileItem fItem : fileItems) {
            if (fItem.isFormField()) {
                LOG.log(Level.INFO, "Form field {0}", fItem.getName());

                StringBody part = new StringBody(fItem.getFieldName());
                entity.addPart(fItem.getFieldName(), part);
            } else {
                LOG.log(Level.INFO, "File item {0}", fItem.getName());

                InputStreamBody file = new InputStreamBody(fItem.getInputStream(), fItem.getName(),
                        fItem.getContentType());
                entity.addPart(fItem.getFieldName(), file);
            }
        }

        method.setEntity(entity);
    }

    @Override
    public void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Match match = (Match) req.getAttribute("match");
        HttpOptions method = new HttpOptions(match.toURL());
        copyHeaders(req, method);
        this.execute(match, method, req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException, UnsupportedEncodingException {
        Match match = (Match) req.getAttribute("match");
        HttpPost method = new HttpPost(match.toURL());
        copyHeaders(req, method);

        //        if (ServletFileUpload.isMultipartContent(req)) {
        //            try {
        //                this.doMultipart(method, req);
        //            } catch (FileUploadException ex) {
        //                Logger.getLogger(ProxyServlet.class.getName())
        //                      .log(Level.SEVERE, null, ex);
        //                throw new ServletException(ex);
        //            }
        //        } else {
        this.doPost(method, req);
        //        }
        this.execute(match, method, req, resp);
    }

    @SuppressWarnings("unchecked")
    private void doPost(HttpPost method, HttpServletRequest req)
            throws IOException {
        copyHeaders(req, method);
        if (CONTENT_TYPE_FORM.equalsIgnoreCase(req.getContentType())) {
            Map<String, String[]> params = (Map<String, String[]>) req.getParameterMap();
            List<NameValuePair> pairs = new LinkedList<NameValuePair>();

            for (String name : params.keySet()) {
                String[] values = params.get(name);

                for (String value : values) {
                    NameValuePair pair = new BasicNameValuePair(name, value);
                    pairs.add(pair);
                }
            }

            method.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
        } else {
            method.setEntity(new InputStreamEntity(req.getInputStream(), req.getContentLength()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Match match = (Match) req.getAttribute("match");
        HttpPut method = new HttpPut(match.toURL());
        copyHeaders(req, method);

        method.setEntity(new InputStreamEntity(req.getInputStream(), req.getContentLength()));

        this.execute(match, method, req, resp);
    }

    @Override
    public void doTrace(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Match match = (Match) req.getAttribute("match");
        HttpTrace method = new HttpTrace(match.toURL());
        copyHeaders(req, method);
        this.execute(match, method, req, resp);
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest hsr, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void execute(Match match, HttpRequestBase method, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        LOG.log(Level.FINE, "Requesting {0}", method.getURI());

        HttpClient client = this.getClient();
        HttpResponse response = client.execute(method);

        for (Header h : response.getHeaders("Set-Cookie")) {
            String line = parseCookie(h.getValue());
            LOG.log(Level.FINER, method.getURI() + "{0}", new Object[]{line});

            if (line != null) {
                LOG.log(Level.INFO, "Bonding session {0} to host {1}",
                        new Object[]{line, match.destination.hostAndPort()});

                try {
                    resolver.bond(line, match);
                } catch (ExecutionException ex) {
                    Logger.getLogger(ProxyServlet.class.getName())
                            .log(Level.SEVERE, null, ex);
                    this.releaseClient(client);

                    throw new ServletException(ex);
                }
            }
        }

        int rc = response.getStatusLine()
                .getStatusCode();

        if ((rc >= HttpServletResponse.SC_MULTIPLE_CHOICES) && (rc < HttpServletResponse.SC_NOT_MODIFIED)) {
            String location = response.getFirstHeader(HEADER_LOCATION)
                    .getValue();

            if (location == null) {
                throw new ServletException("Recieved status code: " + rc + " but no " + HEADER_LOCATION
                        + " header was found in the response");
            }

            String hostname = req.getServerName();

            if ((req.getServerPort() != 80) && (req.getServerPort() != 443)) {
                hostname += (":" + req.getServerPort());
            }

            hostname += req.getContextPath();

            String replaced = location.replace(match.destination.hostAndPort() + match.path.getDestination(), hostname);

            if (replaced.startsWith("http://")) {
                replaced = replaced.replace("http:", req.getScheme() + ":");
            }

            resp.sendRedirect(replaced);
            this.releaseClient(client);

            return;
        } else if (rc == HttpServletResponse.SC_NOT_MODIFIED) {
            resp.setIntHeader(HEADER_CONTENT_LENTH, 0);
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            this.releaseClient(client);

            return;
        }

        resp.setStatus(rc);

        Header[] headerArrayResponse = response.getAllHeaders();

        for (Header header : headerArrayResponse) {
            resp.setHeader(header.getName(), header.getValue());
        }

        if (response.getEntity() != null) {
            resp.setContentLength((int) response.getEntity().getContentLength());

            response.getEntity()
                    .writeTo(resp.getOutputStream());
        }

        this.releaseClient(client);

        LOG.finer("Done.");
    }

    private HttpClient getClient() {
        DefaultHttpClient dc = this.clients.checkout();
        dc.addRequestInterceptor(this.getHost());

        return dc;
    }

    private HostInterceptor getHost() {
        HostInterceptor h = HOST.get();

        if (h == null) {
            h = new HostInterceptor();
            HOST.set(h);
        }

        return h;
    }

    private int getMaxFileUploadSize() {
        return this.maxUploadBytes;
    }

    private String parseCookie(String line) {
        if (line.startsWith(this.affinityCookiePrefix)) {
            line = line.substring(this.affinityCookiePrefix.length());

            int index = line.indexOf(";");

            if (index == -1) {
                return line;
            } else {
                return line.substring(0, index);
            }
        }

        return null;
    }

    private void releaseClient(HttpClient client) {
        DefaultHttpClient dc = (DefaultHttpClient) client;
        dc.removeRequestInterceptorByClass(HostInterceptor.class);
        this.clients.checkin(dc);
    }

    public void setAffinityCookie(String affinityCookie) {
        this.affinityCookie = affinityCookie;
        this.affinityCookiePrefix = this.affinityCookie + "=";
    }

    public void setMaxFileUploadSize(int maxUpload) {
        this.maxUploadBytes = maxUpload;
    }

    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    private void setVHost(String vhost) {
        getHost().host = vhost;
    }

    private static class HostInterceptor implements HttpRequestInterceptor {

        String host;

        @Override
        public void process(HttpRequest hr, HttpContext hc)
                throws HttpException, IOException {
            hr.setHeader(HTTP.TARGET_HOST, host);
        }
    }

    private static class ProxyWebSocket implements WebSocket, OnFrame, OnTextMessage, OnBinaryMessage {

        private Connection conn;

        @Override
        public void onClose(int i, String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean onFrame(byte b, byte b1, byte[] bytes, int i, int i1) {
            return true;
        }

        @Override
        public void onHandshake(FrameConnection fc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void onMessage(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void onMessage(byte[] bytes, int i, int i1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void onOpen(Connection cnctn) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
