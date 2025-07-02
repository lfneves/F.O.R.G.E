package com.webframework.routing

data class Route(
    val method: String,
    val path: String,
    val handler: Handler
) {
    private val pathRegex: Regex
    private val paramNames: List<String>
    
    init {
        val (regex, params) = buildPathRegex(path)
        pathRegex = regex
        paramNames = params
    }
    
    fun matches(method: String, path: String): Boolean {
        return this.method.equals(method, ignoreCase = true) && pathRegex.matches(path)
    }
    
    fun extractPathParams(path: String): Map<String, String> {
        val matchResult = pathRegex.matchEntire(path) ?: return emptyMap()
        return paramNames.zip(matchResult.groupValues.drop(1)).toMap()
    }
    
    private fun buildPathRegex(path: String): Pair<Regex, List<String>> {
        val params = mutableListOf<String>()
        val regexPattern = path.replace(Regex(":([a-zA-Z0-9_]+)")) { matchResult ->
            params.add(matchResult.groupValues[1])
            "([^/]+)"
        }
        return Regex("^$regexPattern$") to params
    }
}