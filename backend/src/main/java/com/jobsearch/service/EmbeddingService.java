package com.jobsearch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${embedding.dimension:384}")
    private int dimension;

    // Semantic cluster centroids to give true semantic affinity to related domain terms
    private static final Map<String, List<String>> SEMANTIC_CLUSTERS = new LinkedHashMap<>();

    static {
        SEMANTIC_CLUSTERS.put("healthcare_mobile", List.of(
                "ios", "react native", "swift", "kotlin", "mobile", "app", "apps",
                "hospital", "healthcare", "clinical", "patient", "medical", "ehr", "hipaa"
        ));
        SEMANTIC_CLUSTERS.put("frontend_web", List.of(
                "frontend", "ui", "ux", "react", "vue", "angular", "javascript", "typescript", "css", "html", "web"
        ));
        SEMANTIC_CLUSTERS.put("backend_cloud", List.of(
                "backend", "java", "spring", "golang", "go", "python", "microservices", "kubernetes", "k8s", "docker", "aws", "cloud", "api"
        ));
        SEMANTIC_CLUSTERS.put("data_ai", List.of(
                "data", "scientist", "analyst", "engineer", "machine learning", "ai", "artificial intelligence", "ml", "python", "pandas", "spark", "sql"
        ));
        SEMANTIC_CLUSTERS.put("finance_audit", List.of(
                "finance", "accounting", "cpa", "audit", "tax", "financial", "compliance", "sec", "sox"
        ));
        SEMANTIC_CLUSTERS.put("security_devops", List.of(
                "security", "devops", "sre", "clearance", "cybersecurity", "ci/cd", "terraform", "linux"
        ));
    }

    /**
     * Generates a 384-dimensional unit-normalized dense vector from arbitrary text.
     * Incorporates domain-specific semantic clustering so conceptual queries match related jobs.
     */
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return generateZeroVector();
        }

        float[] vector = new float[dimension];
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s+/.-]", " ");
        String[] tokens = normalized.split("\\s+");

        // 1. Semantic Cluster Projection
        int clusterOffset = 0;
        for (Map.Entry<String, List<String>> cluster : SEMANTIC_CLUSTERS.entrySet()) {
            int hits = 0;
            for (String term : cluster.getValue()) {
                if (normalized.contains(term)) {
                    hits++;
                }
            }
            if (hits > 0) {
                float clusterWeight = (float) Math.log(1 + hits) * 2.0f;
                int startIdx = (clusterOffset * 32) % dimension;
                for (int i = 0; i < 32 && (startIdx + i) < dimension; i++) {
                    vector[startIdx + i] += clusterWeight * (1.0f / (1.0f + i * 0.05f));
                }
            }
            clusterOffset++;
        }

        // 2. Subword & N-Gram Feature Hashing (dense latent space projection)
        for (String token : tokens) {
            if (token.isBlank() || token.length() < 2) continue;
            projectToken(token, 1.0f, vector);

            // Character 3-grams for typo & morphology invariance
            if (token.length() >= 3) {
                for (int i = 0; i <= token.length() - 3; i++) {
                    String tri = token.substring(i, i + 3);
                    projectToken(tri, 0.4f, vector);
                }
            }
        }

        // 3. Word bigrams
        for (int i = 0; i < tokens.length - 1; i++) {
            if (!tokens[i].isBlank() && !tokens[i + 1].isBlank()) {
                projectToken(tokens[i] + "_" + tokens[i + 1], 1.5f, vector);
            }
        }

        // 4. L2 Normalization (Cosine Similarity Requirement)
        normalizeL2(vector);

        List<Float> result = new ArrayList<>(dimension);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }

    private void projectToken(String token, float weight, float[] vector) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            int idx1 = Math.abs((hash[0] << 24) | ((hash[1] & 0xFF) << 16) | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF)) % dimension;
            int idx2 = Math.abs((hash[4] << 24) | ((hash[5] & 0xFF) << 16) | ((hash[6] & 0xFF) << 8) | (hash[7] & 0xFF)) % dimension;

            float sign1 = (hash[8] % 2 == 0) ? 1.0f : -1.0f;
            float sign2 = (hash[9] % 2 == 0) ? 1.0f : -1.0f;

            vector[idx1] += weight * sign1;
            vector[idx2] += weight * 0.5f * sign2;
        } catch (NoSuchAlgorithmException e) {
            int hash = Math.abs(token.hashCode()) % dimension;
            vector[hash] += weight;
        }
    }

    private void normalizeL2(float[] vector) {
        float sumSq = 0.0f;
        for (float val : vector) {
            sumSq += val * val;
        }
        if (sumSq > 0.0f) {
            float norm = (float) Math.sqrt(sumSq);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    private List<Float> generateZeroVector() {
        List<Float> zero = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            zero.add(0.0f);
        }
        return zero;
    }
}
