package com.simba.snip.npo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snip")
public class SnipProperties {

    private String corpusDir = "testdata/corpus";
    private String kpiFile = "testdata/kpis.json";
    /** {@code stub} for CI. {@code spring-ai} for the local LLM path. */
    private String generator = "stub";
    /** {@code lexical} (default) or {@code vector}. */
    private String retrievalMode = "lexical";
    private int retrieveTopK = 3;
    /** Minimum vector similarity (0–1). Ignored by lexical retrieval. */
    private double retrieveMinScore = 0.45;
    private String chatModel = "qwen2.5:7b";
    private String embeddingModel = "nomic-embed-text";
    /** KPI observations older than this window are omitted from reasoning context. */
    private int recentKpiHours = 168;
    private int recentKpiLimit = 20;
    /** Last-N observations per metric in temporal context. */
    private int telemetryHistoryN = 5;
    /** When false, no Kafka listener is registered (CI / default). */
    private boolean kafkaEnabled = false;
    private String telemetryTopic = "snip.telemetry.cell-kpi.v1";
    private String telemetryDlqTopic = "snip.telemetry.cell-kpi.dlq.v1";
    private int kafkaRetryAttempts = 2;
    private long kafkaRetryIntervalMs = 200L;
    /** BLER_DL warning threshold (ratio). Detection also requires INCREASING trend. */
    private double assuranceBlerDlThreshold = 0.08;
    private double assuranceBlerDlMajorThreshold = 0.10;
    private double assuranceBlerDlCriticalThreshold = 0.12;

    public String getCorpusDir() {
        return corpusDir;
    }

    public void setCorpusDir(String corpusDir) {
        this.corpusDir = corpusDir;
    }

    public String getKpiFile() {
        return kpiFile;
    }

    public void setKpiFile(String kpiFile) {
        this.kpiFile = kpiFile;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getRetrievalMode() {
        return retrievalMode;
    }

    public void setRetrievalMode(String retrievalMode) {
        this.retrievalMode = retrievalMode;
    }

    public int getRetrieveTopK() {
        return retrieveTopK;
    }

    public void setRetrieveTopK(int retrieveTopK) {
        this.retrieveTopK = retrieveTopK;
    }

    public double getRetrieveMinScore() {
        return retrieveMinScore;
    }

    public void setRetrieveMinScore(double retrieveMinScore) {
        this.retrieveMinScore = retrieveMinScore;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getRecentKpiHours() {
        return recentKpiHours;
    }

    public void setRecentKpiHours(int recentKpiHours) {
        this.recentKpiHours = recentKpiHours;
    }

    public int getRecentKpiLimit() {
        return recentKpiLimit;
    }

    public void setRecentKpiLimit(int recentKpiLimit) {
        this.recentKpiLimit = recentKpiLimit;
    }

    public int getTelemetryHistoryN() {
        return telemetryHistoryN;
    }

    public void setTelemetryHistoryN(int telemetryHistoryN) {
        this.telemetryHistoryN = telemetryHistoryN;
    }

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public void setKafkaEnabled(boolean kafkaEnabled) {
        this.kafkaEnabled = kafkaEnabled;
    }

    public String getTelemetryTopic() {
        return telemetryTopic;
    }

    public void setTelemetryTopic(String telemetryTopic) {
        this.telemetryTopic = telemetryTopic;
    }

    public String getTelemetryDlqTopic() {
        return telemetryDlqTopic;
    }

    public void setTelemetryDlqTopic(String telemetryDlqTopic) {
        this.telemetryDlqTopic = telemetryDlqTopic;
    }

    public int getKafkaRetryAttempts() {
        return kafkaRetryAttempts;
    }

    public void setKafkaRetryAttempts(int kafkaRetryAttempts) {
        this.kafkaRetryAttempts = kafkaRetryAttempts;
    }

    public long getKafkaRetryIntervalMs() {
        return kafkaRetryIntervalMs;
    }

    public void setKafkaRetryIntervalMs(long kafkaRetryIntervalMs) {
        this.kafkaRetryIntervalMs = kafkaRetryIntervalMs;
    }

    public double getAssuranceBlerDlThreshold() {
        return assuranceBlerDlThreshold;
    }

    public void setAssuranceBlerDlThreshold(double assuranceBlerDlThreshold) {
        this.assuranceBlerDlThreshold = assuranceBlerDlThreshold;
    }

    public double getAssuranceBlerDlMajorThreshold() {
        return assuranceBlerDlMajorThreshold;
    }

    public void setAssuranceBlerDlMajorThreshold(double assuranceBlerDlMajorThreshold) {
        this.assuranceBlerDlMajorThreshold = assuranceBlerDlMajorThreshold;
    }

    public double getAssuranceBlerDlCriticalThreshold() {
        return assuranceBlerDlCriticalThreshold;
    }

    public void setAssuranceBlerDlCriticalThreshold(double assuranceBlerDlCriticalThreshold) {
        this.assuranceBlerDlCriticalThreshold = assuranceBlerDlCriticalThreshold;
    }
}
