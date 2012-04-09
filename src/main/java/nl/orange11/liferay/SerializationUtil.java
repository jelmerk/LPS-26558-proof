package nl.orange11.liferay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerializationUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SerializationUtil.class);

    private SerializationUtil() {
    }

    public static byte[] serialize(Object object) {
        ObjectOutputStream os = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            os = new ObjectOutputStream(baos);
            os.writeObject(object);
            os.flush();
        } catch (IOException e) {
            LOG.error("Should never happen", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    LOG.error("Should never happen", e);
                }
            }
        }
        return baos.toByteArray();

    }
}
