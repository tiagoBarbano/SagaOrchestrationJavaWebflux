package com.saga.orchestration.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Abstração para sanitização de dados que pode ser estendida para diferentes tipos
 * Segue o princípio SRP - responsabilidade única de sanitização
 */
@Component
public class DataSanitizer {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|SCRIPT|JAVASCRIPT|ONLOAD|ONERROR|ONCLICK)"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|vbscript:|onload|onerror|onclick|onmouseover|onfocus|onblur)"
    );
    
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    
    /**
     * Sanitiza texto removendo caracteres perigosos
     */
    public String sanitizeText(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        String sanitized = input
            .replaceAll("['\";]", "") // Remove aspas e ponto e vírgula
            .replaceAll("\\\\", "")   // Remove barras invertidas
            .trim();
            
        return sanitized;
    }
    
    /**
     * Sanitiza texto removendo HTML tags
     */
    public String sanitizeHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        return HTML_TAG_PATTERN.matcher(input).replaceAll("");
    }
    
    /**
     * Sanitiza texto removendo XSS
     */
    public String sanitizeXss(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        return XSS_PATTERN.matcher(input).replaceAll("");
    }
    
    /**
     * Sanitiza texto removendo SQL Injection
     */
    public String sanitizeSql(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        return SQL_INJECTION_PATTERN.matcher(input).replaceAll("");
    }
    
    /**
     * Sanitização completa - aplica todas as sanitizações
     */
    public String sanitizeComplete(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        
        return sanitizeText(sanitizeHtml(sanitizeXss(sanitizeSql(input))));
    }
    
    /**
     * Sanitiza email removendo caracteres inválidos
     */
    public String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        
        return email.toLowerCase().trim();
    }
    
    /**
     * Sanitiza telefone removendo caracteres não numéricos
     */
    public String sanitizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        
        return phone.replaceAll("[^0-9+]", "");
    }
} 