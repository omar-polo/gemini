package com.omarpolo.gemini;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Request implements AutoCloseable {

    private final BufferedReader in;
    private final PrintWriter out;
    private final SSLSocket sock;

    private final int code;
    private final String meta;

    public static class DummyManager extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    public static class MalformedResponse extends Exception {}

    public Request(String uri) throws IOException, MalformedResponse, URISyntaxException {
        this(new URI(uri));
    }

    public Request(URL url) throws IOException, MalformedResponse {
        this(url.getHost(), url.getPort(), url.toString());
    }

    public Request(URI uri) throws IOException, MalformedResponse {
        this(uri.getHost(), uri.getPort(), uri.toString());
    }

    public Request(String host, int port, URL req) throws IOException, MalformedResponse {
        this(host, port, req.toString());
    }

    public Request(String host, int port, URI req) throws IOException, MalformedResponse {
        this(host, port, req.toString());
    }

    public Request(String host, int port, String req) throws IOException, MalformedResponse {
        if (port == -1) {
            port = 1965;
        }

        sock = connect(host, port);

        var outStream = sock.getOutputStream();
        out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(outStream)));

        out.print(req);
        out.print("\r\n");
        out.flush();

        var inStream = sock.getInputStream();
        in = new BufferedReader(new InputStreamReader(inStream));

        var reply = in.readLine();

        if (reply.length() > 1027) {
            throw new MalformedResponse();
        }

        var s = new Scanner(new StringReader(reply));
        try {
            code = s.nextInt();
            s.skip(" ");
            meta = s.nextLine();
        } catch (NoSuchElementException e) {
            throw new MalformedResponse();
        }
    }

    public SSLSocket connect(String host, int port) throws IOException {
        try {
            var params = new SSLParameters();
            params.setServerNames(Collections.singletonList(new SNIHostName(host)));

            var ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new DummyManager[]{new DummyManager()}, new SecureRandom());
            var factory = (SSLSocketFactory) ctx.getSocketFactory();

            var socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSSLParameters(params);
            socket.startHandshake();
            return socket;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Unexpected failure", e);
        }
    }

    public int getCode() {
        return code;
    }

    public String getMeta() {
        return meta;
    }

    public BufferedReader body() {
        return in;
    }

    public void close() throws IOException {
        in.close();
        out.close();
        sock.close();
    }
}
