package com.Lia.assistant.action

import org.json.JSONArray
import org.json.JSONObject

object FunctionDeclHelper {
    fun functionDecl(
        name: String,
        description: String,
        parameters: Map<String, String>,
        required: List<String> = emptyList()
    ): JSONObject {
        val root = JSONObject()
        root.put("name", name)
        root.put("description", description)

        val paramsObj = JSONObject()
        paramsObj.put("type", "OBJECT")

        val propertiesObj = JSONObject()
        for ((paramName, paramDesc) in parameters) {
            val fieldObj = JSONObject()
            fieldObj.put("type", "STRING")
            fieldObj.put("description", paramDesc)
            propertiesObj.put(paramName, fieldObj)
        }
        paramsObj.put("properties", propertiesObj)

        val reqArray = JSONArray()
        for (r in required) {
            reqArray.put(r)
        }
        paramsObj.put("required", reqArray)

        root.put("parameters", paramsObj)
        return root
    }
}
