
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class UrlBuilder {

    private final String url;
    private final Charset charset;
    private final List<Parameter> parameters = new ArrayList<Parameter>();

    private UrlBuilder(String url, Charset charset) {
        this.url = url;
        this.charset = charset;
    }

    public UrlBuilder addParameter(String name, String value) {
        parameters.add(new Parameter(name, value));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(url);

        boolean first = true;

        for (Parameter parameter : parameters) {
            if (first) {
                builder.append('?');
                first = false;
            } else {
                builder.append('&');
            }

            String encodedValue = urlEncode(parameter.value, charset);

            builder.append(parameter.name)
                   .append('=')
                   .append(encodedValue);

        }
        return builder.toString();
    }

    public static UrlBuilder createUrl(String url, Charset charset) {
        return new UrlBuilder(url, charset);
    }

    private String urlEncode(String value, Charset charset) {
        try {
            return URLEncoder.encode(value, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Charset");
        }
    }

    private static class Parameter {

        public final String name;
        public final String value;

        private Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
