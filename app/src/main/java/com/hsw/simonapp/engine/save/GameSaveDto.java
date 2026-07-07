package com.hsw.simonapp.engine.save;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class GameSaveDto {

    static final int SUPPORTED_SCHEMA_VERSION = 1;

    final int schemaVersion;
    final String worldId;
    final List<SavedEntityDto> entities;

    GameSaveDto(int schemaVersion, String worldId, List<SavedEntityDto> entities) {
        this.schemaVersion = schemaVersion;
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.entities = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(entities, "entities")));
    }
}
