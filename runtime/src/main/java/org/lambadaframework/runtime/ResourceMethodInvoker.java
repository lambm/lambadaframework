package org.lambadaframework.runtime;


import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.model.Invocable;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.Request;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResourceMethodInvoker {


    static final Logger logger = Logger.getLogger(ResourceMethodInvoker.class);
    static final ObjectMapper objectMapper = new ObjectMapper();

    private ResourceMethodInvoker() {
    }

    private static Object toObject(String value, Class clazz) {
        if (Integer.class == clazz || Integer.TYPE == clazz) return value == null ? null : Integer.parseInt(value);
        if (Long.class == clazz || Long.TYPE == clazz) return value == null ? null : Long.parseLong(value);
        if (Float.class == clazz || Float.TYPE == clazz) return value == null ? null : Float.parseFloat(value);
        if (Boolean.class == clazz || Boolean.TYPE == clazz) return value == null ? null : Boolean.parseBoolean(value);
        if (Double.class == clazz || Double.TYPE == clazz) return value == null ? null : Double.parseDouble(value);
        if (Byte.class == clazz || Byte.TYPE == clazz) return value == null ? null : Byte.parseByte(value);
        if (Short.class == clazz || Short.TYPE == clazz) return value == null ? null : Short.parseShort(value);
        // See if the class has a string constructor
        try {
            Constructor constructor = clazz.getConstructor(String.class);
            return constructor.newInstance(value);
        } catch (InvocationTargetException |InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException nsme) {
            // Not a class with a string constructor
        }
        // See if the class has a static 'valueOf' method
        try {
            Method method = clazz.getDeclaredMethod("valueOf", String.class);
            return method.invoke(null, value);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException nsme) {
            // Not a class with a static valueOf method
        }
        // See if the class has a static 'fromStrign' method
        try {
            Method method = clazz.getDeclaredMethod("fromString", String.class);
            return method.invoke(null, value);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException nsme) {
            // Not a class with a static fromString method
        }
        
        return value;
    }

    public static Object invoke(ResourceMethod resourceMethod,
                                Request request,
                                Context lambdaContext)
            throws
            InvocationTargetException,
            IllegalAccessException,
            InstantiationException {

        logger.debug("Request object is: " + request.toString());


        Invocable invocable = resourceMethod.getInvocable();

        Method method = invocable.getHandlingMethod();
        Class clazz = invocable.getHandler().getHandlerClass();

        Object instance = clazz.newInstance();

        List<Object> varargs = new ArrayList<>();


        /**
         * Get consumes annotation from handler method
         */
        Consumes consumesAnnotation = method.getAnnotation(Consumes.class);

        for (Parameter parameter : method.getParameters()) {

            Class<?> parameterClass = parameter.getType();

            /**
             * Path parameter
             */
            if (parameter.isAnnotationPresent(PathParam.class)) {
                PathParam annotation = parameter.getAnnotation(PathParam.class);
                varargs.add(toObject(
                        (String) request.getPathParameters().get(annotation.value()), parameterClass
                        )
                );

            }


            /**
             * Query parameter
             */
            else if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                varargs.add(toObject(
                        (String) request.getQueryParams().get(annotation.value()), parameterClass
                        )
                );
            }

            /**
             * Header parameter
             */
            else if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                varargs.add(toObject(
                        (String) request.getRequestHeaders().get(annotation.value()), parameterClass
                        )
                );
            }
            
            

            else if (consumesAnnotation != null && consumesSpecificType(consumesAnnotation, MediaType.APPLICATION_JSON)
                    && parameter.getType() == String.class) {
                //Pass raw request body
                varargs.add(request.getRequestBody());
            }


            /**
             * Lambda Context can be automatically injected
             */
            else if (parameter.getType() == Context.class) {
                varargs.add(lambdaContext);
            }
            
            
            /**
             * If none of the other types, assume a JSON object
             */
            else {
                try {
                    Object requestBody = request.getRequestBody();
                    Type parameterType = parameter.getParameterizedType();
                    JavaType javaType = objectMapper.getTypeFactory().constructType(parameterType); 
                    if (requestBody instanceof String) {
                        varargs.add(objectMapper.readValue((String)requestBody, javaType));
                    } else {
                        varargs.add(objectMapper.convertValue(requestBody, javaType));
                    }
                } catch (IOException ex) {
                    // Our assumption of a JSON object was incorrect
                }
            }
        }

        return method.invoke(instance, varargs.toArray());
    }

    private static boolean consumesSpecificType(Consumes annotation, String type) {

        String[] consumingTypes = annotation.value();
        for (String consumingType : consumingTypes) {
            if (type.equals(consumingType)) {
                return true;
            }
        }

        return false;
    }
}
