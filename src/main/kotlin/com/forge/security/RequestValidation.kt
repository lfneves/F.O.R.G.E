package com.forge.security

import com.forge.core.Context
import com.forge.routing.Handler
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * Validation rule interface
 */
interface ValidationRule {
    fun validate(value: String): ValidationResult
    val name: String
}

/**
 * Validation result
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * Request validation configuration
 */
data class RequestValidationConfig(
    val enableXSSProtection: Boolean = true,
    val enableSQLInjectionProtection: Boolean = true,
    val enablePathTraversalProtection: Boolean = true,
    val enableHTMLSanitization: Boolean = true,
    val maxParameterLength: Int = 8192,
    val maxHeaderLength: Int = 8192,
    val maxQueryStringLength: Int = 8192,
    val allowedFileExtensions: Set<String> = setOf("jpg", "jpeg", "png", "gif", "pdf", "txt", "csv"),
    val deniedFileExtensions: Set<String> = setOf("exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js"),
    val customValidationRules: List<ValidationRule> = emptyList(),
    val onValidationFailure: ((Context, String) -> Unit)? = null
)

/**
 * XSS protection validation rule
 */
class XSSProtectionRule : ValidationRule {
    override val name = "XSS Protection"
    
    private val xssPatterns = listOf(
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<applet[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE)
    )
    
    override fun validate(value: String): ValidationResult {
        val decodedValue = try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
        
        for (pattern in xssPatterns) {
            if (pattern.matcher(decodedValue).find()) {
                return ValidationResult.Invalid("Potentially malicious content detected: XSS")
            }
        }
        
        return ValidationResult.Valid
    }
}

/**
 * SQL injection protection validation rule
 */
class SQLInjectionProtectionRule : ValidationRule {
    override val name = "SQL Injection Protection"
    
    private val sqlPatterns = listOf(
        Pattern.compile("('|(\\-\\-)|(;)|(\\|)|(\\*)|(%))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE){0,1}|INSERT( +INTO){0,1}|MERGE|SELECT|UPDATE|UNION( +ALL){0,1})\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(AND|OR)\\b.*(=|<|>|LIKE)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(CHAR|NCHAR|VARCHAR|NVARCHAR)\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(CAST|CONVERT)\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("0x[0-9a-fA-F]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(WAITFOR\\s+DELAY|BENCHMARK|SLEEP)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    override fun validate(value: String): ValidationResult {
        val decodedValue = try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
        
        for (pattern in sqlPatterns) {
            if (pattern.matcher(decodedValue).find()) {
                return ValidationResult.Invalid("Potentially malicious content detected: SQL Injection")
            }
        }
        
        return ValidationResult.Valid
    }
}

/**
 * Path traversal protection validation rule
 */
class PathTraversalProtectionRule : ValidationRule {
    override val name = "Path Traversal Protection"
    
    private val pathTraversalPatterns = listOf(
        Pattern.compile("\\.\\./", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\.\\\\", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%2e%2e%2f", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%2e%2e/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("..%2f", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%2e%2e%5c", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%2e%2e\\\\", Pattern.CASE_INSENSITIVE),
        Pattern.compile("..%5c", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%252e%252e%252f", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%c0%ae%c0%ae/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%c1%9c", Pattern.CASE_INSENSITIVE)
    )
    
    override fun validate(value: String): ValidationResult {
        val decodedValue = try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
        
        for (pattern in pathTraversalPatterns) {
            if (pattern.matcher(decodedValue).find()) {
                return ValidationResult.Invalid("Potentially malicious content detected: Path Traversal")
            }
        }
        
        return ValidationResult.Valid
    }
}

/**
 * File extension validation rule
 */
class FileExtensionValidationRule(
    private val allowedExtensions: Set<String>,
    private val deniedExtensions: Set<String>
) : ValidationRule {
    override val name = "File Extension Validation"
    
    override fun validate(value: String): ValidationResult {
        val extension = value.substringAfterLast('.', "").lowercase()
        
        if (extension.isNotEmpty()) {
            if (deniedExtensions.contains(extension)) {
                return ValidationResult.Invalid("File extension '$extension' is not allowed")
            }
            
            if (allowedExtensions.isNotEmpty() && !allowedExtensions.contains(extension)) {
                return ValidationResult.Invalid("File extension '$extension' is not in allowed list")
            }
        }
        
        return ValidationResult.Valid
    }
}

/**
 * Length validation rule
 */
class LengthValidationRule(
    private val maxLength: Int,
    private val fieldName: String = "field"
) : ValidationRule {
    override val name = "Length Validation"
    
    override fun validate(value: String): ValidationResult {
        return if (value.length > maxLength) {
            ValidationResult.Invalid("$fieldName exceeds maximum length of $maxLength characters")
        } else {
            ValidationResult.Valid
        }
    }
}

/**
 * HTML sanitization utility
 */
object HTMLSanitizer {
    private val dangerousTags = setOf(
        "script", "object", "embed", "applet", "meta", "link", "style", "iframe", "frame", "frameset"
    )
    
    private val dangerousAttributes = setOf(
        "onclick", "onload", "onerror", "onmouseover", "onmouseout", "onfocus", "onblur",
        "onkeydown", "onkeyup", "onkeypress", "onsubmit", "onreset", "onchange", "onselect"
    )
    
    fun sanitize(html: String): String {
        var sanitized = html
        
        // Remove dangerous tags
        for (tag in dangerousTags) {
            val pattern = Pattern.compile("<$tag[^>]*>.*?</$tag>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            sanitized = pattern.matcher(sanitized).replaceAll("")
            
            val selfClosingPattern = Pattern.compile("<$tag[^>]*/>", Pattern.CASE_INSENSITIVE)
            sanitized = selfClosingPattern.matcher(sanitized).replaceAll("")
        }
        
        // Remove dangerous attributes
        for (attr in dangerousAttributes) {
            val pattern = Pattern.compile("$attr\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE)
            sanitized = pattern.matcher(sanitized).replaceAll("")
        }
        
        // Remove javascript: and vbscript: protocols
        sanitized = sanitized.replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
        sanitized = sanitized.replace(Regex("vbscript:", RegexOption.IGNORE_CASE), "")
        
        return sanitized
    }
}

/**
 * Request validation middleware
 */
class RequestValidationMiddleware(
    private val config: RequestValidationConfig
) : Handler {
    
    private val validationRules = mutableListOf<ValidationRule>().apply {
        if (config.enableXSSProtection) add(XSSProtectionRule())
        if (config.enableSQLInjectionProtection) add(SQLInjectionProtectionRule())
        if (config.enablePathTraversalProtection) add(PathTraversalProtectionRule())
        add(FileExtensionValidationRule(config.allowedFileExtensions, config.deniedFileExtensions))
        addAll(config.customValidationRules)
    }
    
    override fun handle(ctx: Context) {
        val request = ctx.req()
        
        // Validate query string length
        val queryString = request.queryString
        if (queryString != null && queryString.length > config.maxQueryStringLength) {
            handleValidationFailure(ctx, "Query string exceeds maximum length")
            return
        }
        
        // Validate headers
        val headerNames = request.headerNames
        if (headerNames != null) {
            for (headerName in headerNames) {
                val headerValue = request.getHeader(headerName)
                if (headerValue != null) {
                    if (headerValue.length > config.maxHeaderLength) {
                        handleValidationFailure(ctx, "Header '$headerName' exceeds maximum length")
                        return
                    }
                    
                    for (rule in validationRules) {
                        when (val result = rule.validate(headerValue)) {
                            is ValidationResult.Invalid -> {
                                handleValidationFailure(ctx, "Header validation failed: ${result.message}")
                                return
                            }
                            ValidationResult.Valid -> continue
                        }
                    }
                }
            }
        }
        
        // Validate query parameters
        val parameterNames = request.parameterNames
        if (parameterNames != null) {
            for (paramName in parameterNames) {
                val paramValues = request.getParameterValues(paramName)
                if (paramValues != null) {
                    for (paramValue in paramValues) {
                        if (paramValue.length > config.maxParameterLength) {
                            handleValidationFailure(ctx, "Parameter '$paramName' exceeds maximum length")
                            return
                        }
                        
                        for (rule in validationRules) {
                            when (val result = rule.validate(paramValue)) {
                                is ValidationResult.Invalid -> {
                                    handleValidationFailure(ctx, "Parameter validation failed: ${result.message}")
                                    return
                                }
                                ValidationResult.Valid -> continue
                            }
                        }
                    }
                }
            }
        }
        
        // Validate path parameters
        val pathInfo = request.pathInfo
        if (pathInfo != null) {
            for (rule in validationRules) {
                when (val result = rule.validate(pathInfo)) {
                    is ValidationResult.Invalid -> {
                        handleValidationFailure(ctx, "Path validation failed: ${result.message}")
                        return
                    }
                    ValidationResult.Valid -> continue
                }
            }
        }
        
        // Sanitize request body if HTML sanitization is enabled
        if (config.enableHTMLSanitization) {
            sanitizeRequestBody(ctx)
        }
    }
    
    private fun sanitizeRequestBody(ctx: Context) {
        val request = ctx.req()
        val contentType = request.contentType
        
        if (contentType != null && (contentType.contains("text/html") || contentType.contains("application/x-www-form-urlencoded"))) {
            try {
                val body = request.reader.readText()
                if (body.isNotEmpty()) {
                    val sanitizedBody = HTMLSanitizer.sanitize(body)
                    // Note: In a real implementation, you would need to replace the request body
                    // This is a simplified example showing the sanitization logic
                    ctx.setAttribute("sanitized_body", sanitizedBody)
                }
            } catch (e: Exception) {
                // Handle body reading errors gracefully
            }
        }
    }
    
    private fun handleValidationFailure(ctx: Context, message: String) {
        if (config.onValidationFailure != null) {
            config.onValidationFailure.invoke(ctx, message)
        } else {
            ctx.status(400).json(mapOf(
                "error" to "Bad Request",
                "message" to "Request validation failed",
                "details" to message
            ))
        }
    }
}

/**
 * Request validation configuration builder
 */
class RequestValidationConfigBuilder {
    private var enableXSSProtection: Boolean = true
    private var enableSQLInjectionProtection: Boolean = true
    private var enablePathTraversalProtection: Boolean = true
    private var enableHTMLSanitization: Boolean = true
    private var maxParameterLength: Int = 8192
    private var maxHeaderLength: Int = 8192
    private var maxQueryStringLength: Int = 8192
    private var allowedFileExtensions: Set<String> = setOf("jpg", "jpeg", "png", "gif", "pdf", "txt", "csv")
    private var deniedFileExtensions: Set<String> = setOf("exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js")
    private var customValidationRules: List<ValidationRule> = emptyList()
    private var onValidationFailure: ((Context, String) -> Unit)? = null
    
    fun enableXSSProtection(enable: Boolean = true): RequestValidationConfigBuilder {
        enableXSSProtection = enable
        return this
    }
    
    fun enableSQLInjectionProtection(enable: Boolean = true): RequestValidationConfigBuilder {
        enableSQLInjectionProtection = enable
        return this
    }
    
    fun enablePathTraversalProtection(enable: Boolean = true): RequestValidationConfigBuilder {
        enablePathTraversalProtection = enable
        return this
    }
    
    fun enableHTMLSanitization(enable: Boolean = true): RequestValidationConfigBuilder {
        enableHTMLSanitization = enable
        return this
    }
    
    fun maxParameterLength(length: Int): RequestValidationConfigBuilder {
        maxParameterLength = length
        return this
    }
    
    fun maxHeaderLength(length: Int): RequestValidationConfigBuilder {
        maxHeaderLength = length
        return this
    }
    
    fun maxQueryStringLength(length: Int): RequestValidationConfigBuilder {
        maxQueryStringLength = length
        return this
    }
    
    fun allowedFileExtensions(vararg extensions: String): RequestValidationConfigBuilder {
        allowedFileExtensions = extensions.map { it.lowercase() }.toSet()
        return this
    }
    
    fun deniedFileExtensions(vararg extensions: String): RequestValidationConfigBuilder {
        deniedFileExtensions = extensions.map { it.lowercase() }.toSet()
        return this
    }
    
    fun addValidationRule(rule: ValidationRule): RequestValidationConfigBuilder {
        customValidationRules = customValidationRules + rule
        return this
    }
    
    fun onValidationFailure(handler: (Context, String) -> Unit): RequestValidationConfigBuilder {
        onValidationFailure = handler
        return this
    }
    
    fun build(): RequestValidationConfig {
        return RequestValidationConfig(
            enableXSSProtection = enableXSSProtection,
            enableSQLInjectionProtection = enableSQLInjectionProtection,
            enablePathTraversalProtection = enablePathTraversalProtection,
            enableHTMLSanitization = enableHTMLSanitization,
            maxParameterLength = maxParameterLength,
            maxHeaderLength = maxHeaderLength,
            maxQueryStringLength = maxQueryStringLength,
            allowedFileExtensions = allowedFileExtensions,
            deniedFileExtensions = deniedFileExtensions,
            customValidationRules = customValidationRules,
            onValidationFailure = onValidationFailure
        )
    }
}

/**
 * DSL for creating request validation configurations
 */
fun requestValidationConfig(block: RequestValidationConfigBuilder.() -> Unit): RequestValidationConfig {
    return RequestValidationConfigBuilder().apply(block).build()
}