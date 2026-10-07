package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import com.simba.snip.npo.planning.persist.PlanningCellIntentEntity;
import com.simba.snip.npo.twin.CellParameterSimulationModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PlanningFingerprintService {

    public static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    public record IntentLine(int ordinal, String name, String cellId, String parameterId, BigDecimal intendedValue) {
    }

    public record AdmissionLine(
            int ordinal,
            String cellId,
            UUID twinId,
            Integer twinVersion,
            BigDecimal baseline,
            String configurationFingerprint,
            String modelId,
            String modelVersion
    ) {
    }

    public String intentFingerprint(UUID scenarioId, List<IntentLine> lines) {
        return requireSha256Hex(sha256Hex(intentCanonical(scenarioId, lines)));
    }

    public String intentCanonical(UUID scenarioId, List<IntentLine> lines) {
        List<IntentLine> ordered = new ArrayList<>(lines);
        ordered.sort(Comparator
                .comparingInt(IntentLine::ordinal)
                .thenComparing(IntentLine::cellId));
        StringBuilder sb = new StringBuilder();
        sb.append("schema=planning.intent.v1\n");
        sb.append("scenarioId=").append(scenarioId).append('\n');
        int lastOrdinal = -1;
        for (IntentLine line : ordered) {
            if (line.ordinal() != lastOrdinal) {
                sb.append("alternative.").append(line.ordinal()).append(".name=").append(line.name().trim()).append('\n');
                lastOrdinal = line.ordinal();
            }
            sb.append("alternative.").append(line.ordinal()).append(".intent.").append(line.cellId())
                    .append(".parameter=").append(line.parameterId()).append('\n');
            sb.append("alternative.").append(line.ordinal()).append(".intent.").append(line.cellId())
                    .append(".intended=").append(normalize(line.intendedValue())).append('\n');
        }
        sb.append("modelId=").append(CellParameterSimulationModel.MODEL_ID).append('\n');
        sb.append("modelVersion=").append(CellParameterSimulationModel.MODEL_VERSION);
        return sb.toString();
    }

    public String intentFingerprint(
            UUID scenarioId,
            List<PlanningAlternativeEntity> alternatives,
            List<PlanningCellIntentEntity> intents
    ) {
        Map<UUID, PlanningAlternativeEntity> byId = alternatives.stream()
                .collect(Collectors.toMap(PlanningAlternativeEntity::getId, a -> a));
        List<IntentLine> lines = new ArrayList<>();
        for (PlanningCellIntentEntity intent : intents) {
            PlanningAlternativeEntity alternative = byId.get(intent.getAlternativeId());
            if (alternative == null) {
                continue;
            }
            lines.add(new IntentLine(
                    alternative.getOrdinal(),
                    alternative.getName(),
                    intent.getCellId(),
                    intent.getParameterId(),
                    intent.getIntendedValue()
            ));
        }
        return intentFingerprint(scenarioId, lines);
    }

    public String admissionFingerprint(String intentFingerprint, List<AdmissionLine> lines) {
        return requireSha256Hex(sha256Hex(admissionCanonical(intentFingerprint, lines)));
    }

    public String admissionCanonical(String intentFingerprint, List<AdmissionLine> lines) {
        List<AdmissionLine> ordered = new ArrayList<>(lines);
        ordered.sort(Comparator.comparingInt(AdmissionLine::ordinal).thenComparing(AdmissionLine::cellId));
        StringBuilder sb = new StringBuilder();
        sb.append("schema=planning.admission.v1\n");
        sb.append("intentFingerprint=").append(intentFingerprint);
        for (AdmissionLine line : ordered) {
            sb.append('\n').append("item.").append(line.ordinal()).append('.').append(line.cellId())
                    .append(".twinId=").append(line.twinId() == null ? "-" : line.twinId().toString());
            sb.append('\n').append("item.").append(line.ordinal()).append('.').append(line.cellId())
                    .append(".twinVersion=").append(line.twinVersion() == null ? "-" : line.twinVersion().toString());
            sb.append('\n').append("item.").append(line.ordinal()).append('.').append(line.cellId())
                    .append(".baseline=").append(line.baseline() == null ? "-" : normalize(line.baseline()));
            sb.append('\n').append("item.").append(line.ordinal()).append('.').append(line.cellId())
                    .append(".config=").append(dash(line.configurationFingerprint()));
            sb.append('\n').append("item.").append(line.ordinal()).append('.').append(line.cellId())
                    .append(".modelId=").append(dash(line.modelId()));
            sb.append('\n').append("item.").append(line.ordinal()).append('.').append(line.cellId())
                    .append(".modelVersion=").append(dash(line.modelVersion()));
        }
        return sb.toString();
    }

    public static String configurationToken(String sourceContextVersion) {
        if (sourceContextVersion == null || sourceContextVersion.isBlank()) {
            return null;
        }
        if (sourceContextVersion.length() <= 128) {
            return sourceContextVersion;
        }
        return sha256Hex(sourceContextVersion);
    }

    public static String normalize(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String requireSha256Hex(String value) {
        if (value == null || !SHA256_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("fingerprint must be exactly 64 lowercase hexadecimal characters");
        }
        return value;
    }

    public static String sha256Hex(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
