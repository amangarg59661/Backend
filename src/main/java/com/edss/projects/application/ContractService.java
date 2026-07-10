package com.edss.projects.application;

import com.edss.projects.domain.Contract;
import com.edss.projects.domain.events.ProjectEvents;
import com.edss.projects.infrastructure.ContractRepository;
import com.edss.projects.infrastructure.ProjectRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.events.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contracts are downloadable at any time — every version (unsigned + signed)
 * lives forever. Storage keys come from the knowledge module's file-upload
 * flow; here we only track metadata + SHA-256 for tamper evidence.
 */
@Service
@Transactional
public class ContractService {

    private final ProjectRepository projects;
    private final ContractRepository contracts;
    private final OutboxWriter outbox;
    private final Clock clock;

    public ContractService(
            ProjectRepository projects,
            ContractRepository contracts,
            OutboxWriter outbox,
            Clock clock) {
        this.projects = projects;
        this.contracts = contracts;
        this.outbox = outbox;
        this.clock = clock;
    }

    public Contract register(
            UUID projectId,
            Contract.Kind kind,
            String storageKey,
            String sha256,
            UUID uploaderUserId) {
        if (projects.findById(projectId).isEmpty()) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "Project not found.");
        }
        Instant now = clock.instant();
        Contract row =
                new Contract(
                        UUID.randomUUID(),
                        projectId,
                        kind,
                        storageKey,
                        sha256,
                        uploaderUserId,
                        now);
        contracts.save(row);
        outbox.append(
                "projects",
                new ProjectEvents.ContractUploaded(
                        UUID.randomUUID(), now, projectId, row.getId(), kind.wire(), uploaderUserId),
                Map.of(
                        "project_id", projectId,
                        "contract_id", row.getId(),
                        "kind", kind.wire(),
                        "uploaded_by_user_id", uploaderUserId));
        return row;
    }

    @Transactional(readOnly = true)
    public List<Contract> list(UUID projectId) {
        return contracts.findByProjectIdOrderByUploadedAtDesc(projectId);
    }
}
