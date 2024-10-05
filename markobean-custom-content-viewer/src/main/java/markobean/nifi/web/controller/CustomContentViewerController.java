package markobean.nifi.web.controller;

//import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//import org.apache.avro.Conversions;
//import org.apache.avro.data.TimeConversions;
//import org.apache.avro.file.DataFileStream;
//import org.apache.avro.generic.GenericData;
//import org.apache.avro.generic.GenericDatumReader;
//import org.apache.avro.io.DatumReader;
import org.apache.nifi.authorization.AccessDeniedException;
import org.apache.nifi.web.ContentAccess;
import org.apache.nifi.web.ContentRequestContext;
import org.apache.nifi.web.DownloadableContent;
import org.apache.nifi.web.HttpServletContentRequestContext;
import org.apache.nifi.web.ResourceNotFoundException;
//import org.apache.nifi.xml.processing.transform.StandardTransformProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//import org.yaml.snakeyaml.DumperOptions;
//import org.yaml.snakeyaml.Yaml;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

@RestController()
@RequestMapping("/api")
public class CustomContentViewerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomContentViewerController.class);

    @GetMapping("/content")
    public void getContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ContentRequestContext requestContext = new HttpServletContentRequestContext(request);

        // get the content
        final ServletContext servletContext = request.getServletContext();
        final ContentAccess contentAccess = (ContentAccess) servletContext.getAttribute("nifi-content-access");

        // get the content
        final DownloadableContent downloadableContent;
        try {
            downloadableContent = contentAccess.getContent(requestContext);
        } catch (final ResourceNotFoundException e) {
            logger.warn("Content not found", e);
            response.sendError(HttpURLConnection.HTTP_NOT_FOUND, "Content not found");
            return;
        } catch (final AccessDeniedException e) {
            logger.warn("Content access denied", e);
            response.sendError(HttpURLConnection.HTTP_FORBIDDEN, "Content access denied");
            return;
        } catch (final Exception e) {
            logger.warn("Content retrieval failed", e);
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "Content retrieval failed");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);

        final boolean formatted = Boolean.parseBoolean(request.getParameter("formatted"));
        if (!formatted) {
            final InputStream contentStream = downloadableContent.getContent();
            contentStream.transferTo(response.getOutputStream());
            return;
        }

        // allow the user to drive the data type but fall back to the content type if necessary
        String displayName = request.getParameter("mimeTypeDisplayName");
        if (displayName == null) {
            final String contentType = downloadableContent.getType();
            displayName = getDisplayName(contentType);
        }

        if (displayName == null) {
            response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "Unknown content type");
            return;
        }

        try {
            switch (displayName) {
                case "abcd": {
                    // TODO... This is where custom code is required to interpret the custom data type
                    final InputStream contentStream = downloadableContent.getContent();
                    contentStream.transferTo(response.getOutputStream());

                    break;
                }

                default: {
                    response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "CUSTOM: Unsupported content type: " + displayName);
                }
            }
        } catch (final Throwable t) {
            logger.warn("CUSTOM: Unable to format FlowFile content", t);
            response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, "CUSTOM: Unable to format FlowFile content");
        }
    }

    private String getDisplayName(String contentType) {
        return switch (contentType) {
            case "application/abcd" -> "abcd";
            case null, default -> null;
        };
    }
}