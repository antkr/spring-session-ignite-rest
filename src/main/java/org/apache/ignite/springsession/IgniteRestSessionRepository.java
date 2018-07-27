package org.apache.ignite.springsession;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

@Component
public class IgniteRestSessionRepository implements SessionRepository<IgniteSession> {

    public final static String DFLT_IGNITE_ADDRESS = "localhost";

    public final static String DFLT_IGNITE_PORT = "8080";

    public static final String DFLT_SESSION_STORAGE_NAME = "spring.session.cache";

    private final static String CMD = "cmd";

    private final static String GET = "get";

    private final static String PUT = "put";

    private final static String DELETE = "rmv";

    private final static String CREATE_CACHE = "getOrCreate";

    private static final String CACHE_NAME = "cacheName";

    private static final String KEY = "key";

    private static final String VALUE = "val";

    private static final String CACHE_TEMPLATE = "templateName";

    private static final String REPLICATED = "REPLICATED";

    private static final String PARTITIONED = "PARTITIONED";

    private String ip;

    private String port;

    private String sessionCacheName;

    private Integer defaultMaxInactiveInterval;

    private ObjectMapper mapper;

    private ConversionService conversionService;

    public IgniteRestSessionRepository() {

    }

    public IgniteRestSessionRepository(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(String port) {
        this.port = port;
    }

    private static GenericConversionService createDefaultConversionService() {
        GenericConversionService converter = new GenericConversionService();
        converter.addConverter(IgniteSession.class, byte[].class,
            new SerializingConverter());
        converter.addConverter(byte[].class, IgniteSession.class,
            new DeserializingConverter());
        return converter;
}

    private String serialize(IgniteSession attributeValue) {
        byte[] value = (byte[]) this.conversionService.convert(attributeValue,
            TypeDescriptor.valueOf(IgniteSession.class),
            TypeDescriptor.valueOf(byte[].class));
        return Hex.encodeHexString(value);
    }

    private IgniteSession deserialize(String value) throws DecoderException {
        if ("null".equals(value))
            return null;
        return (IgniteSession) this.conversionService.convert(Hex.decodeHex(value.toCharArray()),
            TypeDescriptor.valueOf(byte[].class),
            TypeDescriptor.valueOf(IgniteSession.class));
    }

    @PostConstruct
    public void init() {
        this.mapper = new ObjectMapper();
        this.mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        this.conversionService = createDefaultConversionService();

        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        HttpResponse res;

        try {
            Map<String, String> ss = new HashMap<String, String>();
            ss.put(CMD, CREATE_CACHE);
            ss.put(CACHE_NAME, sessionCacheName);
            ss.put(CACHE_TEMPLATE, REPLICATED);
            res = client.execute(buildCacheRequest("localhost", "8080", ss));

            try {
                assert res.getStatusLine().getStatusCode() == 200;
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private CloseableHttpClient getHttpClient(String ip, String port) {
        CloseableHttpClient client = HttpClients.custom().build();
        return client;
    }

    private HttpGet buildCacheRequest(String host, String port, Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder("http://" + host + ":" + port + "/ignite?");

        String prefix = "";
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append(prefix);
            prefix="&";
            sb.append(e.getKey()).append('=').append(e.getValue());
        }

        URL url = new URL(sb.toString());

        return new HttpGet(url.toURI());
    }

    public void setSessionCacheName(String sessionCacheName) {
        this.sessionCacheName = sessionCacheName;
    }

    @Override public IgniteSession createSession() {
        IgniteSession session = new IgniteSession();

        if (this.defaultMaxInactiveInterval != null)
            session.setMaxInactiveIntervalInSeconds(this.defaultMaxInactiveInterval);

        return session;
    }

    @Override public void save(IgniteSession session) {
        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        HttpResponse res;

        try {
            Map<String, String> ss = new HashMap<String, String>();
            ss.put(CMD, PUT);
            ss.put(CACHE_NAME, sessionCacheName);
            ss.put(KEY, session.getId());
            ss.put(VALUE, serialize(session));

            res = client.execute(buildCacheRequest(this.ip, this.port, ss));

            try {
                assert res.getStatusLine().getStatusCode() == 200;
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override public IgniteSession getSession(String id) {

        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        Map<String, String> ss = new HashMap<String, String>();
        ss.put(CMD, GET);
        ss.put(CACHE_NAME, sessionCacheName);
        ss.put(KEY, id);

        HttpResponse res;

        IgniteSession session = null;
        try {
            res = client.execute(buildCacheRequest("localhost", "8080", ss));
            try {
                assert res.getStatusLine().getStatusCode() == 200;

                InputStream is = null;

                try {
                    is = res.getEntity().getContent();

                    JsonNode node = mapper.readTree(is).get("response");
                    session = deserialize(node.asText());
                }
                finally {
                    if (is != null )
                        is.close();
                }
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (session == null)
            return null;

        if (session.isExpired()) {
            delete(id);
            return null;
        }

        return session;
    }

    @Override public void delete(String id) {
        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        HttpResponse res;

        try {
            Map<String, String> ss = new HashMap<String, String>();
            ss.put(CMD, DELETE);
            ss.put(CACHE_NAME, sessionCacheName);
            ss.put(KEY, id);
            res = client.execute(buildCacheRequest("localhost", "8080", ss));

            try {
                assert res.getStatusLine().getStatusCode() == 200;
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDefaultMaxInactiveInterval(Integer dfltMaxInactiveInterval) {
        defaultMaxInactiveInterval = dfltMaxInactiveInterval;
    }

    private String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1)
            result.write(buffer, 0, length);
        return result.toString("UTF-8");
    }
}
