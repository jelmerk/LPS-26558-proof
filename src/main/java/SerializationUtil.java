import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerializationUtil {

	private SerializationUtil() {

	}

	public static byte[] serialize(Object object) {
		ObjectOutputStream os = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			os = new ObjectOutputStream(baos);
			os.writeObject(object);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			if (os != null) {
				try {
					os.flush();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
				try {
					os.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return baos.toByteArray();

	}
}
