package se.romram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.romram.helpers.HTTPDate;
import se.romram.server.RelaxRequest;
import se.romram.server.RelaxResponse;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
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
		addExtensionContentType("class", "application/java");
		addExtensionContentType("doc", "application/msword");
		addExtensionContentType("js", "application/javascript");
		addExtensionContentType("json", "application/json");
		addExtensionContentType("m3u8", "application/x-mpegURL");
		addExtensionContentType("pdf", "application/pdf");
		addExtensionContentType("pps", "application/vnd.ms-powerpoint");
		addExtensionContentType("ppt", "application/vnd.ms-powerpoint");
		addExtensionContentType("ps", "application/postscript");
		addExtensionContentType("rtf", "application/rtf");
		addExtensionContentType("swf", "application/x-shockwave-flash");
		addExtensionContentType("word", "application/msword");
		addExtensionContentType("xml", "application/xml");
		addExtensionContentType("xls", "application/vnd.ms-excel");
		addExtensionContentType("zip", "application/zip");
		addExtensionContentType("png", "image/png");
		addExtensionContentType("jpg", "image/jpeg");
		addExtensionContentType("jpeg", "image/jpeg");
		addExtensionContentType("ico", "image/x-icon");
		addExtensionContentType("gif", "image/gif");
		addExtensionContentType("svg", "image/svg+xml");
		addExtensionContentType("tif", "image/tiff");
		addExtensionContentType("tiff", "image/tiff");
		addExtensionContentType("xcf", "image/xcf");
		addExtensionContentType("xpm", "image/xpm");
		addExtensionContentType("css", "text/css");
		addExtensionContentType("csv", "text/csv");
		addExtensionContentType("htm", "text/html");
		addExtensionContentType("html", "text/html");
		addExtensionContentType("txt", "text/plain");
		addExtensionContentType("text", "text/plain");
		addExtensionContentType("rt", "text/richtext");
		addExtensionContentType("rtf", "text/rtf");
		addExtensionContentType("xsl", "text/xml");
		addExtensionContentType("xslt", "text/xml");
		addExtensionContentType("md", "text/x-markdown");
		addExtensionContentType("avi", "video/x-msvideo");
		addExtensionContentType("flv", "video/x-flv");
		addExtensionContentType("mov", "video/quicktime");
		addExtensionContentType("mp3", "audio/mpeg3");
		addExtensionContentType("mp4", "video/mp4");
		addExtensionContentType("mpe", "video/mpeg");
		addExtensionContentType("mpeg", "video/mpeg");
		addExtensionContentType("mpg", "video/mpeg");
		addExtensionContentType("mpga", "audio/mpeg");
		addExtensionContentType("wav", "audio/wav");
		addExtensionContentType("wmv", "audio/x-ms-wmv");
		addExtensionContentType("qt", "video/quicktime");
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
        log.info("A {} request for resource {} with queryParameters '{}' has been received from user agent '{}'."
				, request.getMethod()
				, request.getRequestURL()
				, request.getQueryString()
				, request.getUserAgent());
		if (request.getMethod() == null) {
			log.warn("Empty request '{}'", request.getRequestBuffer());
		}
        Path filePath = FileSystems.getDefault().getPath(pathAsString, request.getPath());

        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return get(filePath, request, response, true);
        }
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return get(filePath, request, response, false);
        }
        if ("PUT".equalsIgnoreCase(request.getMethod())) {
            return put(filePath, request, response);
        }
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			UUID uuid = UUID.randomUUID();
			filePath = FileSystems.getDefault().getPath(pathAsString, uuid.toString());
			return put(filePath, request, response);
		}
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return delete(filePath, request, response);
        }

        response.addHeaders("Allow: " + ALLOWED);
        response.respond(405, "");
        return true;
    }

    private boolean get(Path filePath, RelaxRequest request, RelaxResponse response, boolean head) {
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
			if (head) {
				response.respond(204, "", payload.length);
			} else {
				response.respond(200, payload);
			}
            return true;
        } catch (IOException e) {
            response.respond(404, "");
			log.warn("File {} not found.", filePath);
            return true;
        }

    }

    private byte[] getPayload(Path filePath, RelaxRequest request, RelaxResponse response) throws IOException {
        boolean isHTMLAware = request.getAccept().toLowerCase().contains("text/html");
        if (filePath.toFile().isFile()) {
			if (filePath.toFile().isHidden()) throw new IOException("File not found.");
            return Files.readAllBytes(filePath);
        } else if (filePath.toFile().isDirectory()) {
            File[] files = filePath.toFile().listFiles();
			Arrays.sort(files);
			StringBuffer buf = new StringBuffer();
            if (isHTMLAware) {
                response.setContentType("text/html");
                buf.append("<html><head><meta charset=\"utf-8\" /></head><body><table>");
            }
            for (File file : files) {
				if (!file.isHidden()) {
					if (isHTMLAware) buf.append("<tr><td>");
					buf.append(new Date(file.lastModified()));
					if (isHTMLAware) buf.append("</td><td style=\"text-align:right\">");
					else buf.append(" ");
					buf.append(String.format("%10d", file.length()));
					if (isHTMLAware) {
						String path = request.getPath();
						if (path.charAt(path.length() - 1) != '/') path += "/";
						buf.append("</td><td><a href=\"" + path + file.getName() + "\">");
					} else {
						buf.append(" ");
					}
					buf.append(file.getName());
					if (file.isDirectory()) buf.append("/");
					if (isHTMLAware) buf.append("</a></td></tr>");
					else buf.append("\n");
				}
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
			OutputStream fileOutputStream = Files.newOutputStream(filePath);
			fileOutputStream.write(request.getPayload());
			fileOutputStream.flush();
			fileOutputStream.close();
            response.respond(201, String.format("File %s created!", filePath.getFileName()));
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
