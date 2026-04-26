package com.psd.service;

import com.psd.entity.Chapter;
import com.psd.entity.Project;
import com.psd.entity.Section;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class LocalFileService {

    @Value("${file.storage.path}")
    private String storagePath;

    @PostConstruct
    public void init() {
        try {
            // Создаем структуру папок как в Яндекс.Диске
            Files.createDirectories(Paths.get(storagePath, "portal", "projects"));
            Files.createDirectories(Paths.get(storagePath, "portal", "templates"));
            log.info("Локальное хранилище инициализировано: {}", storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директории хранилища", e);
        }
    }

    /**
     * Сохраняет файл в структуру portal/projects/{projectId}/
     */
    public String saveProjectFile(MultipartFile file, Long projectId) {
        try {
            String relativePath = "portal/projects/" + projectId;
            return saveFile(file, relativePath);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения файла проекта: " + e.getMessage(), e);
        }
    }
    /**
     * Сохраняет файл в структуру portal/projects/{projectId}/
     */
    public String saveProjectFileResult(MultipartFile file, Long projectId) {
        try {
            String relativePath = "portal/projects/" + projectId + "/result";
            return saveFile(file, relativePath);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения файла проекта: " + e.getMessage(), e);
        }
    }

    /**
     * Сохраняет файл в структуру portal/templates/{generalId}/
     */
    public String saveTemplateFile(MultipartFile file, Long generalId) {
        try {
            String relativePath = "portal/templates/" + generalId;
            return saveFile(file, relativePath);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения файла шаблона: " + e.getMessage(), e);
        }
    }

    public String saveTemplateSectionFile(MultipartFile file, Long generalId, Long sectionId) {
        try {
            String relativePath = "portal/templates/" + generalId + "/" + sectionId;
            return saveFile(file, relativePath);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения файла шаблона секции: " + e.getMessage(), e);
        }
    }

    /**
     * Универсальный метод сохранения файла
     */
    private String saveFile(MultipartFile file, String relativePath) throws IOException {
        // ИСПРАВЛЕНИЕ: сохраняем оригинальное имя файла с поддержкой кириллицы
        String fileName = preserveOriginalFileNameWithUnicode(file.getOriginalFilename());
        Path filePath = Paths.get(storagePath, relativePath, fileName);

        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fullRelativePath = relativePath + "/" + fileName;
        log.info("Файл сохранен: {}", fullRelativePath);
        return fullRelativePath;
    }

    /**
     * Сохраняет оригинальное имя файла с поддержкой кириллицы и Unicode
     */
    private String preserveOriginalFileNameWithUnicode(String originalFileName) {
        if (originalFileName == null) {
            return "file_" + System.currentTimeMillis();
        }

        // Убираем только действительно опасные символы для файловой системы
        // Кириллица и другие Unicode символы остаются
        String safeName = originalFileName
                .replaceAll("[\\\\/:*?\"<>|]", "_") // Заменяем только системные запрещенные символы
                .replaceAll("\\s+", " ") // Заменяем множественные пробелы на один
                .trim(); // Убираем пробелы в начале и конце

        // Проверяем, не осталось ли имя пустым после очистки
        if (safeName.isEmpty()) {
            return "file_" + System.currentTimeMillis();
        }

        // Обрезаем слишком длинные имена (максимум 255 символов)
        if (safeName.length() > 255) {
            int dotIndex = safeName.lastIndexOf(".");
            if (dotIndex > 0) {
                String name = safeName.substring(0, Math.min(dotIndex, 200));
                String extension = safeName.substring(dotIndex);
                safeName = name + extension;
            } else {
                safeName = safeName.substring(0, 255);
            }
        }

        log.debug("Оригинальное имя файла: '{}', безопасное имя: '{}'", originalFileName, safeName);
        return safeName;
    }

    /**
     * Загружает файл как Resource
     */
    public Resource loadFile(String relativePath) {
        try {
            Path filePath = Paths.get(storagePath, relativePath);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Файл не найден: " + relativePath);
            }
            return new FileSystemResource(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки файла: " + e.getMessage(), e);
        }
    }

    /**
     * Копирует файл из шаблона в проект
     */
    public String copyTemplateToProject(String templateFilePath, Long projectId) {
        try {
            // Из пути шаблона извлекаем имя файла
            String fileName = extractFileNameFromPath(templateFilePath);
            String targetPath = "portal/projects/" + projectId + "/" + fileName;

            Path source = Paths.get(storagePath, templateFilePath);
            Path target = Paths.get(storagePath, targetPath);

            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Файл скопирован из шаблона в проект: {} -> {}", templateFilePath, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка копирования файла из шаблона в проект: " + e.getMessage(), e);
        }
    }
    /**
     * Копирует файл таблицы из шаблона в проект
     */
    public String copyTemplateTableToProject(String templateFilePath, Long projectId, Long id) {
        try {
            // Из пути шаблона извлекаем имя файла
            String fileName = extractFileNameFromPath(templateFilePath);
            String targetPath = "portal/projects/" + projectId + "/" + id + "/" + fileName;

            Path source = Paths.get(storagePath, templateFilePath);
            Path target = Paths.get(storagePath, targetPath);

            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Файл скопирован из шаблона в проект: {} -> {}", templateFilePath, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка копирования файла из шаблона в проект: " + e.getMessage(), e);
        }
    }


    /**
     * Проверяет существование файла
     */
    public boolean fileExists(String relativePath) {
        return Files.exists(Paths.get(storagePath, relativePath));
    }

    /**
     * Удаляет файл
     */
    public void deleteFile(String relativePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath, relativePath));
            log.info("Файл удален: {}", relativePath);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка удаления файла: " + e.getMessage(), e);
        }
    }


    /**
     * Извлекает имя файла из пути
     */
    private String extractFileNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "file_" + System.currentTimeMillis();
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
            return path.substring(lastSlashIndex + 1);
        }
        return path;
    }

    public void createProjectFolder(Long projectId) {
        try {
            String projectPath = "portal/projects/" + projectId;
            Path folderPath = Paths.get(storagePath, projectPath);

            Files.createDirectories(folderPath);
            log.info("Папка проекта создана: {}", folderPath.toAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку проекта: " + e.getMessage(), e);
        }
    }

    /**
     * Создает папку для шаблона (General)
     */
    public void createTemplateFolder(Long generalId) {
        try {
            String templatePath = "portal/templates/" + generalId;
            Path folderPath = Paths.get(storagePath, templatePath);

            Files.createDirectories(folderPath);
            log.info("Папка шаблона создана: {}", folderPath.toAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку шаблона: " + e.getMessage(), e);
        }
    }

    public String copyStamp(Project project, Section section) {
        try {
            String sourcePath = "portal/stamp.xlsx";
            String targetPath = "portal/projects/" + project.getId() + "/stamp/"+ section.getId() +"/stamp.xlsx";

            Path source = Paths.get(storagePath, sourcePath);
            Path target = Paths.get(storagePath, targetPath);

            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Файл штампа скопирован из шаблона в проект: {} -> {}", sourcePath, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка копирования файла штампа из шаблона в проект: " + e.getMessage(), e);
        }
    }

    public String copyTemplateSampleToProject(Chapter templateFile, Long projectId, Long sectionId) {
        try {
            String sourcePath = templateFile.getPath();
            String targetPath = "portal/projects/" + projectId + "/sample/" + sectionId + "/" + templateFile.getName();

            Path source = Paths.get(storagePath, sourcePath);
            Path target = Paths.get(storagePath, targetPath);

            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Файл шаблона раздела скопирован из шаблона в проект: {} -> {}", sourcePath, targetPath);
            return targetPath;
            } catch (IOException e) {
                throw new RuntimeException("Ошибка копирования файла шаблона раздела  в проект: " + e.getMessage(), e);
        }
    }

    public void deleteIfExists(String path) {
        if (path == null || path.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (Exception e) {
            log.warn("deleteIfExists failed path={} err={}", path, e.getMessage());
        }
    }


    public String saveSectionAttachment(MultipartFile file, Long projectId, Long sectionId) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            String original = sanitizeFileName(file.getOriginalFilename()); // у тебя уже есть sanitize
             // timestamp-based уникальность (достаточно надёжно и читабельно)
            String ts = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String storedName = ts + "_" + original;

            String relativeDir = "portal/projects/" + projectId + "/" + sectionId + "/files";
            Path uploadDir = Paths.get(storagePath).resolve(relativeDir);
            Files.createDirectories(uploadDir);

            Path target = uploadDir.resolve(storedName);

            // на всякий случай избегаем коллизии даже при одном ts
            int n = 1;
            while (Files.exists(target)) {
                String alt = ts + "_" + n + "_" + original;
                target = uploadDir.resolve(alt);
                storedName = alt;
                n++;
            }

            Files.copy(file.getInputStream(), target); // без REPLACE
            return relativeDir + "/" + storedName;

        } catch (IOException e) {
            throw new RuntimeException("Could not save section attachment file", e);
        }
    }

    public void deleteByRelativePath(String relativePath) {
        // твой deleteFile() уже удаляет по relativePath
        deleteFile(relativePath);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }

        // 1. Убираем путь (если кто-то передал C:\... или ../../)
        fileName = Paths.get(fileName).getFileName().toString();

        // 2. Заменяем запрещённые символы
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 3. Убираем управляющие символы
        fileName = fileName.replaceAll("[\\p{Cntrl}]", "");

        // 4. Убираем повторяющиеся пробелы
        fileName = fileName.replaceAll("\\s+", " ").trim();

        // 5. Ограничим длину (файловые системы могут падать >255)
        int maxLength = 200;
        if (fileName.length() > maxLength) {
            String ext = "";
            int dot = fileName.lastIndexOf('.');
            if (dot > 0) {
                ext = fileName.substring(dot);
                fileName = fileName.substring(0, dot);
            }
            fileName = fileName.substring(0, Math.min(fileName.length(), maxLength - ext.length())) + ext;
        }

        return fileName;
    }
    void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void restoreChapterFileFromGeneral(String sourceRelativePath, String targetRelativePath) {
        if (sourceRelativePath == null || sourceRelativePath.isBlank()) {
            throw new RuntimeException("Путь к исходному файлу пустой");
        }
        if (targetRelativePath == null || targetRelativePath.isBlank()) {
            throw new RuntimeException("Путь к целевому файлу пустой");
        }

        try {
            Path source = Paths.get(storagePath, sourceRelativePath);
            Path target = Paths.get(storagePath, targetRelativePath);

            if (!Files.exists(source)) {
                throw new RuntimeException("Исходный файл не найден: " + sourceRelativePath);
            }

            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            log.info("Глава восстановлена из general: {} -> {}", sourceRelativePath, targetRelativePath);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка восстановления файла главы: " + e.getMessage(), e);
        }
    }
}