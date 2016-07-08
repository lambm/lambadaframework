package org.lambadaframework.runtime;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.MethodHandler;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lambadaframework.runtime.models.Request;
import org.lambadaframework.runtime.models.Response;
import org.lambadaframework.runtime.router.Router;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.*;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Invocable.class, ResourceMethod.class, Router.class, org.lambadaframework.jaxrs.model.ResourceMethod.class})
public class HandlerTest {


    public static class Entity {
        public long id;
        public String query1;
    }
    
    public static enum EntityType {
        big, small
    }

    @Path("/")
    public static class DummyController {
        @GET
        @Path("{id}")
        public javax.ws.rs.core.Response getEntity(
                @PathParam("id") long id
        ) {
            Entity entity = new Entity();
            entity.id = id;
            entity.query1 = "cagatay gurturk";
            return javax.ws.rs.core.Response
                    .status(200)
                    .entity(entity)
                    .build();
        }
        
        @GET
        @Path("/type/{type}")
        public javax.ws.rs.core.Response getEntityByType(
                @PathParam("type") EntityType e
        ) {
            Entity entity = new Entity();
            entity.id = 33;
            entity.query1 = e.name();
            return javax.ws.rs.core.Response
                    .status(200)
                    .entity(entity)
                    .build();
        }

        @POST
        @Path("{id}")
        public javax.ws.rs.core.Response createEntity(
                @PathParam("id") long id,
                @QueryParam("query1") String query1
        ) {
            Entity entity = new Entity();
            entity.id = id;
            entity.query1 = query1;

            return javax.ws.rs.core.Response
                    .status(201)
                    .header("Location", "http://www.google.com")
                    .entity(entity)
                    .build();
        }
        
        @POST
        @Path("/")
        public javax.ws.rs.core.Response createEntity(
                Entity entity
        ) {
            return javax.ws.rs.core.Response
                    .status(201)
                    .header("Location", "http://www.google.com")
                    .entity(entity)
                    .build();
        }
        
        @POST
        @Path("/batch")
        public javax.ws.rs.core.Response createEntities(
                List<Entity> entities
        ) {
            return javax.ws.rs.core.Response
                    .status(201)
                    .header("Location", "http://www.google.com")
                    .entity(entities)
                    .build();
        }
    }

    private Router getMockRouter(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        Invocable mockInvocable = PowerMock.createMock(Invocable.class);
        expect(mockInvocable.getHandlingMethod())
                .andReturn(DummyController.class.getDeclaredMethod(methodName, parameterTypes))
                .anyTimes();

        expect(mockInvocable.getHandler())
                .andReturn(MethodHandler.create(DummyController.class))
                .anyTimes();

        org.lambadaframework.jaxrs.model.ResourceMethod mockResourceMethod = PowerMock.createMock(org.lambadaframework.jaxrs.model.ResourceMethod.class);
        expect(mockResourceMethod.getInvocable())
                .andReturn(mockInvocable)
                .anyTimes();

        Router mockRouter = PowerMock.createMock(Router.class);
        expect(mockRouter.route(anyObject()))
                .andReturn(mockResourceMethod)
                .anyTimes();

        PowerMock.replayAll();
        return mockRouter;
    }

    private Request getRequest(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Request.class);
    }

    private Context getContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return "23234234";
            }

            @Override
            public String getLogGroupName() {
                return null;
            }

            @Override
            public String getLogStreamName() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                return null;
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 5000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 128;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        };
    }


    @Test
    public void testWith200Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"GET\",\n" +
                "  \"requestBody\": {},\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");

        Handler handler = new Handler();
        handler.setRouter(getMockRouter("getEntity", long.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("200", response.getErrorMessage());
        assertEquals("cagatay gurturk", ((Entity) response.getEntity()).query1);
        assertEquals(123, ((Entity) response.getEntity()).id);

    }


    @Test
    public void testWith201Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": {},\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntity", long.class, String.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals("test3", ((Entity) response.getEntity()).query1);
        assertEquals(123, ((Entity) response.getEntity()).id);
        assertEquals("http://www.google.com", response.getHeaders().get("Location"));

    }
    
    /**
     * Tests sending an Entity object in the request
    */
    @Test
    public void testWithPostObject()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": { \"id\" : \"456\", \"query1\": \"test4\"},\n" +
                "  \"path\": {},\n" +
                "  \"querystring\": {},\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntity", Entity.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals("test4", ((Entity) response.getEntity()).query1);
        assertEquals(456, ((Entity) response.getEntity()).id);
        assertEquals("http://www.google.com", response.getHeaders().get("Location"));
    }
    
    /**
     * Tests sending a List<Entity> in the request
    */
    @Test
    public void testWithPostList()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/batch}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": [ { \"id\" : \"789\", \"query1\": \"test5\"}, { \"id\" : \"333\", \"query1\": \"test6\"} ],\n" +
                "  \"path\": {},\n" +
                "  \"querystring\": {},\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntities", List.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals(2, ((List) response.getEntity()).size());
        Entity second = (Entity)((List) response.getEntity()).get(1);
        assertEquals(333, second.id);
        assertEquals("test6", second.query1);
        assertEquals("http://www.google.com", response.getHeaders().get("Location"));
    }
    
    
    @Test
    public void testWithEnum()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/type/{type}\",\n" +
                "  \"method\": \"GET\",\n" +
                "  \"requestBody\": {},\n" +
                "  \"path\": {\n" +
                "    \"type\": \"big\"\n" +
                "  },\n" +
                "  \"querystring\": {},\n" +
                "  \"header\": {}\n" +
                "}");

        Handler handler = new Handler();
        handler.setRouter(getMockRouter("getEntityByType", EntityType.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("200", response.getErrorMessage());
        assertEquals("big", ((Entity) response.getEntity()).query1);
        assertEquals(33, ((Entity) response.getEntity()).id);

    }
    
    @Test
    public void testWithNull()
            throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": {},\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {},\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntity", long.class, String.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertNull(((Entity) response.getEntity()).query1);
        assertEquals(123, ((Entity) response.getEntity()).id);
        assertEquals("http://www.google.com", response.getHeaders().get("Location"));

    }


}