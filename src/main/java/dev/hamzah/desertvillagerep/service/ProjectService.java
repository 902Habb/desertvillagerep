package dev.hamzah.desertvillagerep.service;

import dev.hamzah.desertvillagerep.model.ProjectDefinition;
import java.util.Collection;
import java.util.UUID;

public final class ProjectService {
    public enum CompletionResult {
        COMPLETED,
        PROJECT_NOT_FOUND,
        ALREADY_COMPLETED
    }

    private final Database database;
    private final RepService repService;

    public ProjectService(Database database, RepService repService) {
        this.database = database;
        this.repService = repService;
    }

    public void saveProject(ProjectDefinition definition) {
        database.saveProject(definition);
    }

    public ProjectDefinition getProject(String id) {
        return database.getProject(id);
    }

    public Collection<String> projectIds() {
        return database.getProjectIds();
    }

    public CompletionResult completeProject(String id, UUID targetUuid, String targetName, UUID actorUuid) {
        ProjectDefinition project = getProject(id);
        if (project == null) {
            return CompletionResult.PROJECT_NOT_FOUND;
        }
        if (!database.markProjectCompleted(project.id(), targetUuid)) {
            return CompletionResult.ALREADY_COMPLETED;
        }
        repService.changeRep(targetUuid, targetName, project.category(), project.points(), actorUuid, "Project: " + project.id());
        return CompletionResult.COMPLETED;
    }
}

