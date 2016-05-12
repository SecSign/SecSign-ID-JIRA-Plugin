package com.secsign.jira.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Logging helper class
 *
 * @author SecSign Technologies Inc.
 */
public abstract class SecSignIDLogger {
    
    // @see https://confluence.atlassian.com/adminjiraserver071/logging-and-profiling-802592962.html
    
    private final static boolean debug = false;
    
    /**
     * debug log
     * @param message
     */
    public static void debug(Object object){
        if(SecSignIDLogger.debug){
            SecSignIDLogger.log(object, null, 3);
        }
    }
    
    /**
     * debug log
     * @param message
     */
    public static void debug(Object object, String description){
        if(SecSignIDLogger.debug){
            SecSignIDLogger.log(object, description, 3);
        }
    }
    
    /**
     * Logs the calling method and the given message
     * @param message the message which shall be logged
     */
    public static void log(Object message){
        SecSignIDLogger.log(message, null, 3);
    }
    
    /**
     * Logs the calling method and the given message
     * @param message the message which shall be logged
     */
    public static void log(Object message, String description){
        SecSignIDLogger.log(message, description, 3);
    }
    
    /**
     * Logs the calling method and the given message
     * @param message the message or the object which shall be logged
     * @param description an additional description
     * @param stackTraceElement this is just to have the same height of stacktrace
     */
    private static void log(Object message, String description, int stackTraceElement){
        // check whether message is a throwable. in this case just print the stacktrace
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement ste = stackTraceElements[stackTraceElement];
        
        if(message instanceof Map){
            Map map = (Map)message;
            
            System.out.print("SecSign ID (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "): " + 
                    ste.getClassName() + "." + ste.getMethodName() + ": "); 
            
            if(description != null){
                System.out.print(description + ": ");
            }
           
            System.out.println("{");
            for(Iterator it = map.entrySet().iterator(); it.hasNext();){
                Map.Entry entry = (Entry) it.next();
                
                if(entry.getValue() instanceof String[]){
                    System.out.println("\t\t" + entry.getKey() + "=" + Arrays.toString((String[])entry.getValue()));   
                }
                else {
                    System.out.println("\t\t" + entry.getKey() + "=" + String.valueOf(entry.getValue()));
                }
            }
            System.out.println("}");
            
        } else {
            String completeMessage = String.valueOf(message) + (description != null ? (" <= " + description) : "");
            System.out.println("SecSign ID (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "): " + 
                    ste.getClassName() + "." + ste.getMethodName() + ": " + 
                    String.valueOf(completeMessage));
        }
    }
    
    /**
     * Logs the given error
     * @param error the error which shall be logged
     */
    public static void log(Throwable error){
        System.out.println(error.getMessage());
        error.printStackTrace(System.out);
    }
    
    /**
     * Auxiliary method to print objects and strings.
     * @param obj given object which shall be converted into a nice string.
     * @return the string representation of a given object
     */
    public static String toString(Object obj)
    {
        if(obj instanceof Map){
            
            Map map = (Map)obj;
            
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for(Iterator it = map.entrySet().iterator(); it.hasNext();){
                Map.Entry entry = (Entry) it.next();
                
                if(entry.getValue() instanceof String[]){
                    sb.append(entry.getKey() + "=" + Arrays.toString((String[])entry.getValue()));   
                }
                else {
                    sb.append(entry.getKey() + "=" + String.valueOf(entry.getValue()));
                }
                
                if(it.hasNext()){
                    sb.append(", ");
                }
            }
            sb.append("}");
            
            return sb.toString();
        }
        
        return String.valueOf(obj);
    }
}
