// upload.js - только для загрузки файлов глав
let currentXHR = null;

document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('file');
    const fileInputLabel = document.getElementById('fileInputLabel');
    const fileInfo = document.getElementById('fileInfo');
    const fileName = document.getElementById('fileName');
    const fileSize = document.getElementById('fileSize');
    const uploadFormContainer = document.getElementById('uploadFormContainer');
    const cancelBtn = document.getElementById('cancelBtn');

    // 1. Автозагрузка при выборе файла
    fileInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            displayFileInfo(file);
            setTimeout(() => {
                startUpload(file);
            }, 500);
        }
    });

    // 2. Drag & Drop функционал
    uploadFormContainer.addEventListener('dragover', function(e) {
        e.preventDefault();
        e.stopPropagation();
        uploadFormContainer.classList.add('drag-over');
    });

    uploadFormContainer.addEventListener('dragleave', function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (!uploadFormContainer.contains(e.relatedTarget)) {
            uploadFormContainer.classList.remove('drag-over');
        }
    });

    uploadFormContainer.addEventListener('drop', function(e) {
        e.preventDefault();
        e.stopPropagation();
        uploadFormContainer.classList.remove('drag-over');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            const file = files[0];
            fileInput.files = files;
            displayFileInfo(file);
            setTimeout(() => {
                startUpload(file);
            }, 500);
        }
    });

    // 3. Отображение информации о файле
    function displayFileInfo(file) {
        const sizeInMB = (file.size / (1024 * 1024)).toFixed(2);
        fileName.textContent = file.name;
        fileSize.textContent = `${sizeInMB} MB`;
        fileInfo.style.display = 'block';
        fileInputLabel.textContent = `📁 ${file.name}`;
        fileInputLabel.style.background = '#d4edda';
        fileInputLabel.style.borderColor = '#c3e6cb';
    }

    // 4. Кнопка отмены
    cancelBtn.addEventListener('click', function() {
        if (currentXHR) {
            currentXHR.abort();
            resetForm();
            showMessage('Загрузка отменена', 'info');
        }
    });
});

// Функция начала загрузки
function startUpload(file) {
    const generalId = document.getElementById('generalId').value;
    const progressContainer = document.getElementById('progressContainer');
    const cancelBtn = document.getElementById('cancelBtn');

    if (!generalId) {
        alert('Ошибка: General ID не указан');
        return;
    }

    // Показываем прогресс-бар и кнопку отмены
    progressContainer.style.display = 'block';
    cancelBtn.style.display = 'block';

    try {
        updateProgress(10, 'Подготовка файла...');

        const formData = new FormData();
        formData.append('file', file);
        formData.append('generalId', generalId);

        currentXHR = new XMLHttpRequest();

        // Отслеживаем прогресс загрузки
        currentXHR.upload.addEventListener('progress', function(e) {
            if (e.lengthComputable) {
                const percentComplete = (e.loaded / e.total) * 100;
                updateProgress(10 + percentComplete * 0.6, `Загрузка на сервер: ${Math.round(percentComplete)}%`);
            }
        });

        // Обрабатываем ответ
        currentXHR.addEventListener('load', function() {
            try {
                const result = JSON.parse(currentXHR.responseText);

                // Проверяем наличие ошибки в ответе независимо от статуса
                if (result.error) {
                    handleError(result.error);
                    return;
                }

                // Если статус 200 и нет ошибки - успех
                if (currentXHR.status === 200) {
                    updateProgress(100, 'Загрузка завершена!');
                    showSuccessNotification();
                    currentXHR = null;
                    cancelBtn.style.display = 'none';
                } else {
                    // Другие успешные статусы (например, 201 Created)
                    updateProgress(100, 'Загрузка завершена!');
                    showSuccessNotification();
                    currentXHR = null;
                    cancelBtn.style.display = 'none';
                }

            } catch (parseError) {
                // Если не удалось распарсить JSON, показываем общую ошибку
                if (currentXHR.status === 200) {
                    handleError('Ошибка обработки ответа сервера');
                } else {
                    handleError(`Ошибка сервера: ${currentXHR.status} - ${currentXHR.statusText}`);
                }
            }
        });

        // Обрабатываем ошибки
        currentXHR.addEventListener('error', function() {
            handleError('Ошибка сети');
        });

        currentXHR.addEventListener('timeout', function() {
            handleError('Таймаут запроса');
        });

        currentXHR.addEventListener('abort', function() {
            updateProgress(0, 'Загрузка отменена');
            setTimeout(() => {
                progressContainer.style.display = 'none';
            }, 2000);
        });

        // Отправляем запрос
        currentXHR.open('POST', '/api/yandex-disk/upload/template');
        currentXHR.timeout = 60000; // 60 секунд таймаут
        currentXHR.send(formData);

    } catch (error) {
        handleError(`Ошибка: ${error.message}`);
    }
}

// Функция сброса формы
function resetForm() {
    const fileInput = document.getElementById('file');
    const fileInputLabel = document.getElementById('fileInputLabel');
    const fileInfo = document.getElementById('fileInfo');
    const progressContainer = document.getElementById('progressContainer');
    const cancelBtn = document.getElementById('cancelBtn');

    fileInput.value = '';
    fileInputLabel.textContent = '📁 Выберите файл или перетащите его сюда';
    fileInputLabel.style.background = '#f8f9fa';
    fileInputLabel.style.borderColor = '#dee2e6';
    fileInfo.style.display = 'none';
    progressContainer.style.display = 'none';
    cancelBtn.style.display = 'none';
    currentXHR = null;
}

function updateProgress(percent, text) {
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');

    progressFill.style.width = percent + '%';
    progressFill.textContent = Math.round(percent) + '%';
    progressText.textContent = text;

    if (percent < 30) {
        progressFill.style.background = 'linear-gradient(90deg, #ff4444, #ff6b6b)';
    } else if (percent < 70) {
        progressFill.style.background = 'linear-gradient(90deg, #ffa726, #ffb74d)';
    } else {
        progressFill.style.background = 'linear-gradient(90deg, #4CAF50, #45a049)';
    }
}

function showSuccessNotification() {
    alert('Файл успешно загружен! Страница перезагрузится через 3 секунды...');
    setTimeout(() => {
        location.reload();
    }, 3000);
}

function handleError(message) {
    const progressText = document.getElementById('progressText');
    const cancelBtn = document.getElementById('cancelBtn');

    progressText.textContent = 'Ошибка!';
    progressText.style.color = 'red';
    cancelBtn.style.display = 'none';

    setTimeout(() => {
        resetForm();
    }, 3000);

    alert(message);
}

function showMessage(message, type) {
    console.log(`${type}: ${message}`);
}
// Обработчики для кнопок шаблонов разделов
document.addEventListener('DOMContentLoaded', function() {
    // Копирование шаблона
    document.querySelectorAll('.copy-template-btn').forEach(button => {
        button.addEventListener('click', function() {
            const sectionId = this.getAttribute('data-section-id');
            const form = document.getElementById('copy-template-form-' + sectionId);
            if (form) {
                form.submit();
            }
        });
    });

    // Удаление шаблона
    document.querySelectorAll('.delete-template-btn').forEach(button => {
        button.addEventListener('click', function() {
            const sectionId = this.getAttribute('data-section-id');
            if (confirm('Удалить шаблон раздела?')) {
                const form = document.getElementById('delete-template-form-' + sectionId);
                if (form) {
                    form.submit();
                }
            }
        });
    });
});