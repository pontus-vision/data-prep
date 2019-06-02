//  ============================================================================
//
//  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.api.service.command.preparation;

import static org.talend.dataprep.command.Defaults.asString;

import java.net.URISyntaxException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.talend.dataprep.command.GenericCommand;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.APIErrorCodes;

@Component
@Scope("request")
public class PreparationClone extends GenericCommand<String> {

    private PreparationClone(String id) {
        super(GenericCommand.PREPARATION_GROUP);
        execute(() -> onExecute(id)); // $NON-NLS-1$
        onError(e -> new TDPException(APIErrorCodes.UNABLE_TO_CREATE_PREPARATION, e));
        on(HttpStatus.OK).then(asString());
    }

    private HttpRequestBase onExecute(String id) {
        try {
            URIBuilder uriBuilder = new URIBuilder(preparationServiceUrl + "/preparations/clone/" + id);
            return new HttpPut( uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_CREATE_PREPARATION, e);
        }
    }
}
