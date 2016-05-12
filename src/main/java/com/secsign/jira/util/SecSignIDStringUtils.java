package com.secsign.jira.util;

public class SecSignIDStringUtils {

    /**
     * Trims whitespace
     * @param stringToBetrimmed the string to be trimmed
     * @return the trimmed string
     */
    public static String trim(String stringToBeTrimmed){
        return trim(stringToBeTrimmed, ' ', true);
    }
    
    /**
     * Trims an arbitrary character from beginning and ending
     * @param stringToBetrimmed the string to be trimmed
     * @param characterToBeTrimmed the character
     * @return the trimmed string
     */
    public static String trim(String stringToBeTrimmed, char characterToBeTrimmed){
        return trim(stringToBeTrimmed, characterToBeTrimmed, true);
    }
    
    /**
     * Trims an arbitrary character from beginning and ending
     * @param stringToBetrimmed the string to be trimmed
     * @param characterToBeTrimmed the character
     * @param allWhiteSpace does if it is whitespace shall be treated as all whitespace
     * @return the trimmed string
     */
    public static String trim(String stringToBeTrimmed, char characterToBeTrimmed, boolean allWhiteSpace){
        if(stringToBeTrimmed == null || stringToBeTrimmed.length() < 1){
            return stringToBeTrimmed;
        }
        
        //if(Character.isWhitespace(characterToBeTrimmed) && allWhiteSpace){
        if(characterToBeTrimmed == ' ' && allWhiteSpace){
            return stringToBeTrimmed.trim();
        }
        
        int length = stringToBeTrimmed.length();
        int ending = length;
        int beginning = 0;
        
        // no chance to avoid opcode of getChar

        while ((beginning < ending) && (stringToBeTrimmed.charAt(beginning) == characterToBeTrimmed)) {
            beginning++;
        }
        while ((beginning < ending) && (stringToBeTrimmed.charAt(ending - 1) == characterToBeTrimmed)) {
            ending--;
        }
        return ((beginning > 0) || (ending < length)) ? stringToBeTrimmed.substring(beginning, ending) : stringToBeTrimmed;
    }
}
