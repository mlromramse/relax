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

/**
 * Created by micke on 2014-12-02.
 */
public class DefaultFileHandler implements RelaxHandler {
	private Logger log = LoggerFactory.getLogger(DefaultFileHandler.class);

    private static final String ALLOWED = "HEAD,GET,POST,PUT,DELETE";

    private String pathAsString;
    private Path path;

	public DefaultFileHandler(String pathAsString) {
        path = FileSystems.getDefault().getPath(pathAsString);
        this.pathAsString = pathAsString;
    }

    @Override
    public boolean handle(RelaxRequest request, RelaxResponse response) {
        /*
        log.debug("A {} request for resource {} with queryParameters '{}' has been received from user agent '{}'."
                , request.getMethod()
                , request.getRequestURL()
                , request.getQueryString()
                , request.getUserAgent());
                */
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
        return false;
    }

    private boolean head(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            byte[] payload = getPayload(filePath);
            long lastModified = filePath.toFile().lastModified();
            response.addHeaders("Last-Modified: " + HTTPDate.formatDate(lastModified));
            response.respond(204, "", payload.length);
            return true;
        } catch (IOException e) {
            response.respond(404, "");
            return false;
        }

    }

    private boolean get(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            byte[] payload = getPayload(filePath);
            long lastModified = filePath.toFile().lastModified();
            if (filePath.toFile().getName().endsWith(".png")) {
                response.setContentType("image/png");
            }
            response.addHeaders("Last-Modified: " + HTTPDate.formatDate(lastModified));
            response.respond(200, payload);
            return true;
        } catch (IOException e) {
            response.respond(404, "");
            return false;
        }

    }

    private byte[] getPayload(Path filePath) throws IOException {
        if (filePath.toFile().isFile()) {
            return Files.readAllBytes(filePath);
        } else if (filePath.toFile().isDirectory()) {
            File[] files = filePath.toFile().listFiles();
            StringBuffer buf = new StringBuffer();
            for (File file : files) {
                buf.append(new Date(file.lastModified()));
                buf.append(" ");
                buf.append(String.format("%10d", file.length()));
                buf.append(" ");
                buf.append(file.getName());
                if (file.isDirectory()) buf.append("/");
                buf.append("\n");
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
            return false;
        }
    }

    private boolean delete(Path filePath, RelaxRequest request, RelaxResponse response) {
        try {
            Files.delete(filePath);
            response.respond(200, "");
            return true;
        } catch (IOException e) {
            response.respond(404, "File not found at " + filePath.toAbsolutePath().toString());
            return false;
        }
    }

}
