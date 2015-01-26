package se.romram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.helpers.HTTPDate;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by micke on 2014-12-02.
 */
public class DefaultFileHandler implements RelaxHandler {
	private Logger log = LoggerFactory.getLogger(DefaultFileHandler.class);

    private static final String ALLOWED = "HEAD,GET,POST,PUT,DELETE";

    private String pathAsString;
    private Path path;

	private Map<String, String> extensionContentTypeMap;

	public DefaultFileHandler(String pathAsString) {
        path = FileSystems.getDefault().getPath(pathAsString);
        this.pathAsString = pathAsString;
		addExtensionContentType("js", "application/javascript");
		addExtensionContentType("json", "application/json");
		addExtensionContentType("pdf", "application/pdf");
		addExtensionContentType("xml", "application/xml");
		addExtensionContentType("png", "image/png");
		addExtensionContentType("jpg", "image/jpeg");
		addExtensionContentType("jpeg", "image/jpeg");
		addExtensionContentType("ico", "image/x-icon");
		addExtensionContentType("gif", "image/gif");
		addExtensionContentType("svg", "image/svg+xml");
		addExtensionContentType("css", "text/css");
		addExtensionContentType("csv", "text/csv");
		addExtensionContentType("htm", "text/html");
		addExtensionContentType("html", "text/html");
		addExtensionContentType("txt", "text/plain");
		addExtensionContentType("text", "text/plain");
		addExtensionContentType("rtf", "text/rtf");
		addExtensionContentType("md", "text/x-markdown");
    }

	public DefaultFileHandler addExtensionContentType(String extension, String contentType) {
		if (extensionContentTypeMap == null) {
			extensionContentTypeMap = new ConcurrentHashMap<>();
		}
		extensionContentTypeMap.put(extension, contentType);
		return this;
	}

    @Override
    public boolean handle(RelaxRequest request, RelaxResponse response) {
        log.debug("A {} request for resource {} with queryParameters '{}' has been received from user agent '{}'."
                , request.getMethod()
                , request.getRequestURL()
                , request.getQueryString()
                , request.getUserAgent());
		if (request.getMethod() == null) {
			log.warn("Empty request '{}'", request.getRequestBuffer());
		}
        Path filePath = FileSystems.getDefault().getPath(pathAsString, request.getPath());
        //log.debug(filePath.toAbsolutePath().toString());

        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return head(filePath, request, response);
        }
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return get(filePath, request, response);
        }
        if ("PUT".equalsIgnoreCase(request.getMethod())) {
            return put(filePath, request, response);
        }
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return delete(filePath, request, response);
        }

        response.addHeaders("Allow: " + ALLOWED);
        response.respond(405, "");
        return true;
    }

    private boolean head(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            byte[] payload = getPayload(filePath, request, response);
            long lastModified = filePath.toFile().lastModified();
            response.addHeaders("Last-Modified: " + HTTPDate.formatDate(lastModified));
            response.respond(204, "", payload.length);
            return true;
        } catch (IOException e) {
            response.respond(404, "");
            return true;
        }

    }

    private boolean get(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            byte[] payload = getPayload(filePath, request, response);
            long lastModified = filePath.toFile().lastModified();
            int extP = filePath.toFile().getName().lastIndexOf('.');
			if (extP != -1 && extP<filePath.toFile().getName().length()) {
				String ext = filePath.toFile().getName().substring(extP+1);
				String contentType = extensionContentTypeMap.get(ext);
				if (contentType != null) {
					response.setContentType(contentType);
				}
            }
            response.addHeaders("Last-Modified: " + HTTPDate.formatDate(lastModified));
            response.respond(200, payload);
            return true;
        } catch (IOException e) {
            response.respond(404, "");
            return true;
        }

    }

    private byte[] getPayload(Path filePath, RelaxRequest request, RelaxResponse response) throws IOException {
        boolean isHTMLAware = request.getAccept().toLowerCase().contains("text/html");
        if (filePath.toFile().isFile()) {
            return Files.readAllBytes(filePath);
        } else if (filePath.toFile().isDirectory()) {
            File[] files = filePath.toFile().listFiles();
            StringBuffer buf = new StringBuffer();
            if (isHTMLAware) {
                response.setContentType("text/html");
                buf.append("<html><head></head><body><table>");
            }
            for (File file : files) {
                if (isHTMLAware) buf.append("<tr><td>");
                buf.append(new Date(file.lastModified()));
                if (isHTMLAware) buf.append("</td><td>"); else buf.append(" ");
                buf.append(String.format("%10d", file.length()));
                if (isHTMLAware) {
                    String path = request.getPath();
                    if (path.charAt(path.length()-1) != '/') path += "/";
                    buf.append("</td><td><a href=\"" + path + file.getName() + "\">");
                } else {
                    buf.append(" ");
                }
                buf.append(file.getName());
                if (file.isDirectory()) buf.append("/");
                if (isHTMLAware) buf.append("</a></td></tr>"); else buf.append("\n");
            }
            if (isHTMLAware) {
                buf.append("</table></body></html>");
            }
            return buf.toString().getBytes();
        }
        throw new IOException("File not found!");
    }

    private boolean put(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            Files.createDirectories(filePath.getParent());
            BufferedWriter writer = Files.newBufferedWriter(filePath, Charset.forName(request.getRelaxServer().charsetName));
            writer.write(request.getPayload().toString());
            writer.close();
            response.respond(201, "File created!");
            return true;
        } catch (IOException e) {
            response.respond(500, "File was not created due to %s with message '%s'!", e.getClass().getSimpleName(), e.getMessage());
            return true;
        }
    }

    private boolean delete(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            Files.delete(filePath);
            response.respond(200, "");
            return true;
        } catch (IOException e) {
            response.respond(404, "File not found at " + filePath.toAbsolutePath().toString());
            return true;
        }
    }

}
