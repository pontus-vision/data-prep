// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.service.settings.actions.provider;

import org.talend.dataprep.api.service.settings.actions.api.ActionSettings;

import java.util.ArrayList;

import static org.talend.dataprep.api.service.settings.actions.api.ActionDropdownSettings.dropdownBuilder;
import static org.talend.dataprep.api.service.settings.actions.api.ActionSettings.*;

/**
 * Actions that triggers windows (modal, new tab, ...) settings
 */
// @formatter:off
public interface WindowActions {
    ActionSettings ONBOARDING_PREPARATION = builder()
            .id("onboarding:preparation")
            .name("onboarding.preparation")
            .icon("talend-board")
            .type("@@onboarding/START_TOUR")
            .payload(PAYLOAD_METHOD_KEY, "startTour")
            .payload(PAYLOAD_ARGS_KEY, new String[]{"preparation"})
            .build();

    ActionSettings DIVIDER = builder()
            .id("divider")
            .divider(true)
            .build();

    ActionSettings ONBOARDING_PLAYGROUND = builder()
            .id("onboarding:playground")
            .name("onboarding.playground")
            .icon("talend-board")
            .type("@@onboarding/START_TOUR")
            .payload(PAYLOAD_METHOD_KEY, "startTour")
            .payload(PAYLOAD_ARGS_KEY, new String[]{"playground"})
            .build();

    ActionSettings MODAL_ABOUT = builder()
            .id("modal:about")
            .name("modal.about")
            .icon("talend-info-circle")
            .type("@@modal/SHOW")
            .payload(PAYLOAD_METHOD_KEY, "toggleAbout")
            .build();

    ActionSettings MODAL_FEEDBACK = builder()
            .id("modal:feedback")
            .name("modal.feedback")
            .icon("talend-bubbles")
            .type("@@modal/SHOW")
            .payload(PAYLOAD_METHOD_KEY, "showFeedback")
            .build();

    ActionSettings HEADERBAR_INFORMATION = dropdownBuilder()
            .id("headerbar:information")
            .name("headerbar.information")
            .icon("talend-information")
            .staticActions(new ArrayList<>())
            .build();

    ActionSettings HEADERBAR_INFORMATION_PLAYGROUND = dropdownBuilder()
            .id("headerbar:playground:information")
            .name("headerbar.information")
            .icon("talend-information")
            .staticActions(new ArrayList<>())
            .build();

    ActionSettings EXTERNAL_DOCUMENTATION = builder()
            .id("external:documentation")
            .name("external.documentation")
            .icon("talend-question-circle")
            .type("@@external/OPEN_WINDOW")
            .payload(PAYLOAD_METHOD_KEY, "open")
            .build();
}
// @formatter:on
