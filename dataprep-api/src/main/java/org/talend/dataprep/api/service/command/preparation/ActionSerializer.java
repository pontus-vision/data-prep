package org.talend.dataprep.api.service.command.preparation;

import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.talend.dataprep.api.preparation.Action;

final class ActionSerializer {

    private ActionSerializer() {
    }

    /**
     * Serialize the actions to string.
     *
     * @param stepActions - map of couple (stepId, action)
     * @return the serialized actions
     */
    static String serializeActions(ObjectMapper mapper, final Collection<Action> stepActions) throws JsonProcessingException {
        return mapper
                .writer() //
                .withRootName("actions") //
                .writeValueAsString(stepActions);
    }
}
