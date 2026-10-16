package de.mertendieckmann.griplbackend.model.dto

// TODO Add Description
// TODO Allow full URLS, not just relative endpoints
// TODO Show description in frontend

data class AnalysisEndpoint(
    val name: String,
    val endpoint: String,
    val responseType: CustomAnalysisResponseType
)
