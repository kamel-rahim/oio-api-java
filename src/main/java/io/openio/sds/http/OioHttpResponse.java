package io.openio.sds.http;

import static io.openio.sds.common.Strings.nullOrEmpty;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.Socket;
import java.util.HashMap;

import io.openio.sds.common.Strings;

public class OioHttpResponse {

    private static final int R = 1;
    private static final int RN = 2;
    private static final int RNR = 3;
    private static final int RNRN = 3;

    private static byte BS_R = '\r';
    private static byte BS_N = '\n';

    private ResponseHead head;

    private Socket sock;

    private InputStream sis;

    private OioHttpResponse(Socket sock) {
        this.sock = sock;
    }

    public static OioHttpResponse build(Socket sock) throws IOException {
        return new OioHttpResponse(sock)
                .responseHead();
    }

    public HashMap<String, String> headers() {
        return head.headers();
    }

    public String header(String key) {
        return head.header(key);
    }

    public int code() {
        return head.code();
    }

    public String msg() {
        return head.msg();
    }

    private OioHttpResponse responseHead() throws IOException {
        this.sis = sock.getInputStream();
        this.head = ResponseHead.parse(readHeaders());
        return this;
    }

    private String readHeaders() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int state = 0;
        while (state < RNRN)
            state = next(sis, bos, state);
        return new String(bos.toByteArray());
    }

    private int next(InputStream sis, ByteArrayOutputStream bos,
            int state) throws IOException {
        int b = sis.read();
        System.out.println(b);
        if (-1 == b) {
            System.err.println("Unexpected end of stream");
            throw new IOException("Unexpected end of stream");
        }
        bos.write(b);
        switch (state) {
        case R:
            return b == BS_N ? RN : 0;
        case RN:
            return b == BS_R ? RNR : 0;
        case RNR:
            return b == BS_N ? RNRN : 0;
        default:
            return b == BS_R ? R : 0;
        }
    }

    public static class ResponseHead {

        private BufferedReader reader;
        private StatusLine statusLine;
        private HashMap<String, String> headers = new HashMap<String, String>();

        private ResponseHead(BufferedReader reader) {
            this.reader = reader;
        }

        public static ResponseHead parse(String head) throws IOException {
            return new ResponseHead(new BufferedReader(new StringReader(head)))
                    .parseStatusLine()
                    .parseHeaders();
        }

        private ResponseHead parseStatusLine() throws IOException {
            this.statusLine = StatusLine.parse(reader.readLine());
            return this;
        }

        private ResponseHead parseHeaders() throws IOException {
            String line = null;
            while (null != (line = reader.readLine())) {
                if (nullOrEmpty(line))
                    continue;
                String[] tok = line.trim().split(":", 2);
                if (2 != tok.length)
                    continue;
                headers.put(tok[0].trim(), tok[1].trim());
            }
            return this;
        }

        public HashMap<String, String> headers() {
            return this.headers;
        }

        public String header(String key) {
            return this.headers.get(key);
        }

        public int code() {
            return statusLine.code();
        }

        public String msg() {
            return statusLine.msg();
        }

    }

    public static class StatusLine {

        private String proto;
        private int code;
        private String msg;

        private StatusLine(String proto, int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static StatusLine parse(String line) throws IOException {
            System.out.println(line);
            String[] tok = line.trim().split(" ", 3);
            if (3 != tok.length)
                throw new IOException(format("Invalid status line (%s)", line));
            return new StatusLine(tok[0], Integer.valueOf(tok[1].trim()),
                    tok[2].trim());
        }
        
        public String proto() {
            return this.proto;
        }

        public String msg() {
            return this.msg;
        }

        public int code() {
            return this.code;
        }

    }

}
