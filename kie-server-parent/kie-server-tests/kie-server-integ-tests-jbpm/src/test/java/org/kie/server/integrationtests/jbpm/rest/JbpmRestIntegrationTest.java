/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.integrationtests.jbpm.rest;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.type.JaxbLong;
import org.kie.server.integrationtests.config.TestConfig;
import org.kie.server.integrationtests.jbpm.DBExternalResource;
import org.kie.server.integrationtests.shared.KieServerDeployer;
import org.kie.server.integrationtests.shared.basetests.RestOnlyBaseIntegrationTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.kie.server.api.rest.RestURI.*;
import org.kie.server.integrationtests.shared.KieServerAssert;
import org.kie.server.integrationtests.shared.KieServerDeployer;


public class JbpmRestIntegrationTest extends RestOnlyBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "rest-processes", "1.0.0.Final");

    private static Logger logger = LoggerFactory.getLogger(JbpmRestIntegrationTest.class);

    private static Map<MarshallingFormat, String> acceptHeadersByFormat = new HashMap<MarshallingFormat, String>();

    @ClassRule
    public static ExternalResource StaticResource = new DBExternalResource();

    @BeforeClass
    public static void buildAndDeployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/rest-processes").getFile());
        // set the accepted formats with quality param to express preference
        acceptHeadersByFormat.put(MarshallingFormat.JAXB, "application/xml;q=0.9,application/json;q=0.3");// xml is preferred over json
        acceptHeadersByFormat.put(MarshallingFormat.JSON, "application/json;q=0.9,application/xml;q=0.3");// json is preferred over xml

        createContainer(CONTAINER, releaseId);
    }


    @Before
    public void cleanup() {
        cleanupSingletonSessionId();
    }

    /**
     * Process ids
     */

//    private static final String HUMAN_TASK_PROCESS_ID        = "org.test.kjar.writedocument";
//    private static final String HUMAN_TASK_VAR_PROCESS_ID    = "org.test.kjar.HumanTaskWithForm";
//    private static final String SCRIPT_TASK_PROCESS_ID       = "org.test.kjar.scripttask";
//    private static final String SCRIPT_TASK_VAR_PROCESS_ID   = "org.test.kjar.scripttask.var";
//    private static final String SINGLE_HUMAN_TASK_PROCESS_ID = "org.test.kjar.singleHumanTask";
//    private static final String OBJECT_VARIABLE_PROCESS_ID   = "org.test.kjar.ObjectVariableProcess";
//    private static final String RULE_TASK_PROCESS_ID         = "org.test.kjar.RuleTask";
//    private static final String TASK_CONTENT_PROCESS_ID      = "org.test.kjar.UserTask";
//    private static final String EVALUTAION_PROCESS_ID        = "org.test.kjar.evaluation";
//    private static final String GROUP_ASSSIGNMENT_PROCESS_ID = "org.test.kjar.GroupAssignmentHumanTask";
//    private static final String GROUP_ASSSIGN_VAR_PROCESS_ID = "org.test.kjar.groupAssign";
//    private static final String CLASSPATH_OBJECT_PROCESS_ID  = "org.test.kjar.classpath.process";

    private static final String CONTAINER = "rest-processes";
    private static final String HUMAN_TASK_OWN_TYPE_ID = "org.test.kjar.HumanTaskWithOwnType";

    @Test
    public void testBasicJbpmRequest() throws Exception {
        KieContainerResource resource = new KieContainerResource(CONTAINER, releaseId);

        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(CONTAINER_ID, resource.getContainerId());
        valuesMap.put(PROCESS_ID, HUMAN_TASK_OWN_TYPE_ID);

        ClientResponse<JaxbLong> response = null;
        try {
            ClientRequest clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + START_PROCESS_POST_URI, valuesMap)).header("Content-Type", getMediaType().toString());
            logger.info( "[POST] " + clientRequest.getUri());
            response = clientRequest.post();
            Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            Assert.assertEquals(getMediaType().toString(), response.getHeaders().getFirst("Content-Type"));

            valuesMap.put(PROCESS_INST_ID, response.getEntity(JaxbLong.class).unwrap());
            clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + ABORT_PROCESS_INST_DEL_URI, valuesMap)).header("Content-Type", getMediaType().toString());
            logger.info( "[DELETE] " + clientRequest.getUri());

            response = clientRequest.delete();
            int noContentStatusCode = Response.Status.NO_CONTENT.getStatusCode();
            int okStatusCode = Response.Status.OK.getStatusCode();
            assertTrue("Wrong status code returned: " + response.getStatus(),
                    response.getStatus() == noContentStatusCode || response.getStatus() == okStatusCode);

        } catch (Exception e) {
            throw new ClientResponseFailure(e, response);
        } finally {
            response.releaseConnection();
        }

    }

    @Test
    public void testBasicJbpmRequestWithSingleAcceptHeader() throws Exception {
        KieContainerResource resource = new KieContainerResource(CONTAINER, releaseId);

        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(CONTAINER_ID, resource.getContainerId());
        valuesMap.put(PROCESS_ID, HUMAN_TASK_OWN_TYPE_ID);

        ClientResponse<JaxbLong> response = null;
        try {
            ClientRequest clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + START_PROCESS_POST_URI, valuesMap))
                    .header("Content-Type", getMediaType().toString())
                    .header("Accept", getMediaType().toString());
            logger.info( "[POST] " + clientRequest.getUri());
            response = clientRequest.post();
            Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            Assert.assertEquals(getMediaType().toString(), response.getHeaders().getFirst("Content-Type"));

            valuesMap.put(PROCESS_INST_ID, response.getEntity(JaxbLong.class).unwrap());
            clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + ABORT_PROCESS_INST_DEL_URI, valuesMap)).header("Content-Type", getMediaType().toString());
            logger.info( "[DELETE] " + clientRequest.getUri());

            response = clientRequest.delete();
            int noContentStatusCode = Response.Status.NO_CONTENT.getStatusCode();
            int okStatusCode = Response.Status.OK.getStatusCode();
            assertTrue("Wrong status code returned: " + response.getStatus(),
                    response.getStatus() == noContentStatusCode || response.getStatus() == okStatusCode);

        } catch (Exception e) {
            throw new ClientResponseFailure(e, response);
        } finally {
            response.releaseConnection();
        }

    }

    @Test
    public void testBasicJbpmRequestManyAcceptHeaders() throws Exception {
        KieContainerResource resource = new KieContainerResource(CONTAINER, releaseId);

        Map<String, Object> valuesMap = new HashMap<String, Object>();
        valuesMap.put(CONTAINER_ID, resource.getContainerId());
        valuesMap.put(PROCESS_ID, HUMAN_TASK_OWN_TYPE_ID);

        ClientResponse<JaxbLong> response = null;
        try {

            ClientRequest clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + START_PROCESS_POST_URI, valuesMap))
                    .header("Content-Type", getMediaType().toString())
                    .header("Accept", acceptHeadersByFormat.get(marshallingFormat));
            logger.info( "[POST] " + clientRequest.getUri());
            response = clientRequest.post();
            Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            Assert.assertEquals(getMediaType().toString(), response.getHeaders().getFirst("Content-Type"));

            valuesMap.put(PROCESS_INST_ID, response.getEntity(JaxbLong.class).unwrap());
            clientRequest = newRequest(build(TestConfig.getKieServerHttpUrl(), PROCESS_URI + "/" + ABORT_PROCESS_INST_DEL_URI, valuesMap)).header("Content-Type", getMediaType().toString());
            logger.info( "[DELETE] " + clientRequest.getUri());

            response = clientRequest.delete();
            int noContentStatusCode = Response.Status.NO_CONTENT.getStatusCode();
            int okStatusCode = Response.Status.OK.getStatusCode();
            assertTrue("Wrong status code returned: " + response.getStatus(),
                    response.getStatus() == noContentStatusCode || response.getStatus() == okStatusCode);

        } catch (Exception e) {
            throw new ClientResponseFailure(e, response);
        } finally {
            response.releaseConnection();
        }

    }
}
