package com.psd.controller.onlyOffice;

import com.psd.entity.Chapter;
import com.psd.repo.ChapterRepository;
import com.psd.service.jwt.JwtHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/onlyoffice")
public class OnlyOfficeController {

    private final ChapterRepository chapterRepository;
    private final JwtHelper jwtHelper;

    // Храним версию документа в памяти для быстрого доступа
    private static final Map<Long, Integer> DOCUMENT_VERSIONS = new ConcurrentHashMap<>();

    @Value("${portal.internal-url}")
    private String portalInternalUrl; // для OnlyOffice server-to-server (из контейнера)

    @Value("${portal.public-url}")
    private String portalPublicUrl;   // для браузера (внешний адрес)

    public OnlyOfficeController(ChapterRepository chapterRepository, JwtHelper jwtHelper) {
        this.chapterRepository = chapterRepository;
        this.jwtHelper = jwtHelper;
    }

    @GetMapping("/config/{id}")
    public Map<String, Object> getConfig(@PathVariable Long id, Principal principal) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        String fileName = Paths.get(chapter.getPath()).getFileName().toString();
        Integer currentVersion = (chapter.getVersion() == null ? 1 : chapter.getVersion());
        String documentKey = generateDocumentKey(id, currentVersion);

        // ✅ ВНУТРЕННИЕ URL для OnlyOffice (server-to-server)
        String fileUrl = portalInternalUrl + "/api/files/" + id;

        Map<String, Object> document = new HashMap<>();
        document.put("fileType", getExtension(fileName));
        document.put("key", documentKey);
        document.put("title", fileName);
        document.put("url", fileUrl + "?v=" + currentVersion + "&t=" + System.currentTimeMillis());

        Map<String, Object> user = new HashMap<>();
        user.put("id", principal.getName());
        user.put("name", principal.getName());

        Map<String, Object> customization = new HashMap<>();
        customization.put("forcesave", true);
        customization.put("autosave", true);

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("callbackUrl", portalInternalUrl + "/onlyoffice/callback/" + id);
        editorConfig.put("lang", "ru");
        editorConfig.put("user", user);
        editorConfig.put("customization", customization);

        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("editorConfig", editorConfig);

        String token = jwtHelper.createToken(config);
        config.put("token", token);

        return config;
    }


    // Генерация ключа на основе ID и версии
    private String generateDocumentKey(Long chapterId, Integer version) {
        return chapterId + "_v" + version;
    }

    @GetMapping("/editor")
    public String openEditor(@RequestParam("docId") Long docId, Model model) {
        Chapter chapter = chapterRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        model.addAttribute("chapter", chapter);
        return "document";
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex != -1) ? fileName.substring(dotIndex + 1) : "docx";
    }
}
