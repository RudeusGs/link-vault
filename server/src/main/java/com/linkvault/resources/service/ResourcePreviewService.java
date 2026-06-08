package com.linkvault.resources.service;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.resources.dto.DocumentPreviewResponse;
import com.linkvault.resources.dto.ResourceFileContent;
import com.linkvault.resources.dto.ResourcePreviewResponse;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.enums.ResourceType;
import com.linkvault.storage.dto.StorageFile;
import com.linkvault.storage.service.StorageService;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Service;

@Service
public class ResourcePreviewService {

    private final StorageService storageService;

    public ResourcePreviewService(StorageService storageService) {
        this.storageService = storageService;
    }

    public ResourcePreviewResponse preview(Resource resource) {
        if (resource.getResourceType() != ResourceType.FILE) {
            return new ResourcePreviewResponse(
                resource.getId(),
                resource.getResourceType(),
                resource.getTitle(),
                resource.getUrl(),
                resource.getMimeType(),
                resource.getFileName(),
                true,
                null
            );
        }

        boolean supported = isPreviewSupported(resource.getMimeType(), resource.getFileName());
        return new ResourcePreviewResponse(
            resource.getId(),
            resource.getResourceType(),
            resource.getTitle(),
            "/api/resources/" + resource.getId() + "/file",
            resource.getMimeType(),
            resource.getFileName(),
            supported,
            supported ? null : "Preview is not supported for this file type"
        );
    }

    public ResourceFileContent fileContent(Resource resource) {
        if (resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Resource is not a file");
        }
        if (isBlank(resource.getFileUrl())) {
            throw new BadRequestException("File URL is missing");
        }

        StorageFile storageFile = storageService.download(resource.getFileUrl());
        String mimeType = resolveResponseMimeType(resource.getMimeType(), storageFile.mimeType(), resource.getFileName());
        return new ResourceFileContent(
            resource.getId(),
            resource.getFileName(),
            mimeType,
            storageFile.content()
        );
    }

    public DocumentPreviewResponse documentPreview(Resource resource) {
        if (resource.getResourceType() != ResourceType.FILE) {
            throw new BadRequestException("Resource is not a file");
        }

        String extension = extensionOf(resource.getFileName());
        if (!extension.equals("docx")) {
            return new DocumentPreviewResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getFileName(),
                resource.getMimeType(),
                "",
                0,
                false,
                extension.equals("doc")
                    ? "Legacy .doc files cannot be rendered inline yet. Convert it to .docx for live preview."
                    : "Document preview is available for .docx files"
            );
        }

        ResourceFileContent file = fileContent(resource);
        List<String> paragraphs = extractDocxParagraphs(file.content());
        String plainText = String.join("\n\n", paragraphs);

        return new DocumentPreviewResponse(
            resource.getId(),
            resource.getTitle(),
            resource.getFileName(),
            file.mimeType(),
            plainText,
            paragraphs.size(),
            true,
            plainText.isBlank() ? "The DOCX file did not contain readable body text" : null
        );
    }

    private boolean isPreviewSupported(String mimeType, String fileName) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String extension = extensionOf(fileName);

        if (mime.startsWith("image/") || imageExtensions().contains(extension)) {
            return true;
        }

        if (mime.startsWith("text/") || textPreviewExtensions().contains(extension)) {
            return true;
        }

        return mime.equals("application/pdf") || mime.equals("application/json") || extension.equals("pdf") || extension.equals("docx");
    }

    private List<String> imageExtensions() {
        return List.of("jpg", "jpeg", "png", "webp", "gif", "svg");
    }

    private List<String> textPreviewExtensions() {
        return List.of(
            "txt", "md", "csv", "json", "xml", "yaml", "yml", "log",
            "java", "kt", "py", "ts", "tsx", "js", "jsx", "html", "css", "scss",
            "sql", "sh", "ps1", "c", "cpp", "h", "hpp", "cs", "go", "rs", "php",
            "rb", "swift", "dart"
        );
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveResponseMimeType(String resourceMimeType, String storageMimeType, String fileName) {
        String mime = firstNonBlank(resourceMimeType, storageMimeType);
        if (!isBlank(mime) && !"application/octet-stream".equalsIgnoreCase(mime)) {
            return mime.toLowerCase(Locale.ROOT);
        }

        return switch (extensionOf(fileName)) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "csv" -> "text/csv";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? trimToNull(second) : first.trim();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> extractDocxParagraphs(byte[] content) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return parseDocxDocumentXml(zip);
                }
            }
        } catch (java.io.IOException | XMLStreamException exception) {
            throw new BadRequestException("Could not render DOCX preview");
        }

        throw new BadRequestException("DOCX document body was not found");
    }

    private List<String> parseDocxDocumentXml(ZipInputStream zip) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        setXmlProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
        setXmlProperty(factory, "javax.xml.stream.isSupportingExternalEntities", false);

        XMLStreamReader reader = factory.createXMLStreamReader(zip);
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();
        boolean inParagraph = false;
        boolean inText = false;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                if ("p".equals(name)) {
                    inParagraph = true;
                    currentParagraph.setLength(0);
                } else if ("t".equals(name)) {
                    inText = true;
                } else if (inParagraph && "tab".equals(name)) {
                    currentParagraph.append('\t');
                } else if (inParagraph && "br".equals(name)) {
                    currentParagraph.append('\n');
                }
            } else if (event == XMLStreamConstants.CHARACTERS && inParagraph && inText) {
                currentParagraph.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String name = reader.getLocalName();
                if ("t".equals(name)) {
                    inText = false;
                } else if ("p".equals(name)) {
                    String paragraph = currentParagraph.toString().trim();
                    if (!paragraph.isBlank()) {
                        paragraphs.add(paragraph);
                    }
                    inParagraph = false;
                    inText = false;
                }
            }
        }

        return paragraphs;
    }

    private void setXmlProperty(XMLInputFactory factory, String property, boolean value) {
        try {
            factory.setProperty(property, value);
        } catch (IllegalArgumentException ignored) {
            // Some XML providers do not expose every hardening flag.
        }
    }
}