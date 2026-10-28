package com.aria.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({org.springframework.dao.DataAccessResourceFailureException.class,org.springframework.transaction.CannotCreateTransactionException.class})
    public ResponseEntity<Map<String,String>> handleStorageUnavailable(Exception exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After","30")
            .body(Map.of("message","Хранилище временно недоступно или пробуждается. Повторите запрос через 30 секунд"));
    }
    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<Map<String,String>> handleConcurrent(Exception ex){return ResponseEntity.status(409).body(Map.of("message","Данные уже изменены другим сотрудником. Обновите страницу и повторите действие"));}
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Запрос не выполнен":ex.getReason()));
    }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        Map<String,String> fields=new java.util.LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e->fields.putIfAbsent(e.getField(),e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message","Проверьте поля формы","fields",fields));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String,String>> handleMalformed(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message","Неверный формат значения или даты"));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,String>> handleIntegrity(Exception ex) {
        return ResponseEntity.status(409).body(Map.of("message","Запись связана с другими данными или содержит дубликат. Обновите страницу"));
    }

    // Обработка любых исключений, связанных с данными (товар не найден и т.д.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "error");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "conflict");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Обработка ошибок доступа к базе данных или бизнес-логики
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "server_error");
        error.put("message", "Внутренняя ошибка сервера. Попробуйте позже.");
        // Логируем реальную причину в консоль разработчика
        System.err.println("SERVER ERROR: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Обработка 404 для API
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoHandlerFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("status", "not_found");
        error.put("message", "Эндпоинт не существует");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
